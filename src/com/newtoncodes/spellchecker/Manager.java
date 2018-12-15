package com.newtoncodes.spellchecker;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.*;
import com.intellij.spellchecker.SpellCheckerManager;
import com.intellij.spellchecker.dictionary.CustomDictionaryProvider;
import com.intellij.spellchecker.dictionary.Dictionary;
import com.intellij.spellchecker.settings.SpellCheckerSettings;
import com.intellij.spellchecker.state.CachedDictionaryState;
import com.intellij.spellchecker.state.ProjectDictionaryState;
import com.newtoncodes.spellchecker.dictionaries.ExtendedDictionary;
import com.newtoncodes.spellchecker.dictionaries.ExtendedProjectDictionary;
import com.newtoncodes.spellchecker.states.GlobalSettingsState;
import com.newtoncodes.spellchecker.states.GlobalUserState;
import com.newtoncodes.spellchecker.states.ProjectSettingsState;
import com.newtoncodes.spellchecker.states.ProjectUserState;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.intellij.openapi.application.PathManager.getOptionsPath;
import static com.intellij.project.ProjectKt.getProjectStoreDirectory;


class Manager extends SpellCheckerManager {
    private static final String CACHE_PATH = System.getProperty("idea.system.path", "") + File.separator + "spellchecker-extended";

    private static final Logger LOG = Logger.getInstance("#com.newtoncodes.spellchecker.Manager");

    private ProjectUserState projectState;
    private GlobalUserState globalState;

    private ProjectSettingsState projectSettings;
    private GlobalSettingsState globalSettings;

    private Set<String> hunspell;
    private String version;

    private Manager(Project project, SpellCheckerSettings settings) {
        super(project, settings);
    }

    private void updateGlobal() {
        globalState.setShared(globalSettings.isSharedGlobal());
    }

    private void updateProject() {
        if (hunspell == null) hunspell = new THashSet<>();

        projectState.setShared(projectSettings.isSharedProject());

        final Set<String> enabled = projectSettings.getHunspell();
        final Set<String> removed = new THashSet<>();

        boolean changed = false;

        for (String name : hunspell) {
            if (!enabled.contains(name)) removed.add(name);
        }

        for (String name : removed) {
            changed = true;
            unloadHunspell(name);
        }

        for (String name : enabled) {
            if (!hunspell.contains(name)) {
                changed = true;
                loadHunspell(name);
            }
        }

        if (changed) restartInspections();
    }

    @Override
    public void fullConfigurationReload() {
        super.fullConfigurationReload();

        if (version == null) {
            IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("com.newtoncodes.spellchecker"));
            version = plugin != null ? plugin.getVersion() : "0.0.0";
        }

        if (hunspell == null) hunspell = new THashSet<>();

        if (globalSettings == null) {
            globalSettings = ServiceManager.getService(GlobalSettingsState.class);
            globalSettings.addListener(this::updateGlobal);
            globalState = (GlobalUserState) ServiceManager.getService(CachedDictionaryState.class);
            globalState.setShared(globalSettings.isSharedGlobal());
        }

        if (projectSettings == null) {
            projectSettings = ServiceManager.getService(getProject(), ProjectSettingsState.class);
            projectSettings.addListener(this::updateProject);
            projectState = (ProjectUserState) ServiceManager.getService(getProject(), ProjectDictionaryState.class);
            projectState.setShared(projectSettings.isSharedProject());
        }

        final Set<String> enabled = projectSettings.getHunspell();

        hunspell.clear();

