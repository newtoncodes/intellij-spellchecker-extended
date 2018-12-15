package com.newtoncodes.spellchecker.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;

import com.newtoncodes.spellchecker.states.GlobalSettingsState;
import com.newtoncodes.spellchecker.states.ProjectSettingsState;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;


@SuppressWarnings("CanBeFinal")
class Manager implements SearchableConfigurable, Configurable.NoScroll {
    private Pane pane;
    private ProjectSettingsState settingsProject;
    private GlobalSettingsState settingsGlobal;
    private Project project;

    public Manager(Project project, GlobalSettingsState settingsGlobal, ProjectSettingsState settingsProject) {
        this.project = project;
        this.settingsProject = settingsProject;
        this.settingsGlobal = settingsGlobal;
    }

    @Override
    @Nls
    public String getDisplayName() {
        return Bundle.message("spelling.extended");
    }

    @Override
    @NotNull
    @NonNls
    public String getHelpTopic() {
        return "reference.settings.ide.settings.spelling.extended";
    }

    @Override
    @NotNull
    public String getId() {
        return getHelpTopic();
    }

    @Override
    public JComponent createComponent() {
        if (pane == null) pane = new Pane(settingsGlobal, settingsProject, project);
        return pane.getPane();
    }

    @Override
    public boolean isModified() {
        return pane == null || pane.isModified();
    }

    @Override
    public void apply() {
        if (pane != null) pane.apply();
    }

    @Override
    public void reset() {
        if (pane != null) pane.reset();
    }

    @Override
    public void disposeUIResources() {
        pane = null;
    }
}
