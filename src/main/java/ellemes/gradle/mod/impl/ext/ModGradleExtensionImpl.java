package ellemes.gradle.mod.impl.ext;

import ellemes.gradle.mod.api.helpers.FabricApiHelper;
import ellemes.gradle.mod.api.helpers.QslHelper;
import ellemes.gradle.mod.impl.dependency.DependencyDownloadHelper;
import ellemes.gradle.mod.api.ext.ModGradleExtension;
import ellemes.gradle.mod.impl.Constants;
import ellemes.gradle.mod.impl.Platform;
import ellemes.gradle.mod.impl.TemplateProject;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.text.MessageFormat;
import java.util.Map;

public final class ModGradleExtensionImpl implements ModGradleExtension {
    private final TemplateProject templateProject;
    private final Project project;
    private final DependencyDownloadHelper helper;
    private QslHelper qslHelper;

    public ModGradleExtensionImpl(TemplateProject project, DependencyDownloadHelper helper) {
        this.templateProject = project;
        this.project = project.getProject();
        this.helper = helper;
    }

    @Override
    public void fabricApi(String... modules) {
        if (!project.hasProperty(Constants.TEMPLATE_PROPERTY_KEY)) {
            throw new IllegalStateException("Only usable on a platform project");
        }
        if (!project.hasProperty(Constants.FABRIC_API_VERSION_KEY)) {
            throw new IllegalStateException("Must specify " + Constants.FABRIC_API_VERSION_KEY + " in gradle.properties");
        }
        Platform platform = templateProject.getPlatform();
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
                    dependencies.add("modImplementation", helper.fabricApi(module, fabricApiVersion));
                }
            } else if (platform == Platform.QUILT) {
                for (String module : modules) {
                    dependencies.addProvider("modImplementation", project.provider(() -> helper.quiltedFabricApi(module, fabricApiVersion)), dep -> {
                        dep.exclude(Map.of("group", "net.fabricmc"));
                    });
                }
            }
        }
    }

    @Override
    public void qsl(String... modules) {
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
                dependencies.addProvider("modImplementation", project.provider(() -> helper.qsl(module, qslVersion)), dep -> {
                    dep.exclude(Map.of("group", "net.fabricmc"));
                });
            }
        }
    }

    @Override
    public QslHelper qsl() {
        if (qslHelper == null) {
            qslHelper = new QslHelper() {
                @Override
                public String library(String libraryName) {
                    return null;
                }

                @Override
                public String module(String libraryName, String moduleName) {
                    return null;
                }
            };
        }
        return qslHelper;
    }

    @Override
    public FabricApiHelper fabricApi() {
        Platform platform = templateProject.getPlatform();
        String fabricApiVersion = templateProject.property("fabric_api_version");
        if (platform == Platform.QUILT) {
            return new FabricApiHelper() {
                @Override
                public String module(String moduleName) {
                    return MessageFormat.format("org.quiltmc.quilted-fabric-api:{}:{}", moduleName, fabricApiVersion);
                }

                @Override
                public String full() {
                    return "org.quiltmc.quilted-fabric-api:quilted-fabric-api:" + fabricApiVersion;
                }

                @Override
                public String deprecated() {
                    return "org.quiltmc.quilted-fabric-api:quilted-fabric-api-deprecated:" + fabricApiVersion;
                }
            };
        }

        return new FabricApiHelper() {
            @Override
            public String module(String moduleName) {
                return helper.fabricApi(moduleName, fabricApiVersion);
            }

            @Override
            public String full() {
                return "net.fabricmc.fabric-api:fabric-api:" + fabricApiVersion;
            }

            @Override
            public String deprecated() {
                return "net.fabricmc.fabric-api:fabric-api-deprecated:" + fabricApiVersion;
            }
        };
    }
}
