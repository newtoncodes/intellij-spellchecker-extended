package com.newtoncodes.spellchecker;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.intellij.openapi.application.PathManager.getOptionsPath;
import static com.intellij.project.ProjectKt.getProjectStoreDirectory;


public class Manager extends SpellCheckerManager {
    private static final String CACHE_PATH = System.getProperty("idea.system.path", "") + File.separator + "spellchecker-extended";

    private static final Logger LOG = Logger.getInstance("#com.newtoncodes.spellchecker.Manager");

    private ProjectUserState projectState;
    private GlobalUserState globalState;

    private ProjectSettingsState projectSettings;
    private GlobalSettingsState globalSettings;

    private Set<String> hunspell;
    private String version;

    private ArrayList<String> queue;
    private boolean queueRunning = false;

    public Manager(Project project, SpellCheckerSettings settings) {
        super(project, settings);
    }

    private void updateGlobal() {
        globalState.setShared(globalSettings.isSharedGlobal());
    }

    private void updateProject() {
        if (hunspell == null) hunspell = new THashSet<>();
        if (queue == null) queue = new ArrayList<>();

        projectState.setShared(projectSettings.isSharedProject());

        final List<String> enabled = projectSettings.getHunspell();
        final Set<String> removed = new THashSet<>();

        boolean changed = false;

        for (String name : hunspell) {
            if (!enabled.contains(name)) removed.add(name);
        }

        for (String name : removed) {
            changed = true;
            unloadHunspell(name);
        }

        enabled.sort(String::compareToIgnoreCase);

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
        if (queue == null) queue = new ArrayList<>();

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

        ArrayList<String> enabled = projectSettings.getHunspell();
        enabled.sort(String::compareToIgnoreCase);

        hunspell.clear();
        purgeResourceFiles(new File(CACHE_PATH), 0);

        if (!enabled.isEmpty()) for (String name : enabled) {
            loadHunspell(name);
        }
    }

    @Override
    public boolean hasProblem(@NotNull String word) {
        if (word.length() <= 3) return super.hasProblem(word);

        // Hex
        if (word.matches("^[a-fA-F0-9]+$")) return false;

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

    private void loadHunspell() {
        if (queueRunning || queue.size() == 0) return;
        queueRunning = true;

        String name = queue.remove(0);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            hunspell.add(name);

            final String dic = preloadResource(name, "dic");
            final String aff = preloadResource(name, "aff");

            if (dic == null || aff == null) {
                queueRunning = false;
                loadHunspell();
                return;
            }

            for (CustomDictionaryProvider provider : CustomDictionaryProvider.EP_NAME.getExtensionList()) {
                if (!provider.toString().startsWith("com.intellij.hunspell.HunspellDictionaryProvider")) continue;

                final Dictionary dictionary = provider.get(dic);
                if (dictionary == null) continue;

                getSpellChecker().addDictionary(dictionary);

                break;
            }

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    while (!getSpellChecker().isDictionaryLoad(getResourceFile(getResourceName(name, "dic")).getPath())) {
                        Thread.sleep(10);
                    }
                } catch (Exception e) { /* Ignore */ }

                if (queue.size() == 0) restartInspections();
                queueRunning = false;
                loadHunspell();
            });
        });
    }

    private void loadHunspell(@NotNull String name) {
        if (queue.contains(name)) return;
        queue.add(name);
        loadHunspell();
    }

    private void unloadHunspell(@NotNull String name) {
        hunspell.remove(name);
        getSpellChecker().removeDictionary(getResourceFile(getResourceName(name, "dic")).getPath());
    }

    private String getResourceName(@NotNull String name, @NotNull String ext) {
        return "/hunspell/" + name.replaceAll(" ", "_").replaceAll("-", "") + "." + ext;
    }

    private File getResourceFile(@NotNull String name) {
        return new File(CACHE_PATH + File.separator + version + File.separator + name);
    }

    private String preloadResource(@NotNull String name, @NotNull String ext) {
        final File file = getResourceFile(getResourceName(name, ext));
        if (file.exists()) return file.getPath();

        try {
            final InputStream stream = Manager.class.getResourceAsStream(getResourceName(name, ext));
            if (stream == null) throw new Exception();

            final byte[] buffer;
            final FileOutputStream writer;

            try {
                //noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();
            } catch (SecurityException e) {
                try {stream.close();} catch (IOException ex) { /* Ignore */ }
                throw new Exception();
            }

            try {
                buffer = new byte[stream.available()];
            } catch (IOException ex) {
                try {stream.close();} catch (IOException e) { /* Ignore */ }
                throw new Exception();
            }

            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (IOException ex) {
                try {stream.close();} catch (IOException e) { /* Ignore */ }
                throw new Exception();
            }

            try {
                writer = new FileOutputStream(file, false);
            } catch (FileNotFoundException ex) {
                try {stream.close();} catch (IOException e) { /* Ignore */ }
                throw new Exception();
            }

            try {
                //noinspection ResultOfMethodCallIgnored
                stream.read(buffer);
                stream.close();
            } catch (IOException ex) {
                try {stream.close();} catch (IOException e) { /* Ignore */ }
                try {writer.close();} catch (IOException e) { /* Ignore */ }
                throw new Exception();
            }

            try {
                writer.write(buffer);
                writer.close();
            } catch (IOException ex) {
                try {writer.close();} catch (IOException e) { /* Ignore */ }
                throw new Exception();
            }
        } catch (Exception e) {
            LOG.warn("[HUNSPELL] Resource not found: " + name + " [" + ext + "] -> " + file.getPath());
            return null;
        }

        return file.getPath();
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
}
