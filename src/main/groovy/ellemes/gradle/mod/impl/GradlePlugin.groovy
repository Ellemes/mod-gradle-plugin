//file:noinspection UnnecessaryQualifiedReference
package ellemes.gradle.mod.impl

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import ellemes.gradle.mod.api.ext.ModGradleExtension
import ellemes.gradle.mod.api.task.MinifyJsonTask
import ellemes.gradle.mod.impl.dependency.DependencyDownloadHelper
import ellemes.gradle.mod.impl.ext.ModGradleExtensionImpl
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.ConfigurationVariantDetails
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.publish.maven.MavenPublication

class GradlePlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        GradlePlugin.validateGradleVersion(target)

        DependencyDownloadHelper helper = new DependencyDownloadHelper(target.projectDir.toPath().resolve(".gradle/mod-cache/"))
        Task buildTask = target.task("buildMod")
        Task releaseTask = target.tasks.create("releaseMod", ReleaseModTask.class, target.projectDir)
        GradlePlugin.configureParentProject(target)

        target.subprojects { Project project ->
            if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
                TemplateProject templateProject = new TemplateProject(project)
                Platform platform = templateProject.platform
                project.extensions.extraProperties[Constants.TEMPLATE_PLATFORM_KEY] = templateProject
                project.extensions.add(ModGradleExtension.class, "mod", new ModGradleExtensionImpl(templateProject, helper))
                project.apply(Map.of("plugin", "java-library"))
                project.group = "ellemes"
                project.version = "${project.property(Constants.MOD_VERSION_KEY)}+${project.property(Constants.MINECRAFT_VERSION_KEY)}"
                project.extensions.getByType(BasePluginExtension.class).archivesName.set("${project.property("archives_base_name")}")
                project.buildDir = target.projectDir.toPath().resolve("build/${project.name}")
                JavaVersion javaVersion = JavaVersion.toVersion(project.property(Constants.JAVA_VERSION_KEY))

                project.extensions.configure(JavaPluginExtension.class, (JavaPluginExtension extension) -> {
                    extension.sourceCompatibility = extension.targetCompatibility = javaVersion
                })

                project.tasks.withType(JavaCompile.class) { JavaCompile task ->
                    task.options.tap {
                        encoding = "UTF-8"
                        release.set(javaVersion.ordinal() + 1)
                    }
                }

                project.repositories.tap {
                    maven { MavenArtifactRepository repo ->
                        repo.name = "Unofficial CurseForge Maven"
                        repo.url = "https://cursemaven.com"
                        repo.content {
                            includeGroup("curse.maven")
                        }
                    }

                    maven { MavenArtifactRepository repo ->
                        repo.name = "Modrinth Maven"
                        repo.url = "https://api.modrinth.com/maven"
                        repo.content {
                            includeGroup("maven.modrinth")
                        }
                    }

                    mavenLocal()
                }

                project.dependencies.tap {
                    add("compileOnly", "org.jetbrains:annotations:${Constants.JETBRAINS_ANNOTATIONS_VERSION}")
                }

                if (templateProject.usesDataGen()) {
                    if (platform != Platform.FORGE) { // Arch does this for forge...
                        project.extensions.getByType(JavaPluginExtension.class).sourceSets.tap {
                            named("main") {
                                resources.srcDir("src/generated/resources")
                            }
                        }
                    }
                    project.tasks.named("jar", Jar.class) {
                        exclude("**/datagen")
                    }
                }

                if (templateProject.producesReleaseArtifact()) {
                    project.apply(Map.of("plugin", "com.modrinth.minotaur"))
                    project.apply(Map.of("plugin", "me.hypherionmc.cursegradle"))
                    releaseTask.dependsOn(project.task("releaseMod"))
                }

                if (platform == Platform.COMMON) {
                    GradlePlugin.configureCommonProject(templateProject)
                } else if (platform == Platform.FABRIC) {
                    GradlePlugin.configureFabricProject(templateProject)
                } else if (platform == Platform.QUILT) {
                    GradlePlugin.configureQuiltProject(templateProject)
                } else if (platform == Platform.FORGE) {
                    GradlePlugin.configureForgeProject(templateProject)
                }

                templateProject.ifCommonProjectPresent(common -> {
                    project.apply(Map.of("plugin", "com.github.johnrengelman.shadow"))
                    ConfigurationContainer configurations = project.configurations
                    Provider<Configuration> shadowCommonConfiguration = configurations.register("shadowCommon") {
                        canBeConsumed = false
                        canBeResolved = true
                    }

                    project.tasks.named("jar", Jar.class) {
                        archiveClassifier.set("dev")
                    }

                    Provider<ShadowJar> shadowJar = project.tasks.named("shadowJar", ShadowJar.class) {
                        exclude("architectury.common.json") // todo: useless?
                        it.configurations = List.of(shadowCommonConfiguration.get())
                        archiveClassifier.set("dev-shadow")
                    }

                    project.tasks.named("remapJar", RemapJarTask.class) {
                        if (platform != Platform.FORGE) {
                            injectAccessWidener.set(true)
                        }
                        inputFile.set(shadowJar.get().archiveFile)
                        archiveClassifier.set("fat")
                        dependsOn(shadowJar)
                    }

                    project.components.named("java", AdhocComponentWithVariants.class) {
                        withVariantsFromConfiguration(configurations["shadowRuntimeElements"], ConfigurationVariantDetails::skip)
                    }
                })

                String modInfoFile = platform.modInfoFile
                if (modInfoFile != null) {
                    project.tasks.withType(ProcessResources.class).configureEach(task -> {
                        HashMap<String, String> props = new HashMap<>()
                        props.put("version", "${project.property(Constants.MOD_VERSION_KEY)}")
                        if (project.hasProperty(Constants.TEMPLATE_EXTRA_MOD_INFO_REPLACEMENTS_KEY)) {
                            Map<String, String> extraProps = templateProject.property(Constants.TEMPLATE_EXTRA_MOD_INFO_REPLACEMENTS_KEY)
                            if (extraProps != null) {
                                props.putAll(extraProps)
                            }
                        }
                        task.inputs.properties(props)
                        task.filesMatching(modInfoFile) {
                            expand(props)
                        }
                        task.exclude(".cache/*")
                    })
                }
            }
        }

        target.subprojects { Project project ->
            if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
                TemplateProject templateProject = new TemplateProject(project);
                templateProject.ifCommonProjectPresent {Project common ->
                    if (common.hasProperty(Constants.ACCESS_WIDENER_KEY)) {
                        project.extensions.getByType(LoomGradleExtensionAPI.class).tap {
                            accessWidenerPath.set(common.extensions.getByType(LoomGradleExtensionAPI.class).accessWidenerPath)
                        }
                    }

                    DependencyHandler dependencies = project.dependencies
                    ModuleDependency commonDep = ((ProjectDependency) dependencies.project(Map.of("path", common.path, "configuration", "namedElements"))).setTransitive(false)
                    ModuleDependency shadowCommonDep = ((ProjectDependency) dependencies.project(Map.of("path", common.path, "configuration", "namedElements"))).setTransitive(false)
                    dependencies.tap {
                        add("implementation", commonDep)
                        add("shadowCommon", shadowCommonDep)
                    }
                }

                if (templateProject.producesReleaseArtifact()) {
                    buildTask.dependsOn(project.tasks["build"])

                    Provider<MinifyJsonTask> minJarTask = project.tasks.register("minJar", MinifyJsonTask.class) {
                            Task remapJarTask = project.tasks["remapJar"]
                            input.set(remapJarTask.outputs.files.singleFile)
                            archiveClassifier.set(project.name)
                            from(target.projectDir.toPath().resolve("LICENSE"))
                            dependsOn(remapJarTask)
                    }

                    project.tasks["build"].dependsOn(minJarTask)
                }

                if (templateProject.producesMavenArtifact()) {
                    project.apply(Map.of("plugin", "maven-publish"));

                    project.extensions.getByType(PublishingExtension.class).tap {
                        publications {
                            create("maven${project.name.capitalize()}", MavenPublication.class) {
                                artifactId = "${project.property("template.maven_artifact_id")}-${project.property(Constants.MINECRAFT_VERSION_KEY)}-${project.name}"
                                version = "${project.property("mod_version")}"
                                var minifyJar = project.tasks["minJar"]
                                artifact(minifyJar) {
                                    builtBy(minifyJar)
                                    classifier = ""
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void validateGradleVersion(Project target) {
        boolean isCorrectGradleVersion = target.gradle.gradleVersion == Constants.REQUIRED_GRADLE_VERSION
        List<String> tasks = target.gradle.startParameter.taskNames
        boolean isExecutingWrapperTaskOnly = tasks.size() == 3 && tasks[0] == ":wrapper" && tasks[1] == "--gradle-version" && tasks[2] == Constants.REQUIRED_GRADLE_VERSION
        if (!isCorrectGradleVersion && !isExecutingWrapperTaskOnly) {
            throw new IllegalStateException("This plugin requires gradle ${Constants.REQUIRED_GRADLE_VERSION} to update run: ./gradlew :wrapper --gradle-version ${Constants.REQUIRED_GRADLE_VERSION}")
        }
    }

    private static void configureParentProject(Project project) {
        project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
            graph.allTasks.forEach {
                if (it instanceof ReleaseModTask) {
                    (it as ReleaseModTask).doPreReleaseChecks()
                }
            }
        }
    }

    private static void applyArchLoom(TemplateProject templateProject) {
        Project project = templateProject.project
        project.apply(Map.of("plugin", "dev.architectury.loom"))
        LoomGradleExtensionAPI loom = project.extensions.getByType(LoomGradleExtensionAPI.class)
        loom.tap {
            silentMojangMappingsLicense()
            mods {
                register("main") {
                    SourceSet main = project.extensions.getByType(JavaPluginExtension.class).sourceSets["main"]
                    sourceSet(main)
                    templateProject.ifCommonProjectPresent(common -> {
                        configuration(project.configurations["shadowCommon"])
                    })
                }
            }
        }

        project.dependencies.tap {
            add("minecraft", "com.mojang:minecraft:${project.property(Constants.MINECRAFT_VERSION_KEY)}")
            add("mappings", loom.officialMojangMappings())
        }
    }

    private static void configureCommonProject(TemplateProject templateProject) {
        Project project = templateProject.project
        GradlePlugin.applyArchLoom(templateProject)
        project.dependencies.tap {
            add("modImplementation", "net.fabricmc:fabric-loader:${project.property(Constants.FABRIC_LOADER_VERSION_KEY)}")
        }

        project.extensions.getByType(LoomGradleExtensionAPI.class).tap {
            runs {
                named("client") {
                    ideConfigGenerated(false)
                }
                named("server") {
                    ideConfigGenerated(false)
                    serverWithGui()
                }
            }

            if (project.hasProperty(Constants.ACCESS_WIDENER_KEY)) {
                accessWidenerPath.set(project.file(project.property(Constants.ACCESS_WIDENER_KEY)))
            }
        }
    }

    private static void configureFabricProject(TemplateProject templateProject) {
        Project project = templateProject.project
        project.extensions.extraProperties["loom.platform"] = "fabric"
        GradlePlugin.applyArchLoom(templateProject)
        project.dependencies.tap {
            add("modImplementation", "net.fabricmc:fabric-loader:${project.property(Constants.FABRIC_LOADER_VERSION_KEY)}")
        }
        project.extensions.getByType(LoomGradleExtensionAPI.class).tap {
            runs {
                named("client") {
                    ideConfigGenerated(false)
                }
                named("server") {
                    ideConfigGenerated(false)
                    serverWithGui()
                }
                if (templateProject.usesDataGen()) {
                    register("datagen") {
                        client()
                        vmArg("-Dfabric-api.datagen")
                        vmArg("-Dfabric-api.datagen.output-dir=${project.file("src/generated/resources")}")
                        vmArg("-Dfabric-api.datagen.datagen.modid=${project.property("mod_id")}")
                        runDir("build/${project.name}-datagen")
                    }
                }
            }

        }
    }

    private static void configureQuiltProject(TemplateProject templateProject) {
        Project project = templateProject.project
        project.extensions.extraProperties["loom.platform"] = "quilt"
        GradlePlugin.applyArchLoom(templateProject)
        project.repositories.tap {
            maven { MavenArtifactRepository repo ->
                repo.name = "Quilt Release Maven"
                repo.url = "https://maven.quiltmc.org/repository/release/"
            }

            maven { MavenArtifactRepository repo ->
                repo.name = "Quilt Snapshot Maven"
                repo.url = "https://maven.quiltmc.org/repository/snapshot/"
            }
        }
        project.dependencies.tap {
            add("modImplementation", "org.quiltmc:quilt-loader:${project.property(Constants.QUILT_LOADER_VERSION_KEY)}")
        }
        project.extensions.getByType(LoomGradleExtensionAPI.class).tap {
            runs {
                named("client") {
                    ideConfigGenerated(false)
                }
                named("server"){
                    ideConfigGenerated(false)
                    serverWithGui()
                }
                if (templateProject.usesDataGen()) {
                    register("datagen") {
                        client()
                        vmArg("-Dfabric-api.datagen")
                        vmArg("-Dfabric-api.datagen.output-dir=${project.file("src/generated/resources")}")
                        vmArg("-Dfabric-api.datagen.datagen.modid=${project.property("mod_id")}")
                        runDir("build/${project.name}-datagen")
                    }
                }
            }
        }
    }

    private static void configureForgeProject(TemplateProject templateProject) {
        Project project = templateProject.project
        project.extensions.extraProperties["loom.platform"] = "forge"
        GradlePlugin.applyArchLoom(templateProject)
        project.dependencies.tap {
            add("forge", "net.minecraftforge:forge:${project.property(Constants.MINECRAFT_VERSION_KEY)}-${project.property(Constants.FORGE_VERSION_KEY)}")
        }

        project.extensions.getByType(LoomGradleExtensionAPI.class).tap {
            if (templateProject.usesDataGen()) {
                forge {
                    dataGen {
                        mod("${project.property("mod_id")}")
                    }
                }
            }

            runs {
                named("client") {
                    ideConfigGenerated(false)
                }
                named("server") {
                    ideConfigGenerated(false)
                    serverWithGui()
                }
                if (templateProject.usesDataGen()) {
                    named("data") {
                        programArg("--existing")
                        programArg(project.file("src/main/resources").absolutePath)
                        templateProject.ifCommonProjectPresent {Project commonProject ->
                            programArg("--existing")
                            programArg(commonProject.file("src/main/resources").absolutePath)
                        }
                        runDir("build/${project.name}-datagen")
                    }
                }
            }
        }
    }
}
