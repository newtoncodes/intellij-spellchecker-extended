package com.newtoncodes.spellchecker.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;

import com.intellij.spellchecker.SpellCheckerManager;
import com.intellij.ui.OptionalChooserComponent;
import com.intellij.ui.components.JBCheckBox;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.newtoncodes.spellchecker.Provider;
import com.newtoncodes.spellchecker.states.GlobalSettingsState;
import com.newtoncodes.spellchecker.states.ProjectSettingsState;


@SuppressWarnings({"WeakerAccess", "unused"})
public class Pane implements Disposable {
    private JPanel root;
    private JBCheckBox sharedGlobal;
    private JBCheckBox sharedProject;

    private JPanel hunspellListPanel;
    private final OptionalChooserComponent<String> hunspellChooser;
    private final List<Pair<String, Boolean>> bundledDictionaries = new ArrayList<>();

    private final GlobalSettingsState globalSettings;
    private final ProjectSettingsState projectSettings;

    public Pane(GlobalSettingsState globalSettings, ProjectSettingsState projectSettings, final Project project) {
        this.globalSettings = globalSettings;
        this.projectSettings = projectSettings;

        fillBundledDictionaries();

        hunspellChooser = new OptionalChooserComponent<String>(bundledDictionaries) {
            @Override
            public JCheckBox createCheckBox(String path, boolean checked) {
                return new JCheckBox(FileUtil.toSystemDependentName(path), checked);
            }

            @Override
            public void apply() {
                super.apply();
                final HashSet<String> enabled = new HashSet<>();

                for (Pair<String, Boolean> pair : bundledDictionaries) {
                    if (pair.second) enabled.add(pair.first);
                }

                projectSettings.setHunspell(enabled);
            }

            @Override
            public void reset() {
                super.reset();
                fillBundledDictionaries();
            }
        };

        hunspellListPanel.setLayout(new BorderLayout());
        hunspellListPanel.add(hunspellChooser.getContentPane(), BorderLayout.CENTER);
        hunspellChooser.getEmptyText().setText(Bundle.message("no.dictionaries"));
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

    @SuppressWarnings("RedundantThrows")
    public void apply() throws ConfigurationException {
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

    private void fillBundledDictionaries() {
        bundledDictionaries.clear();

        try {
            for (String dictionary : Provider.getDictionaries()) {
                bundledDictionaries.add(Pair.create(dictionary, projectSettings.getHunspell().contains(dictionary)));
            }
        } catch (IOException ignored) {}
    }

    @Override
    public void dispose() {}
}
