package ninjaphenix.gradle.mod;

import org.gradle.api.Project;

import java.util.Optional;

public final class TemplateProject {
    private final Project project;
    private Boolean producesReleaseArtifact;
    private Boolean usesDataGen;
    private Platform platform;
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

    public Platform getPlatform() {
        if (platform == null) {
            platform = Platform.of(this.property(Constants.TEMPLATE_PLATFORM_KEY));
        }
        return platform;
    }

    public Optional<Project> getCommonProject() {
        if (commonProject == null) {
            if (project.hasProperty("template.commonProject")) {
                commonProject = Optional.ofNullable(project.getRootProject().getChildProjects().get(this.<String>property("template.commonProject")));
            }
        }
        return commonProject;
    }

    public <T> T property(String name) {
        //noinspection unchecked
        return (T) project.property(name);
    }
}