        if (enabled != null && !enabled.isEmpty()) for (String name : enabled) {
            loadHunspell(name);
        }
    }

    @Override
    public boolean hasProblem(@NotNull String word) {
        if (word.matches("^[a-fA-F0-9]+$")) return false;
        LOG.warn("[HUNSPELL] WORD: " + word);
        return super.hasProblem(word);
    }

    @Override
    @NotNull
    protected List<String> getRawSuggestions(@NotNull String word) {
        if (!hasProblem(word)) return Collections.emptyList();
        return super.getRawSuggestions(word);
    }

    @Override
    @NotNull
    public String getProjectDictionaryPath() {
        final ExtendedProjectDictionary dictionary = (ExtendedProjectDictionary) projectState.getProjectDictionary();

        if (ProjectUserState.DICTIONARY_SHARED.equals(dictionary.getActive())) {
            final VirtualFile base = ProjectUtil.guessProjectDir(getProject());
            final VirtualFile store = base != null ? getProjectStoreDirectory(base) : null;
            return store != null ? store.getPath() + File.separator + "dictionary.xml" : "";
        }

        return super.getProjectDictionaryPath();
    }

    @Override
    @NotNull
    public String getAppDictionaryPath() {
        final ExtendedDictionary dictionary = (ExtendedDictionary) globalState.getDictionary();

        if (GlobalUserState.DICTIONARY_SHARED.equals(dictionary.getActive())) {
            return getOptionsPath() + File.separator + "sharedDictionary.xml";
        }

        return super.getAppDictionaryPath();
    }

    private void loadHunspell(@NotNull String name) {
        hunspell.add(name);

        final String dic = preloadResource(name);
        if (dic == null) {
            LOG.warn("[HUNSPELL] Dic file not found: " + name);
            return;
        }

        final String aff = preloadResource(FileUtilRt.getNameWithoutExtension(name) + "." + "aff");
        if (aff == null) {
            LOG.warn("[HUNSPELL] Aff file not found: " + name);
            return;
        }

        for (CustomDictionaryProvider provider : CustomDictionaryProvider.EP_NAME.getExtensionList()) {
            if (!provider.toString().startsWith("com.intellij.hunspell.HunspellDictionaryProvider")) continue;

            final Dictionary dictionary = provider.get(dic);
            if (dictionary == null) continue;

            getSpellChecker().addDictionary(dictionary);
            return;
        }
    }

    private void unloadHunspell(@NotNull String name) {
        hunspell.remove(name);
        getSpellChecker().removeDictionary(getResourceFile(name).getPath());
    }

    private File getResourceFile(@NotNull String name) {
        return new File(CACHE_PATH + File.separator + version + File.separator + name);
    }

    private void purgeResourceFiles(@NotNull File dir, int level) {
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] list = dir.listFiles();
        if (list == null) return;

        for (File f: list) {
            if (level == 0 && f.getName().equals(version)) continue;
            if (f.isDirectory()) purgeResourceFiles(f, 1);
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    private String preloadResource(@NotNull String name) {
        final File file = getResourceFile(name);

        purgeResourceFiles(new File(CACHE_PATH), 0);
        if (file.exists()) return file.getPath();

        final InputStream stream = Manager.class.getResourceAsStream(name);
        if (stream == null) return null;

        final byte[] buffer;
        final FileOutputStream writer;

        try {
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
        } catch (SecurityException e) {
            try {stream.close();} catch (IOException ex) { /* Ignore */ }
            return null;
        }

        try {
            buffer = new byte[stream.available()];
        } catch (IOException ex) {
            try {stream.close();} catch (IOException e) { /* Ignore */ }
            return null;
        }

        try {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
        } catch (IOException ex) {
            try {stream.close();} catch (IOException e) { /* Ignore */ }
            return null;
        }

        try {
            writer = new FileOutputStream(file, false);
        } catch (FileNotFoundException ex) {
            try {stream.close();} catch (IOException e) { /* Ignore */ }
            return null;
        }

        try {
            //noinspection ResultOfMethodCallIgnored
            stream.read(buffer);
            stream.close();
        } catch (IOException ex) {
            try {stream.close();} catch (IOException e) { /* Ignore */ }
            try {writer.close();} catch (IOException e) { /* Ignore */ }
            return null;
        }

        try {
            writer.write(buffer);
            writer.close();
        } catch (IOException ex) {
            try {writer.close();} catch (IOException e) { /* Ignore */ }
            return null;
        }

        return file.getPath();
    }
}
