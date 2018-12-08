package com.newtoncodes.spellchecker.states;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.EventDispatcher;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.intellij.openapi.util.text.StringUtil.notNullize;
import static com.intellij.openapi.util.text.StringUtil.parseInt;


@SuppressWarnings("unused")
@State(
    name = "ExtendedGlobalSpellCheckerSettings",
    storages = @Storage(value = "spellchecker-extended.xml")
)
public class GlobalSettingsState implements PersistentStateComponent<Element> {
    private static final String TAG = "ExtendedSpellCheckerSettings";
    private static final String ATTR_HUNSPELL_DICTIONARIES = "HunspellDictionaries";
    private static final String ATTR_HUNSPELL_DICTIONARY = "HunspellDictionary";
    private static final String ATTR_PROJECT_SHARED = "ProjectShared";
    private static final String ATTR_GLOBAL_SHARED = "GlobalShared";

    private boolean sharedProject = false;
    private boolean sharedGlobal = false;
    private Set<String> hunspell = new HashSet<>();

    public interface SettingsStateListener extends EventListener {
        void changed();
    }

    private final EventDispatcher<SettingsStateListener> dispatcher = EventDispatcher.create(SettingsStateListener.class);

    public GlobalSettingsState() {
        super();
    }

    public void setSharedGlobal(boolean value) {
        if (sharedGlobal == value) return;
        sharedGlobal = value;
        dispatcher.getMulticaster().changed();
    }

    public boolean isSharedGlobal() {
        return sharedGlobal;
    }

    public void setSharedProject(boolean value) {
        if (sharedProject == value) return;
        sharedProject = value;
        dispatcher.getMulticaster().changed();
    }

    public boolean isSharedProject() {
        return sharedProject;
    }

    public void setHunspell(Set<String> value) {
        if (hunspell == value || hunspell.equals(value)) return;
        hunspell = value;
        dispatcher.getMulticaster().changed();
    }

    public Set<String> getHunspell() {
        return hunspell;
    }

    @Override
    public void loadState(@NotNull final Element element) {
        hunspell.clear();

        try {
            final int bundledDictionariesSize = parseInt(element.getAttributeValue(ATTR_HUNSPELL_DICTIONARIES), 0);
            for (int i = 0; i < bundledDictionariesSize; i++) {
                hunspell.add(element.getAttributeValue(ATTR_HUNSPELL_DICTIONARY + i));
            }

            sharedProject = Boolean.parseBoolean(notNullize(element.getAttributeValue(ATTR_PROJECT_SHARED), String.valueOf(sharedProject)));
            sharedGlobal = Boolean.parseBoolean(notNullize(element.getAttributeValue(ATTR_GLOBAL_SHARED), String.valueOf(sharedGlobal)));

            dispatcher.getMulticaster().changed();
        }
        catch (Exception ignored) {}
    }

    @Override
    public Element getState() {
        final Element element = new Element(TAG);

        element.setAttribute(ATTR_PROJECT_SHARED, String.valueOf(sharedProject));
        element.setAttribute(ATTR_GLOBAL_SHARED, String.valueOf(sharedGlobal));

        element.setAttribute(ATTR_HUNSPELL_DICTIONARIES, String.valueOf(hunspell.size()));
        Iterator<String> iterator = hunspell.iterator();
        int i = 0;

        while (iterator.hasNext()) {
            element.setAttribute(ATTR_HUNSPELL_DICTIONARY + i, iterator.next());
            i ++;
        }

        return element;
    }

    public void addListener(SettingsStateListener listener) {
        dispatcher.addListener(listener);
    }
}
