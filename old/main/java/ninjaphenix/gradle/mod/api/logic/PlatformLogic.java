package ninjaphenix.gradle.mod.api.logic;

import ninjaphenix.gradle.mod.api.ext.ModGradleExtension;
import ninjaphenix.gradle.mod.impl.Constants;
import ninjaphenix.gradle.mod.impl.TemplateProject;
import ninjaphenix.gradle.mod.impl.ext.ModGradleExtensionImpl;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;

import java.util.Map;

public class PlatformLogic {
    public static void apply(Project project) {
        TemplateProject templateProject = new TemplateProject(project);
        project.getExtensions().getExtraProperties().set(Constants.TEMPLATE_PROPERTY_KEY, templateProject);
        project.getExtensions().add(ModGradleExtension.class, "mod", new ModGradleExtensionImpl(project, project.getParent().getExtensions().getByType(ModGradleExtension.class).getDependencyDownloadHelper()));
        project.apply(Map.of("plugin", "java-library"));
        project.setGroup("ninjaphenix");
        project.setVersion(templateProject.property("mod_version") + "+" + Constants.MINECRAFT_VERSION);
        project.getExtensions().getByType(BasePluginExtension.class).getArchivesName().set(templateProject.<String>property("archives_base_name"));
        project.setBuildDir(project.getRootDir().toPath().resolve("build/" + project.getName()));

        project.getExtensions().configure(JavaPluginExtension.class, extension -> {
            extension.setSourceCompatibility(Constants.JAVA_VERSION);
            extension.setTargetCompatibility(Constants.JAVA_VERSION);
        });

        project.getTasks().withType(JavaCompile.class).configureEach(task -> {
            CompileOptions options = task.getOptions();
            options.setEncoding("UTF-8");
            options.getRelease().set(Constants.JAVA_VERSION.ordinal() + 1);
        });

        project.getRepositories().maven(repo -> {
            repo.setName("Unofficial CurseForge Maven");
            repo.setUrl("https://cursemaven.com");
            repo.content(descriptor -> descriptor.includeGroup("curse.maven"));
        });

        project.getRepositories().maven(repo -> {
            repo.setName("Modrinth Maven");
            repo.setUrl("https://api.modrinth.com/maven");
            repo.content(descriptor -> descriptor.includeGroup("maven.modrinth"));
        });

        project.getRepositories().mavenLocal();

        project.getDependencies().add("implementation", "org.jetbrains:annotations:" + Constants.JETBRAINS_ANNOTATIONS_VERSION);

        if (templateProject.producesReleaseArtifact()) {
        //    buildTask.dependsOn(project.getTasks().getByName("build"));
        }

        if (templateProject.usesDataGen()) {
            SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
            sourceSets.named("main", sourceSet -> sourceSet.getResources().srcDir("src/main/generated"));
            project.getTasks().getByName("jar", task -> ((Jar) task).exclude("**/datagen"));
        }

        //if (templateProject.getPlatform() == Platform.COMMON) {
        //    this.applyCommon(templateProject, target);
        //} else if (templateProject.getPlatform() == Platform.FABRIC) {
        //    this.applyFabric(templateProject, target);
        //} else if (templateProject.getPlatform() == Platform.QUILT) {
        //    this.applyQuilt(templateProject, target);
        //} else if (templateProject.getPlatform() == Platform.FORGE) {
        //    this.applyForge(templateProject, target);
        //}
    }
}
