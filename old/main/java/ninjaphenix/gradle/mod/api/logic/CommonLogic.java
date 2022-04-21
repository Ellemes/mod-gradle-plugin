package ninjaphenix.gradle.mod.api.logic;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import ninjaphenix.gradle.mod.impl.TemplateProject;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class CommonLogic {

    public static void apply(Project project, LoomGradleExtensionAPI loom) {
        //this.validateArchLoomVersionIfNeeded(project);
        TemplateProject templateProject = new TemplateProject(project);
        ArchLogic.applyLoom(project);
        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("modImplementation", "net.fabricmc:fabric-loader:" + templateProject.rootProperty("fabric_loader_version"));

        project.getExtensions().configure(LoomGradleExtensionAPI.class, extension -> {
            extension.runs(container -> {
                container.named("client", settings -> settings.ideConfigGenerated(false));
                container.named("server", settings -> {
                    settings.ideConfigGenerated(false);
                    settings.serverWithGui();
                });
            });

            if (project.hasProperty("access_widener_path")) {
                extension.getAccessWidenerPath().set(project.file(templateProject.property("access_widener_path")));
            }
        });
    }
}
