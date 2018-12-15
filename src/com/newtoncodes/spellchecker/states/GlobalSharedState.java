package com.newtoncodes.spellchecker.states;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.spellchecker.dictionary.EditableDictionary;
import com.intellij.spellchecker.dictionary.UserDictionary;
import com.intellij.spellchecker.state.DictionaryState;
import com.intellij.spellchecker.state.DictionaryStateListener;
import com.intellij.util.EventDispatcher;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;


@State(
    name = "SharedCachedDictionaryState",
    storages = @Storage(value = "sharedDictionary.xml")
)
class GlobalSharedState extends DictionaryState implements PersistentStateComponent<DictionaryState> {
    private final EventDispatcher<DictionaryStateListener> dispatcher = EventDispatcher.create(DictionaryStateListener.class);

    @SuppressWarnings("unused")
    public GlobalSharedState() {
        super();
        name = "cachedShared";
    }

    @SuppressWarnings("unused")
    public GlobalSharedState(EditableDictionary dictionary) {
        super(dictionary);
        name = "cachedShared";
    }

    @Override
    @NotNull
    @Transient
    public EditableDictionary getDictionary() {
        EditableDictionary dictionary = super.getDictionary();
        if (dictionary != null) return dictionary;

        dictionary = new UserDictionary(name);
        setDictionary(dictionary);

        return dictionary;
    }

    @Override
    @Transient
    public void setDictionary(@NotNull EditableDictionary dictionary) {
        if (super.getDictionary() == dictionary) return;

        super.setDictionary(dictionary);
        dispatcher.getMulticaster().dictChanged(dictionary);
    }

    @Override
    public void loadState(@NotNull DictionaryState state) {
        if (state.name == null) state.name = "cachedShared";

        super.loadState(state);
        dispatcher.getMulticaster().dictChanged(getDictionary());
    }

    void addListener(DictionaryStateListener listener) {
        dispatcher.addListener(listener);
    }
}
