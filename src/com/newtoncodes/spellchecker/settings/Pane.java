package com.newtoncodes.spellchecker.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;

import com.intellij.spellchecker.SpellCheckerManager;
import com.intellij.ui.OptionalChooserComponent;
import com.intellij.ui.components.JBCheckBox;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.newtoncodes.spellchecker.Hunspell;
import com.newtoncodes.spellchecker.states.GlobalSettingsState;
import com.newtoncodes.spellchecker.states.ProjectSettingsState;


@SuppressWarnings("unused")
public class Pane implements Disposable {
    private JPanel root;
    private JBCheckBox sharedGlobal;
    private JBCheckBox sharedProject;

    private JPanel hunspellListPanel;
    private final OptionalChooserComponent<String> hunspellChooser;
    private final List<Pair<String, Boolean>> hunspellDictionaries = new ArrayList<>();

    private final GlobalSettingsState globalSettings;
    private final ProjectSettingsState projectSettings;

    public Pane(GlobalSettingsState globalSettings, ProjectSettingsState projectSettings, final Project project) {
        this.globalSettings = globalSettings;
        this.projectSettings = projectSettings;

        setup();

        hunspellChooser = new OptionalChooserComponent<String>(hunspellDictionaries) {
            @Override
            public JCheckBox createCheckBox(String path, boolean checked) {
                return new JCheckBox(FileUtil.toSystemDependentName(path), checked);
            }

            @Override
            public void apply() {
                super.apply();
                final HashSet<String> enabled = new HashSet<>();

                for (Pair<String, Boolean> pair : hunspellDictionaries) {
                    if (pair.second) enabled.add(pair.first);
                }

                projectSettings.setHunspell(enabled);
            }

            @Override
            public void reset() {
                super.reset();
                setup();
            }
        };

        hunspellListPanel.setLayout(new BorderLayout());
        hunspellListPanel.add(hunspellChooser.getContentPane(), BorderLayout.CENTER);
        hunspellChooser.getEmptyText().setText(Bundle.message("no.dictionaries"));
    }

    private void setup() {
        Set current = projectSettings.getHunspell();

        hunspellDictionaries.clear();
        for (String dictionary : Hunspell.getDictionaries()) {
            hunspellDictionaries.add(Pair.create(dictionary, current.contains(dictionary)));
        }
    }

    public JComponent getPane() {
        return root;
    }

    public boolean isModified() {
        return (
            hunspellChooser.isModified() ||
            globalSettings.isSharedGlobal() != sharedGlobal.isSelected() ||
            projectSettings.isSharedProject() != sharedProject.isSelected()
        );
    }

    public void apply() {
        if (globalSettings.isSharedGlobal() != sharedGlobal.isSelected()) {
            globalSettings.setSharedGlobal(sharedGlobal.isSelected());
        }

        if (projectSettings.isSharedProject() != sharedProject.isSelected()) {
            projectSettings.setSharedProject(sharedProject.isSelected());
        }

        SpellCheckerManager.restartInspections();

        if (hunspellChooser.isModified()) hunspellChooser.apply();
    }

    public void reset() {
        sharedGlobal.setSelected(globalSettings.isSharedGlobal());
        sharedProject.setSelected(projectSettings.isSharedProject());
        hunspellChooser.reset();
    }

    @Override
    public void dispose() {}
}
