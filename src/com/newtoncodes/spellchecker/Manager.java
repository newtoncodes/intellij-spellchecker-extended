package com.newtoncodes.spellchecker;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.spellchecker.BundledDictionaryProvider;
import com.intellij.spellchecker.FileLoader;
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

import java.io.File;
import java.net.URL;
import java.util.*;

import static com.intellij.openapi.application.PathManager.getOptionsPath;
import static com.intellij.project.ProjectKt.getProjectStoreDirectory;


@SuppressWarnings("WeakerAccess")
public class Manager extends SpellCheckerManager {
    private static final Logger LOG = Logger.getInstance("#com.newtoncodes.spellchecker.Manager");

    public static final String SHARED_CACHED_DICTIONARY_FILE  = "sharedDictionary.xml";
    public static final String SHARED_PROJECT_DICTIONARY_PATH = "dictionary.xml";

    private ProjectUserState projectState;
    private GlobalUserState globalState;

    private ProjectSettingsState projectSettings;
    private GlobalSettingsState globalSettings;

    private Set<String> hunspell;

    public Manager(Project project, SpellCheckerSettings settings) {
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
            hunspell.remove(name);
            getSpellChecker().removeDictionary(name);
        }

        for (String name : enabled) {
            if (!hunspell.contains(name)) {
                changed = true;
                hunspell.add(name);
                loadHunspell(name);
            }
        }

        if (changed) restartInspections();
    }

    @Override
    public void fullConfigurationReload() {
        super.fullConfigurationReload();

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
        // TODO: loader class from provider

        hunspell.clear();

        if (enabled != null && !enabled.isEmpty()) for (String name : enabled) {
            hunspell.add(name);
            loadHunspell(name);
        }
    }

    private void loadHunspell(@NotNull String name) {
        final URL stream = Manager.class.getResource(name);
        if (stream == null) {
            LOG.info("Hunspell not found: " + name);
            return;
        }

        final String path = stream.getFile();
        for (CustomDictionaryProvider provider : CustomDictionaryProvider.EP_NAME.getExtensionList()) {
            LOG.info("HUNSPELL PROVIDER0 " + provider.toString());

            final Dictionary dictionary = provider.get(name);
            if (dictionary == null) continue;

            LOG.info("HUNSPELL PROVIDER2 " + provider.toString());

            getSpellChecker().addDictionary(dictionary);
            return;
        }

        LOG.info("HUNSPELL NO PROVIDER " + path);
        getSpellChecker().loadDictionary(new FileLoader(path));
    }

    @Override
    public boolean hasProblem(@NotNull String word) {
        // TODO: skip hex and shit
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
            return store != null ? store.getPath() + File.separator + SHARED_PROJECT_DICTIONARY_PATH : "";
        }

        return super.getProjectDictionaryPath();
    }

    @Override
    @NotNull
    public String getAppDictionaryPath() {
        final ExtendedDictionary dictionary = (ExtendedDictionary) globalState.getDictionary();

        if (GlobalUserState.DICTIONARY_SHARED.equals(dictionary.getActive())) {
            return getOptionsPath() + File.separator + SHARED_CACHED_DICTIONARY_FILE;
        }

        return super.getAppDictionaryPath();
    }
}
