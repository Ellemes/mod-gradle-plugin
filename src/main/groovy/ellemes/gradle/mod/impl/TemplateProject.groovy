 package ellemes.gradle.mod.impl

import org.gradle.api.Project

import java.util.function.Consumer

final class TemplateProject {
    private final Project project
    private final TemplateProject commonProject
    private Boolean producesReleaseArtifact, producesMavenArtifact
    private Boolean usesDataGen
    private Platform platform

    TemplateProject(Project project) {
        this.project = project
        if (project.hasProperty(Constants.Keys.Template.COMMON_PROJECT)) {
            def cProject = project.parent.childProjects["${this.property(Constants.Keys.Template.COMMON_PROJECT)}"]
            commonProject = GradlePlugin.getOrSetTemplateProject(cProject)
        } else {
            commonProject = null
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
            platform = Platform.of(this.property(Constants.Keys.Template.PLATFORM))
        }
        return platform
    }

    void ifCommonProjectPresent(Consumer<TemplateProject> consumer) {
        if (commonProject != null) {
            consumer.accept(commonProject)
        }
    }

    def <T> T property(String name) {
        return (T) project.property(name)
    }

    String getModId() {
        property("mod_id")
    }

    String getMinecraftVersion() {
        property("minecraft_version")
    }

    String getModVersion() {
        property("mod_version")
    }
}
