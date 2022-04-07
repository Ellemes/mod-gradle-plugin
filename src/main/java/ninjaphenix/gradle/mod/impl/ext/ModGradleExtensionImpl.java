package ninjaphenix.gradle.mod.impl.ext;

import net.fabricmc.loom.configuration.FabricApiExtension;
import ninjaphenix.gradle.mod.impl.Constants;
import ninjaphenix.gradle.mod.api.ext.ModGradleExtension;
import ninjaphenix.gradle.mod.impl.DependencyDownloadHelper;
import ninjaphenix.gradle.mod.impl.Platform;
import ninjaphenix.gradle.mod.impl.TemplateProject;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.util.Map;

public class ModGradleExtensionImpl implements ModGradleExtension {
    private final Project project;
    private final DependencyDownloadHelper helper;

    public ModGradleExtensionImpl(Project project, DependencyDownloadHelper helper) {
        this.project = project;
        this.helper = helper;
    }
    @Override
    public String getMinecraftVersion() {
        return Constants.MINECRAFT_VERSION;
    }

    @Override
    public JavaVersion getJavaVersion() {
        return Constants.JAVA_VERSION;
    }

    public DependencyDownloadHelper getDependencyDownloadHelper() {
        return helper;
    }

    @Override
    public void fabricApiModules(String... modules) {
        if (!project.hasProperty(Constants.TEMPLATE_PROPERTY_KEY)) {
            throw new IllegalStateException("Only usable on a platform project");
        }
        if (!project.hasProperty(Constants.FABRIC_API_VERSION_KEY)) {
            throw new IllegalStateException("Must specify " + Constants.FABRIC_API_VERSION_KEY + " in gradle.properties");
        }
        Platform platform = ((TemplateProject) project.property(Constants.TEMPLATE_PROPERTY_KEY)).getPlatform();
        if (!(platform == Platform.FABRIC || platform == Platform.QUILT)) {
            throw new IllegalStateException("Only applicable to fabric and quilt platforms");
        }
        DependencyHandler dependencies = project.getDependencies();
        String fabricApiVersion = (String) project.property(Constants.FABRIC_API_VERSION_KEY);
        if (modules.length == 1 && modules[0].equals("all")) {
            if (platform == Platform.FABRIC) {
                dependencies.add("modImplementation", "net.fabricmc.fabric-api:fabric-api:" + fabricApiVersion);
            } else if (platform == Platform.QUILT) {
                dependencies.addProvider("modImplementation", project.provider(() -> "org.quiltmc.fabric_api_qsl:fabric-api:" + fabricApiVersion), dep -> {
                    dep.exclude(Map.of("group", "net.fabricmc"));
                });
            }
        } else {
            if (platform == Platform.FABRIC) {
                for (String module : modules) {
                    dependencies.add("modImplementation", project.getExtensions().getByType(FabricApiExtension.class).module(module, fabricApiVersion));
                    //dependencies.add("modImplementation", helper.fabricApiModule(module, fabricApiVersion));
                }
            } else if (platform == Platform.QUILT) {
                for (String module : modules) {
                    dependencies.addProvider("modImplementation", project.provider(() -> helper.quiltedFabricApiModule(module, fabricApiVersion)), dep -> {
                        dep.exclude(Map.of("group", "net.fabricmc"));
                    });
                }
            }
        }
    }

    @Override
    public void qslModules(String... modules) {
        if (!project.hasProperty(Constants.TEMPLATE_PROPERTY_KEY)) {
            throw new IllegalStateException("Only usable on a platform project");
        }
        if (!project.hasProperty(Constants.QSL_VERSION_KEY)) {
            throw new IllegalStateException("Must specify " + Constants.QSL_VERSION_KEY + " in gradle.properties");
        }
        DependencyHandler dependencies = project.getDependencies();
        String qslVersion = (String) project.property(Constants.QSL_VERSION_KEY);
        if (modules.length == 1 && modules[0].equals("all")) {
            dependencies.addProvider("modImplementation", project.provider(() -> "org.quiltmc.qsl:qsl:" + qslVersion), dep -> {
                dep.exclude(Map.of("group", "net.fabricmc"));
            });
        } else {
            for (String module : modules) {
                dependencies.addProvider("modImplementation", project.provider(() -> helper.qslModule(module, qslVersion)), dep -> {
                    dep.exclude(Map.of("group", "net.fabricmc"));
                });
            }
        }
    }
}
