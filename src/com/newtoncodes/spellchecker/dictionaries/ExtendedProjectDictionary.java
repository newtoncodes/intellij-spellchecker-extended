package com.newtoncodes.spellchecker.dictionaries;

import com.intellij.spellchecker.dictionary.EditableDictionary;
import com.intellij.spellchecker.dictionary.ProjectDictionary;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;


public class ExtendedProjectDictionary extends ProjectDictionary {
    private ProjectDictionary user;
    private EditableDictionary shared;
    private String key;

    @SuppressWarnings("unused")
    public ExtendedProjectDictionary() {
        this(new THashSet<>());
    }

    @SuppressWarnings("WeakerAccess")
    public ExtendedProjectDictionary(@NotNull Set<EditableDictionary> dictionaries) {
        super(dictionaries);
    }

    public boolean setShared(@Nullable EditableDictionary dictionary) {
        if (dictionary == null || shared == dictionary) return false;

        shared = dictionary;
        getDictionaries().add(dictionary);

        return true;
    }

    public boolean setUser(@Nullable ProjectDictionary dictionary) {
        if (dictionary == null || user == dictionary) return false;

        user = dictionary;
        getDictionaries().add(dictionary);

        return true;
    }

    public void setActive(@NotNull String key) {
        if (key.equals(this.key)) return;

        this.key = key;
        super.setActiveName(key.equals("project") ? "project" : "projectShared");
    }

    public String getActive() {
        return key;
    }

    @SuppressWarnings("unused")
    @Override
    public void setActiveName(String name) {
        user.setActiveName(name);
    }
}
