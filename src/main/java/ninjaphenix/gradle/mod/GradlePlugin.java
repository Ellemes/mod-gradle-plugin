package ninjaphenix.gradle.mod;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.util.HashMap;
import java.util.Map;

public final class GradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        // todo: validate this is the only task.
        if (!target.getGradle().getGradleVersion().equals(Constants.REQUIRED_GRADLE_VERSION) && target.getGradle().getStartParameter().getTaskNames().stream().noneMatch(it -> it.equals(":wrapper"))) {
            throw new IllegalStateException("This plugin requires gradle " + Constants.REQUIRED_GRADLE_VERSION + " to update run: ./gradlew :wrapper --gradle-version " +  Constants.REQUIRED_GRADLE_VERSION);
        }

        Task buildTask = target.task("buildMod");

        target.subprojects(project -> {
            if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
                TemplateProject templateProject = new TemplateProject(project);
                project.apply(Map.of("plugin", "java-library"));
                project.setGroup("ninjaphenix");
                project.setVersion(templateProject.property("mod_version") + "+" + Constants.MINECRAFT_VERSION);
                project.getExtensions().getByType(BasePluginExtension.class).getArchivesName().set(templateProject.<String>property("archives_base_name"));
                project.setBuildDir(project.getRootDir().toPath().resolve("build/" + project.getName()));

                project.getExtensions().configure(JavaPluginExtension.class, extension -> {
                    extension.setSourceCompatibility(Constants.JAVA_VERSION);
                    extension.setTargetCompatibility(Constants.JAVA_VERSION);
                });

                if (templateProject.usesCommonSourceSet()) {
                    SourceSetContainer sourceSets =  project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
                    sourceSets.named("main").configure(sourceSet -> {
                        sourceSet.getJava().srcDir("commonSrc/main/java");
                        sourceSet.getResources().srcDir("commonSrc/main/resources");
                    });
                }

                project.getTasks().withType(JavaCompile.class).configureEach(task -> task.getOptions().setEncoding("UTF-8"));

                project.getDependencies().add("implementation", "org.jetbrains:annotations:" + Constants.JETBRAINS_ANNOTATIONS_VERSION);

                if (templateProject.producesReleaseArtifact()) {
                    buildTask.dependsOn(project.getTasks().getByName("build"));
                }

                if (templateProject.getPlatform() == Platform.FABRIC) {
                    this.applyFabric(templateProject);
                }
            }
        });
    }

    private void applyFabric(TemplateProject templateProject) {
        Project project = templateProject.getProject();
        project.apply(Map.of("plugin", "fabric-loom"));
        SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        if (templateProject.usesDataGen()) {
            sourceSets.named("main", sourceSet -> sourceSet.getResources().srcDir("src/main/generated"));

            sourceSets.create("datagen", sourceSet -> {
                sourceSet.getCompileClasspath().plus(sourceSets.getByName("main").getCompileClasspath());
                sourceSet.getRuntimeClasspath().plus(sourceSets.getByName("main").getCompileClasspath());
                sourceSet.getCompileClasspath().plus(sourceSets.getByName("main").getOutput());
                sourceSet.getRuntimeClasspath().plus(sourceSets.getByName("main").getOutput());
            });
        }

        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("minecraft", "com.mojang:minecraft:" + Constants.MINECRAFT_VERSION);
        dependencies.add("mappings", "net.fabricmc:yarn:" + Constants.MINECRAFT_VERSION + "+build." + Constants.YARN_VERSION + ":v2");
        dependencies.add("modImplementation", "net.fabricmc:fabric-loader:" + templateProject.property("fabric_loader_version"));
        if (project.hasProperty("fabric_api_version")) {
            dependencies.add("modImplementation", "net.fabricmc.fabric-api:fabric-api:" + templateProject.property("fabric_api_version"));
        }

        project.getExtensions().configure(LoomGradleExtensionAPI.class, extension -> {
            extension.runs(container -> {
                container.named("client", settings -> settings.ideConfigGenerated(false));
                container.named("server", settings -> {
                    settings.ideConfigGenerated(false);
                    settings.serverWithGui();
                });
                if (templateProject.usesDataGen()) {
                    container.create("datagen", settings -> {
                        settings.client();
                        settings.vmArg("-Dfabric-api.datagen");
                        // todo: needs checking.
                        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"));
                        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property("mod_id"));
                        settings.runDir("build/fabric-datagen");
                        settings.source(sourceSets.getByName("datagen"));
                    });
                }
            });

            //noinspection UnstableApiUsage
            extension.getMixin().getUseLegacyMixinAp().set(false);

            if (project.hasProperty("access_widener_path")) {
                extension.getAccessWidenerPath().set(project.file(templateProject.property("access_widener_path")));
            }
        });

        //noinspection UnstableApiUsage
        project.getTasks().withType(ProcessResources.class).configureEach(task -> {
            HashMap<String, String> props = new HashMap<>();
            props.put("version", templateProject.property("mod_version"));
            task.getInputs().properties(props);
            task.filesMatching("fabric.mod.json", details -> details.expand(props));
            task.exclude(".cache/*");
        });
    }
}
