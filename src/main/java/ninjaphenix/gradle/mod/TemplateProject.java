package ninjaphenix.gradle.mod;

import org.gradle.api.Project;

public final class TemplateProject {
    private final Project project;
    private Boolean producesReleaseArtifact;
    private Boolean usesCommonSourceSet;
    private Boolean usesDataGen;
    private Platform platform;

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

    public boolean usesCommonSourceSet() {
        if (usesCommonSourceSet == null) {
            usesCommonSourceSet = project.hasProperty("template.usesCommonSourceSet");
        }
        return usesCommonSourceSet;
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

    public <T> T property(String name) {
        //noinspection unchecked
        return (T) project.property(name);
    }
}
