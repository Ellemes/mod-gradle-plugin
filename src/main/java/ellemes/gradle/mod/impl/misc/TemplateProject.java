package ellemes.gradle.mod.impl.misc;

import ellemes.gradle.mod.impl.Constants;
import org.gradle.api.Project;

import java.util.Set;
import java.util.function.Consumer;

public final class TemplateProject {
    private final Project project;
    private Set<TemplateProject> commonProjects;
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

    public void setCommonProjects(Set<TemplateProject> commonProjects) {
        if (this.commonProjects == null) {
            this.commonProjects = commonProjects;
        } else {
            throw new IllegalStateException("Tried setting common projects twice for " + project.getName());
        }
    }

    public void ifCommonProjectsPresent(Consumer<TemplateProject> consumer) {
        if (commonProjects != null) {
            for (TemplateProject commonProject : commonProjects) {
                consumer.accept(commonProject);
            }
        }
    }

    public <T> T property(String name) {
        //noinspection unchecked
        return (T) project.property(name);
    }

    public boolean containsCommonProject(TemplateProject project) {
        return this.hasCommonProjects() && commonProjects.contains(project);
    }

    public boolean hasCommonProjects() {
        return commonProjects != null;
    }
}
