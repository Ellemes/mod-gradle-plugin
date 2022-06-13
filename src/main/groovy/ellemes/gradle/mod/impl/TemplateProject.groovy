 package ellemes.gradle.mod.impl

import org.gradle.api.Project

import java.util.function.Consumer

final class TemplateProject {
    private final Project project
    private final Project commonProject
    private Boolean producesReleaseArtifact, producesMavenArtifact
    private Boolean usesDataGen
    private Platform platform

    TemplateProject(Project project) {
        this.project = project
        if (project.hasProperty(Constants.TEMPLATE_COMMON_PROJECT_KEY)) {
            this.commonProject = project.parent.childProjects["${this.property(Constants.TEMPLATE_COMMON_PROJECT_KEY)}"]
        } else {
            this.commonProject = null
        }
    }

    Project getProject() {
        return project
    }

    boolean producesReleaseArtifact() {
        if (producesReleaseArtifact == null) {
            producesReleaseArtifact = project.hasProperty("template.producesReleaseArtifact")
        }
        return producesReleaseArtifact
    }

    boolean producesMavenArtifact() {
        if (producesMavenArtifact == null) {
            producesMavenArtifact = project.hasProperty("template.producesMavenArtifact")
        }
        return producesMavenArtifact
    }

    boolean usesDataGen() {
        if (usesDataGen == null) {
            usesDataGen = project.hasProperty("template.usesDataGen")
        }
        return usesDataGen
    }

    Platform getPlatform() {
        if (platform == null) {
            platform = Platform.of(this.property(Constants.TEMPLATE_PLATFORM_KEY))
        }
        return platform
    }

    void ifCommonProjectPresent(Consumer<Project> consumer) {
        if (commonProject != null) {
            consumer.accept(commonProject)
        }
    }

    def <T> T property(String name) {
        return (T) project.property(name)
    }
}
