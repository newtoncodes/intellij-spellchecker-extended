package com.newtoncodes.spellchecker.dictionaries;

import com.intellij.spellchecker.dictionary.EditableDictionary;
import com.intellij.util.Consumer;
import com.intellij.util.containers.hash.HashSet;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


@SuppressWarnings({"CanBeFinal", "unused"})
public class ExtendedDictionary implements EditableDictionary {
    private final Map<String, EditableDictionary> map = new HashMap<>();
    private EditableDictionary active;
    private String key;
    private String name;

    @SuppressWarnings("unused")
    public ExtendedDictionary(@Nullable String name) {
        this(name, new HashSet<>(), null);
    }

    @SuppressWarnings("unused")
    public ExtendedDictionary(@Nullable String name, @NotNull Set<EditableDictionary> dictionaries) {
        this(name, dictionaries, null);
    }

    @SuppressWarnings("WeakerAccess")
    public ExtendedDictionary(@Nullable String name, @NotNull Set<EditableDictionary> dictionaries, @Nullable String key) {
        for (EditableDictionary dictionary: dictionaries) {
            if (active == null) active = dictionary;
            map.put(dictionary.getName(), dictionary);
        }

        this.name = name;
        this.key = key;
        this.active = key != null ? map.get(key) : null;
    }

    public boolean update(@Nullable EditableDictionary dictionary) {
        if (dictionary == null) return false;

        if (map.get(dictionary.getName()) == dictionary) return false;

        map.put(dictionary.getName(), dictionary);
        if (dictionary.getName().equals(key)) active = dictionary;

        return true;
    }

    public void setActive(@NotNull String key) {
        this.key = key;
        this.active = map.get(key);
    }

    public String getActive() {
        return key;
    }

    @Override
    public void addToDictionary(String word) {
        if (active != null) active.addToDictionary(word);
    }

    @Override
    public void addToDictionary(@Nullable Collection<String> words) {
        if (active != null) active.addToDictionary(words);
    }

    @Override
    public void removeFromDictionary(String word) {
        if (active != null) active.removeFromDictionary(word);
    }

    @Override
    public void replaceAll(@Nullable Collection<String> words) {
        if (active != null) active.replaceAll(words);
    }

    @Override
    public void clear() {
        if (active != null) active.clear();
    }

    @Nullable
    @Override
    public Boolean contains(@NotNull String word) {
        int errors = 0;

        for (Map.Entry<String, EditableDictionary> entry : map.entrySet()) {
            Boolean contains = entry.getValue().contains(word);

            if (contains == null) errors ++;
            else if (contains) return true;
        }

        if (errors == map.size()) return null;

        return false;
    }

    @Override
    public int size() {
        int result = 0;

        for (Map.Entry<String, EditableDictionary> entry : map.entrySet()) {
            result += entry.getValue().size();
        }

        return result;
    }

    @Override
    @NotNull
    public Set<String> getEditableWords() {
        if (active == null) return Collections.emptySet();

        return active.getWords();
    }

    @Override
    @NotNull
    public Set<String> getWords() {
        Set<String> words = new THashSet<>();
        for (Map.Entry<String, EditableDictionary> entry : map.entrySet()) {
            Set<String> otherWords = entry.getValue().getWords();
            words.addAll(otherWords);
        }

        return words;
    }

    @Override
    public void traverse(@NotNull Consumer<String> consumer) {
        for (Map.Entry<String, EditableDictionary> entry : map.entrySet()) {
            entry.getValue().traverse(consumer);
        }
    }

    @SuppressWarnings("SameReturnValue")
    @Override
    public boolean isEmpty() {
        return false;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }
}
