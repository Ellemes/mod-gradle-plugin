package ninjaphenix.gradle.mod.impl;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public final class TemplateProject {
    private final Project project;
    private final Project rootProject;
    private Boolean producesReleaseArtifact;
    private Boolean usesDataGen;
    private Platform platform;
    //private final Project commonProject;

    public TemplateProject(Project project, Project rootProject) {
        this.project = project;
        this.rootProject = rootProject;
        //if (project.hasProperty(Constants.TEMPLATE_COMMON_PROJECT_KEY)) {
        //    this.commonProject = rootProject.getChildProjects().get(this.<String>property(Constants.TEMPLATE_COMMON_PROJECT_KEY));
        //} else {
        //    this.commonProject = null;
        //}
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    public boolean producesReleaseArtifact() {
        if (producesReleaseArtifact == null) {
            producesReleaseArtifact = project.hasProperty("template.producesReleaseArtifact");
        }
        return producesReleaseArtifact;
    }

    public boolean usesDataGen() {
        if (usesDataGen == null) {
            usesDataGen = project.hasProperty("template.usesDataGen");
        }
        return usesDataGen;
    }

    @NotNull
    public Platform getPlatform() {
        if (platform == null) {
            platform = Platform.of(this.property(Constants.TEMPLATE_PLATFORM_KEY));
        }
        return platform;
    }

    //public void ifCommonProjectPresent(Consumer<Project> consumer) {
    //    if (commonProject != null) {
    //        consumer.accept(commonProject);
    //    }
    //}

    public <T> T property(String name) {
        //noinspection unchecked
        return (T) project.property(name);
    }

    public <T> T rootProperty(String name) {
        //noinspection unchecked
        return (T) rootProject.property(name);
    }
}
