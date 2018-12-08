package com.newtoncodes.spellchecker.states;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.spellchecker.dictionary.EditableDictionary;
import com.intellij.spellchecker.dictionary.ProjectDictionary;
import com.intellij.spellchecker.state.DictionaryStateListener;
import com.intellij.spellchecker.state.ProjectDictionaryState;
import com.intellij.util.EventDispatcher;
import com.intellij.util.xmlb.annotations.Transient;
import com.newtoncodes.spellchecker.dictionaries.ExtendedProjectDictionary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@SuppressWarnings({"WeakerAccess", "unused"})
public class ProjectUserState extends ProjectDictionaryState implements PersistentStateComponent<ProjectDictionaryState> {
    // private static final Logger LOG = Logger.getInstance("#com.newtoncodes.spellchecker.states.ProjectUserState");

    public static final String DICTIONARY_USER   = "project";
    public static final String DICTIONARY_SHARED = "projectShared";

    private EventDispatcher<DictionaryStateListener> dispatcher;
    private ExtendedProjectDictionary collection;

    public ProjectUserState() {
        super();

        dispatcher = EventDispatcher.create(DictionaryStateListener.class);
        collection = new ExtendedProjectDictionary();
        collection.setActive(DICTIONARY_USER);
    }

    public ProjectUserState(Project project) {
        this();

        ProjectSharedState shared = ServiceManager.getService(project, ProjectSharedState.class);
        shared.addListener(this::updateShared);
        updateShared(shared.getDictionary());
    }

    public void setShared(boolean value) {
        collection.setActive(value ? DICTIONARY_SHARED : DICTIONARY_USER);
    }

    @Override
    public void addProjectDictListener(DictionaryStateListener listener) {
        dispatcher.addListener(listener);
    }

    @Override
    @Transient
    public ProjectDictionary getProjectDictionary() {
        updateUser(super.getProjectDictionary());
        return collection;
    }

    @Override
    public void loadState(@NotNull ProjectDictionaryState state) {
        super.loadState(state);
        updateUser(super.getProjectDictionary());
    }

    @Override
    public ProjectDictionaryState getState() {
        super.getState();
        return this;
    }

    private void updateUser(@Nullable ProjectDictionary dictionary) {
        if (dictionary == null || !collection.setUser(dictionary)) return;
        dispatcher.getMulticaster().dictChanged(collection);
    }

    private void updateShared(@Nullable EditableDictionary dictionary) {
        if (dictionary == null || !collection.setShared(dictionary)) return;
        dispatcher.getMulticaster().dictChanged(collection);
    }
}
