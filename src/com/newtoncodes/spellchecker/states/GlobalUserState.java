package com.newtoncodes.spellchecker.states;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.spellchecker.dictionary.EditableDictionary;
import com.intellij.spellchecker.dictionary.UserDictionary;
import com.intellij.spellchecker.state.CachedDictionaryState;
import com.intellij.spellchecker.state.DictionaryState;
import com.intellij.spellchecker.state.DictionaryStateListener;
import com.intellij.util.EventDispatcher;
import com.intellij.util.xmlb.annotations.Transient;
import com.newtoncodes.spellchecker.dictionaries.ExtendedDictionary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@SuppressWarnings({"WeakerAccess", "unused"})
public class GlobalUserState extends CachedDictionaryState {
    public static final String DICTIONARY_USER   = "cached";
    public static final String DICTIONARY_SHARED = "cachedShared";

    private final EventDispatcher<DictionaryStateListener> dispatcher;
    private final ExtendedDictionary collection;

    public GlobalUserState() {
        this(new UserDictionary(DEFAULT_NAME));
    }

    public GlobalUserState(@NotNull EditableDictionary dictionary) {
        super();
        super.setDictionary(dictionary);

        dispatcher = EventDispatcher.create(DictionaryStateListener.class);
        collection = new ExtendedDictionary("application");
        collection.setActive(DICTIONARY_USER);

        GlobalSharedState shared = ServiceManager.getService(GlobalSharedState.class);
        shared.addListener(this::update);

        update(shared.getDictionary());
        update(dictionary);
    }

    public void setShared(boolean value) {
        collection.setActive(value ? DICTIONARY_SHARED : DICTIONARY_USER);
    }

    @Override
    public void addCachedDictListener(DictionaryStateListener listener) {
        dispatcher.addListener(listener);
    }

    @Override
    public void loadState(@NotNull DictionaryState state) {
        super.loadState(state);
        update(super.getDictionary());
    }

    @Override
    @Transient
    public void setDictionary(@NotNull EditableDictionary dictionary) {
        super.setDictionary(dictionary);
        update(dictionary);
    }

    @Override
    @Transient
    public EditableDictionary getDictionary() {
        return collection;
    }

    private void update(@Nullable EditableDictionary dictionary) {
        if (dictionary == null) return;
        if (collection.update(dictionary)) dispatcher.getMulticaster().dictChanged(collection);
    }
}