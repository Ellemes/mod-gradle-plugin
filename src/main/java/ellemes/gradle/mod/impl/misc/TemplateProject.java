package ellemes.gradle.mod.impl.misc;

import ellemes.gradle.mod.impl.Constants;
import org.gradle.api.Project;

import java.util.function.Consumer;

public final class TemplateProject {
    private final Project project;
    private TemplateProject commonProject;
    private Boolean producesReleaseArtifact, producesMavenArtifact;
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

    public void setCommonProject(TemplateProject commonProject) {
        if (this.commonProject == null) {
            this.commonProject = commonProject;
        } else {
            throw new IllegalStateException("Tried setting common project twice for " + project.getName());
        }
    }

    public void ifCommonProjectPresent(Consumer<TemplateProject> consumer) {
        if (commonProject != null) {
            consumer.accept(commonProject);
        }
    }

    public <T> T property(String name) {
        //noinspection unchecked
        return (T) project.property(name);
    }

    public TemplateProject getCommonProject() {
        return commonProject;
    }
}
