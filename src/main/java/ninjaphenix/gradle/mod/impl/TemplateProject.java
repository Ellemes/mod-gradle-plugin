package ninjaphenix.gradle.mod.impl;

import org.gradle.api.Project;

import java.util.function.Consumer;

public final class TemplateProject {
    private final Project project;
    private Boolean producesReleaseArtifact;
    private Boolean usesDataGen, usesMixins, usesAccessTransformers;
    private Platform platform;
    private final Project commonProject;

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

    public boolean usesDataGen() {
        if (usesDataGen == null) {
            usesDataGen = project.hasProperty("template.usesDataGen");
        }
        return usesDataGen;
    }

    // Forge only
    public boolean usesMixins() {
        if (usesMixins == null) {
            usesMixins = project.hasProperty("template.usesMixins");
        }
        return usesMixins;
    }

    // Forge only.
    public boolean usesAccessTransformers() {
        if (usesAccessTransformers == null) {
            usesAccessTransformers = project.hasProperty("template.usesAccessTransformers");
        }
        return usesAccessTransformers;
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
}
