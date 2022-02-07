package ninjaphenix.gradle.mod;

import org.gradle.api.Project;

import java.util.Optional;

public final class TemplateProject {
    private final Project project;
    private Boolean producesReleaseArtifact;
    private Boolean usesDataGen, usesMixins, usesAccessTransformers;
    private Platform platform;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<Project> commonProject;

    public TemplateProject(Project project) {
        this.project = project;
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

    public Optional<Project> getCommonProject() {
        //noinspection OptionalAssignedToNull
        if (commonProject == null) {
            if (project.hasProperty(Constants.TEMPLATE_COMMON_PROJECT_KEY)) {
                // Read bottom up :)
                // Also, why do I refer to myself as we...
                // Does this hold up for a child project as the mod root?
                // noinspection ConstantConditions Maybe we should throw an exception here, invalid gradle user configuration.
                commonProject = Optional.ofNullable(project.getParent().getChildProjects().get(this.<String>property(Constants.TEMPLATE_COMMON_PROJECT_KEY)));
            } else {
                commonProject = Optional.empty();
            }
        }
        return commonProject;
    }

    public <T> T property(String name) {
        //noinspection unchecked
        return (T) project.property(name);
    }
}
