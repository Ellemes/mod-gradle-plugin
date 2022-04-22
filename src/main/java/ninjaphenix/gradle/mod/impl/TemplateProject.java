package ninjaphenix.gradle.mod.impl;

import org.gradle.api.Project;

import java.util.function.Consumer;

public final class TemplateProject {
    private final Project project;
    private final Project commonProject;
    private Boolean producesReleaseArtifact, producesMavenArtifact;
    private Boolean usesDataGen;
    private Platform platform;

    public TemplateProject(Project project) {
        this.project = project;
        if (project.hasProperty(Constants.TEMPLATE_COMMON_PROJECT_KEY)) {
            this.commonProject = project.getParent().getChildProjects().get(this.<String>property(Constants.TEMPLATE_COMMON_PROJECT_KEY));
        } else {
            this.commonProject = null;
        }
    }

    public Project getProject() {
        return project;
    }

    public boolean producesReleaseArtifact() {
        if (producesReleaseArtifact == null) {
            producesReleaseArtifact = project.hasProperty("template.producesReleaseArtifact");
        }
        return producesReleaseArtifact;
    }

    public boolean producesMavenArtifact() {
        if (producesMavenArtifact == null) {
            producesMavenArtifact = project.hasProperty("template.producesMavenArtifact");
        }
        return producesMavenArtifact;
    }

    public boolean usesDataGen() {
        if (usesDataGen == null) {
            usesDataGen = project.hasProperty("template.usesDataGen");
        }
        return usesDataGen;
    }

    public Platform getPlatform() {
        if (platform == null) {
            platform = Platform.of(this.property(Constants.TEMPLATE_PLATFORM_KEY));
        }
        return platform;
    }

    public void ifCommonProjectPresent(Consumer<Project> consumer) {
        if (commonProject != null) {
            consumer.accept(commonProject);
        }
    }

    public <T> T property(String name) {
        //noinspection unchecked
        return (T) project.property(name);
    }

    public <T> T rootProperty(String name) {
        //noinspection unchecked
        return (T) project.getRootProject().property(name);
    }
}
