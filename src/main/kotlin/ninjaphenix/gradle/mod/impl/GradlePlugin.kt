package ninjaphenix.gradle.mod.impl

import dev.architectury.plugin.ArchitectPluginExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.configuration.FabricApiExtension
import ninjaphenix.gradle.mod.api.ext.ModGradleExtension
import ninjaphenix.gradle.mod.impl.ext.ModGradleExtensionImpl
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.concurrent.atomic.AtomicBoolean

class GradlePlugin : Plugin<Project> {
    private var validatedArchPluginVersion: AtomicBoolean = AtomicBoolean(false)
    private var validatedArchLoomVersion: AtomicBoolean = AtomicBoolean(false)
    private var validatedQuiltLoomVersion : AtomicBoolean = AtomicBoolean(false)

    override fun apply(target: Project) {
        this.validateGradleVersion(target)
        this.validateArchPluginVersion(target)
        target.apply(mapOf("plugin" to "architectury-plugin"))
        target.extensions.configure(ArchitectPluginExtension::class.java) {
            it.minecraft = Constants.MINECRAFT_VERSION
        }
        val buildTask = target.task("buildMod")
        val extensionImpl = ModGradleExtensionImpl()
        target.allprojects {
            it.extensions.add(ModGradleExtension::class.java, "mod", extensionImpl)
        }
        target.subprojects { project: Project ->
            if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
                val templateProject = TemplateProject(project, target)
                project.apply(mapOf("plugin" to "java-library"))
                project.group = "ninjaphenix"
                project.version = "${templateProject.property<String>("mod_version")}+${Constants.MINECRAFT_VERSION}"
                project.extensions.getByType(BasePluginExtension::class.java).archivesName.set(templateProject.property<String>("archives_base_name"))
                project.setBuildDir(project.rootDir.toPath().resolve("build/" + project.name))
                project.extensions.configure(JavaPluginExtension::class.java) {
                    it.sourceCompatibility = Constants.JAVA_VERSION
                    it.targetCompatibility = Constants.JAVA_VERSION
                }
                project.tasks.withType(JavaCompile::class.java).configureEach {
                    it.options.encoding = "UTF-8"
                    it.options.release.set(Constants.JAVA_VERSION.ordinal + 1)
                }
                project.repositories.maven {
                    it.name = "Unofficial CurseForge Maven"
                    it.setUrl("https://cursemaven.com")
                    it.content { desc -> desc.includeGroup("curse.maven") }
                }
                project.repositories.maven {
                    it.name = "Modrinth Maven"
                    it.setUrl("https://api.modrinth.com/maven")
                    it.content { desc -> desc.includeGroup("maven.modrinth") }
                }
                project.repositories.mavenLocal()
                project.dependencies.add("implementation", "org.jetbrains:annotations:${Constants.JETBRAINS_ANNOTATIONS_VERSION}")
                if (templateProject.producesReleaseArtifact()) {
                    buildTask.dependsOn(project.tasks.named("build"))
                }
                if (templateProject.usesDataGen()) {
                    val sourceSets = project.extensions.getByType(JavaPluginExtension::class.java).sourceSets
                    sourceSets.named("main") { it.resources.srcDir("src/main/generated") }
                    project.tasks.named("jar", Jar::class.java) { it.exclude("**/datagen") }
                }
                when (templateProject.platform) {
                    Platform.COMMON -> this.applyCommon(templateProject, target)
                    Platform.FABRIC -> this.applyFabric(templateProject, target)
                    Platform.QUILT -> this.applyQuilt(templateProject, target)
                    Platform.FORGE -> this.applyForge(templateProject, target)
                }
            }
        }
        target.subprojects {
            if (it.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
                val templateProject = TemplateProject(it, target)
                this.applyArchPlugin(it, templateProject.platform)
            }
        }
    }

    private fun applyArchPlugin(project: Project, platform: Platform) {
        project.apply(mapOf("plugin" to "architectury-plugin"))
        project.extensions.configure(ArchitectPluginExtension::class.java) {
            when (platform) {
                Platform.COMMON -> {
                    it.common()
                    it.injectInjectables = false
                }
                Platform.FABRIC, Platform.QUILT -> it.fabric()
                Platform.FORGE -> it.forge()
            }
        }
    }

    private fun applyArchLoom(project: Project) {
        project.apply(mapOf("plugin" to "dev.architectury.loom"))
        val loomPlugin = project.extensions.getByType(LoomGradleExtensionAPI::class.java)
        loomPlugin.silentMojangMappingsLicense()
        project.dependencies.apply {
            add("minecraft", "com.mojang:minecraft:${Constants.MINECRAFT_VERSION}")
            add("mappings", loomPlugin.officialMojangMappings())
        }
    }

    private fun applyCommon(templateProject: TemplateProject, target: Project) {
        this.validateArchLoomVersionIfNeeded(target)
        val project = templateProject.project
        this.applyArchLoom(project)
        project.dependencies.add("modImplementation", "net.fabricmc:fabric-loader:" + templateProject.rootProperty("fabric_loader_version"))
        project.extensions.configure(LoomGradleExtensionAPI::class.java) { extension ->
            extension.runs { runs ->
                runs.named("client") { settings -> settings.ideConfigGenerated(false) }
                runs.named("server") { settings ->
                    settings.ideConfigGenerated(false)
                    settings.serverWithGui()
                }
            }
            if (project.hasProperty("access_widener_path")) {
                extension.accessWidenerPath.set(project.file(templateProject.property("access_widener_path")))
            }
        }
    }

    private fun applyFabric(templateProject: TemplateProject, target: Project) {
        this.validateArchLoomVersionIfNeeded(target)
        val project = templateProject.project
        project.extensions.extraProperties["loom.platform"] = "fabric"
        this.applyArchLoom(project)
        project.dependencies.apply {
            add("modImplementation", "net.fabricmc:fabric-loader:" + templateProject.rootProperty("fabric_loader_version"))
            if (project.hasProperty("fabric_api_version") && project.hasProperty("fabric_api_modules")) {
                val modules = templateProject.property<String>("fabric_api_modules")
                val fabricApiVersion = templateProject.property<String>("fabric_api_version")
                if (modules == "all") {
                    add("modImplementation", "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
                } else {
                    for (module in modules.split(",")) {
                        add("modImplementation", project.extensions.getByType(FabricApiExtension::class.java).module(module, fabricApiVersion))
                        //dependencies.add("modImplementation", target.extensions.getByType(ModGradleExtension::class.java).dependencyDownloadHelper.fabricApiModule(module, fabricApiVersion))
                    }
                }
            }
        }
        project.extensions.configure(LoomGradleExtensionAPI::class.java) { extension ->
            extension.runs { runs ->
                runs.named("client") { settings -> settings.ideConfigGenerated(false) }
                runs.named("server") { settings ->
                    settings.ideConfigGenerated(false)
                    settings.serverWithGui()
                }
                if (templateProject.usesDataGen()) {
                    runs.create("datagen") { settings ->
                        settings.client()
                        settings.vmArg("-Dfabric-api.datagen")
                        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"))
                        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.rootProperty("mod_id"))
                        settings.runDir("build/" + project.name + "-datagen")
                    }
                }
            }
        }
        project.tasks.withType(ProcessResources::class.java).configureEach {
            val props = mutableMapOf<String, String>("version" to templateProject.rootProperty("mod_version"))
            it.inputs.properties(props)
            it.filesMatching("fabric.mod.json") { details: FileCopyDetails -> details.expand(props) }
            it.exclude(".cache/*")
        }
    }

    private fun applyQuilt(templateProject: TemplateProject, target: Project) {
        this.validateQuiltLoomVersionIfNeeded(target)
        val project = templateProject.project
        project.apply(mapOf("plugin" to "org.quiltmc.loom"))
        project.dependencies.apply {
            add("minecraft", "com.mojang:minecraft:" + Constants.MINECRAFT_VERSION)
            add("mappings", project.extensions.getByType(LoomGradleExtensionAPI::class.java).officialMojangMappings())
            add("modImplementation", "org.quiltmc:quilt-loader:" + templateProject.property("quilt_loader_version"))
            if (project.hasProperty("qsl_version") && project.hasProperty("qsl_modules")) {
                val modules = templateProject.property<String>("qsl_modules")
                val qslVersion = templateProject.property<String>("qsl_version")
                if (modules == "all") {
                    addProvider("modImplementation", project.provider { "org.quiltmc.qsl:qsl:" + templateProject.property("qsl_version") }) { thing: ModuleDependency ->
                        thing.exclude(mapOf("group" to "net.fabricmc"))
                    }
                } else {
                    for (module in modules.split(",")) {
                        addProvider("modImplementation", project.provider { target.extensions.getByType(ModGradleExtension::class.java).dependencyDownloadHelper.qslModule(module, qslVersion) }) { thing: ModuleDependency ->
                            thing.exclude(mapOf("group" to "net.fabricmc"))
                        }
                    }
                }
            }
            if (project.hasProperty("fabric_api_version") && project.hasProperty("fabric_api_modules")) {
                val modules = templateProject.property<String>("fabric_api_modules")
                val fabricApiVersion = templateProject.property<String>("fabric_api_version")
                if (modules == "all") {
                    addProvider("modImplementation", project.provider { "org.quiltmc.fabric_api_qsl:fabric-api:$fabricApiVersion" }) { thing: ModuleDependency ->
                        thing.exclude(mapOf("group" to "net.fabricmc"))
                    }
                } else {
                    for (module in modules.split(",")) {
                        add("modImplementation", target.extensions.getByType(ModGradleExtension::class.java).dependencyDownloadHelper.quiltedFabricApiModule(module, fabricApiVersion))
                    }
                }
            }
        }
        project.extensions.configure(LoomGradleExtensionAPI::class.java) { extension ->
            extension.runs { runs ->
                runs.named("client") { settings -> settings.ideConfigGenerated(false) }
                runs.named("server") { settings ->
                    settings.ideConfigGenerated(false)
                    settings.serverWithGui()
                }
                if (templateProject.usesDataGen()) {
                    runs.create("datagen") { settings ->
                        settings.client()
                        settings.vmArg("-Dfabric-api.datagen")
                        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"))
                        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property("mod_id"))
                        settings.runDir("build/" + project.name + "-datagen")
                    }
                }
            }
        }
        project.tasks.withType(ProcessResources::class.java).configureEach {
            val props = mutableMapOf<String, String>("version" to templateProject.rootProperty("mod_version"))
            it.inputs.properties(props)
            it.filesMatching("quilt.mod.json") { details: FileCopyDetails -> details.expand(props) }
            it.exclude(".cache/*")
        }
    }

    private fun applyForge(templateProject: TemplateProject, target: Project) {
        this.validateArchLoomVersionIfNeeded(target)
        val project = templateProject.project
        project.extensions.extraProperties["loom.platform"] = "forge"
        this.applyArchLoom(project)
        project.dependencies.add("forge", "net.minecraftforge:forge:" + Constants.MINECRAFT_VERSION + "-" + templateProject.property("forge_version"))
        project.extensions.configure(LoomGradleExtensionAPI::class.java) { extension ->
            extension.runs { runs ->
                runs.named("client") { settings -> settings.ideConfigGenerated(false) }
                runs.named("server") { settings ->
                    settings.ideConfigGenerated(false)
                    settings.serverWithGui()
                }
                //if (templateProject.usesDataGen()) {
                //    runs.create("datagen") { settings ->
                //        settings.client()
                //        settings.vmArg("-Dfabric-api.datagen")
                //        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"))
                //        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.rootProperty("mod_id"))
                //        settings.runDir("build/" + project.name + "-datagen")
                //    }
                //}
            }
        }
        project.tasks.withType(ProcessResources::class.java).configureEach {
            val props = mutableMapOf<String, String>("version" to templateProject.rootProperty("mod_version"))
            it.inputs.properties(props)
            it.filesMatching("META-INF/mods.toml") { details: FileCopyDetails -> details.expand(props) }
            it.exclude(".cache/*")
        }
    }

    private fun validateGradleVersion(target: Project) {
        val isCorrectGradleVersion = target.gradle.gradleVersion == Constants.REQUIRED_GRADLE_VERSION
        val tasks = target.gradle.startParameter.taskNames
        val isExecutingWrapperTaskOnly = tasks.size == 1 && tasks[0] == ":wrapper"
        if (!isCorrectGradleVersion && !isExecutingWrapperTaskOnly) {
            // todo: throw error when check is fixed.
            target.logger.error("This plugin requires gradle ${Constants.REQUIRED_GRADLE_VERSION} to update run: ./gradlew :wrapper --gradle-version ${Constants.REQUIRED_GRADLE_VERSION}")
            //throw IllegalStateException("This plugin requires gradle ${Constants.REQUIRED_GRADLE_VERSION} to update run: ./gradlew :wrapper --gradle-version ${Constants.REQUIRED_GRADLE_VERSION}")
        }
    }

    private fun validateArchPluginVersion(target: Project) {
        this.validatePluginVersionIfNeeded(target, validatedArchPluginVersion, "architectury-plugin", "architectury-plugin.gradle.plugin", Constants.REQUIRED_ARCH_PLUGIN_VERSION, "arch plugin")
    }

    private fun validateArchLoomVersionIfNeeded(target: Project) {
        this.validatePluginVersionIfNeeded(target, validatedArchLoomVersion, "dev.architectury", "architectury-loom", Constants.REQUIRED_ARCH_LOOM_VERSION, "arch loom")
    }

    private fun validateQuiltLoomVersionIfNeeded(target: Project) {
        //Set<ResolvedArtifact> artifacts = target.getBuildscript().getConfigurations().getByName("classpath").getResolvedConfiguration().getResolvedArtifacts();
        //for (ResolvedArtifact artifact : artifacts) {
        //    ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
        //    target.getLogger().error(identifier.getGroup() + ":" + identifier.getName() + ":" + identifier.getVersion());
        //}
        this.validatePluginVersionIfNeeded(target, validatedQuiltLoomVersion, "org.quiltmc", "quilt-loom", Constants.REQUIRED_QUILT_LOOM_VERSION, "quilt loom")
    }

    private fun validatePluginVersionIfNeeded(target: Project, validated: AtomicBoolean, pluginGroup: String, pluginName: String, requiredVersion: String, pluginFriendlyName: String) {
        if (!validated.get()) {
            val artifacts = target.buildscript.configurations.getByName("classpath").resolvedConfiguration.resolvedArtifacts
            for (artifact in artifacts) {
                val identifier = artifact.moduleVersion.id
                if (identifier.group == pluginGroup && identifier.name == pluginName) {
                    val pluginVersion = identifier.version
                    if (pluginVersion != requiredVersion) {
                        throw IllegalStateException("This plugin requires $pluginFriendlyName v$requiredVersion, current is $pluginVersion.")
                    } else {
                        validated.set(true)
                        return
                    }
                }
            }
            throw IllegalStateException("This plugin requires $pluginFriendlyName, add it to the current project un-applied.")
        }
    }
}