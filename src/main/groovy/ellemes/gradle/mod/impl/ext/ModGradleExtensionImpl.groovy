package ellemes.gradle.mod.impl.ext

import ellemes.gradle.mod.api.ext.ModGradleExtension
import ellemes.gradle.mod.api.helpers.FabricApiHelper
import ellemes.gradle.mod.api.helpers.QslHelper
import ellemes.gradle.mod.impl.Constants
import ellemes.gradle.mod.impl.Platform
import ellemes.gradle.mod.impl.TemplateProject
import ellemes.gradle.mod.impl.dependency.DependencyDownloadHelper
import org.gradle.api.Project

final class ModGradleExtensionImpl implements ModGradleExtension {
    private TemplateProject templateProject
    private Project project
    private DependencyDownloadHelper helper

    private QslHelper qslHelper
    private FabricApiHelper fabricApiHelper

    ModGradleExtensionImpl(TemplateProject project, DependencyDownloadHelper helper) {
        this.templateProject = project
        this.project = project.getProject()
        this.helper = helper
    }

    @Override
    void fabricApi(String... modules) {
        if (!project.hasProperty(Constants.FABRIC_API_VERSION_KEY)) {
            throw new IllegalStateException("Must specify ${Constants.FABRIC_API_VERSION_KEY} in gradle.properties");
        }
        Platform platform = templateProject.platform
        if (!(platform == Platform.FABRIC || platform == Platform.QUILT)) {
            throw new IllegalStateException("Only applicable to fabric and quilt platforms")
        }
        String fabricApiVersion = project.property(Constants.FABRIC_API_VERSION_KEY)
        project.dependencies.tap {
            if (modules.length == 1 && modules[0] == "all") {
                if (platform == Platform.FABRIC) {
                    add("modImplementation", "net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")
                } else if (platform == Platform.QUILT) {
                    addProvider("modImplementation", project.provider(() -> "org.quiltmc.fabric_api_qsl:fabric-api:${fabricApiVersion}")) {
                        exclude(Map.of("group", "net.fabricmc"))
                    }
                }
            } else {
                if (platform == Platform.FABRIC) {
                    for (String module : modules) {
                        add("modImplementation", helper.fabricApi(module, fabricApiVersion));
                    }
                } else if (platform == Platform.QUILT) {
                    for (String module : modules) {
                        addProvider("modImplementation", project.provider(() -> helper.quiltedFabricApi(module, fabricApiVersion))) {
                            exclude(Map.of("group", "net.fabricmc"))
                        }
                    }
                }
            }
        }

    }

    @Override
    void qsl(String... modules) {
        if (!project.hasProperty(Constants.QSL_VERSION_KEY)) {
            throw new IllegalStateException("Must specify ${Constants.QSL_VERSION_KEY} in gradle.properties");
        }

        String qslVersion = project.property(Constants.QSL_VERSION_KEY)
        project.dependencies.tap {
            if (modules.length == 1 && modules[0] == "all") {
                addProvider("modImplementation", project.provider(() -> "org.quiltmc.qsl:qsl:${qslVersion}")) {
                    exclude(Map.of("group", "net.fabricmc"))
                }
            } else {
                for (String module : modules) {
                    addProvider("modImplementation", project.provider(() -> helper.qsl(module, qslVersion))) {
                        exclude(Map.of("group", "net.fabricmc"))
                    }
                }
            }
        }


    }

    @Override
    QslHelper qsl() {
        if (qslHelper == null) {
            String qslVersion = project.property("qsl_version");
            qslHelper = new QslHelper() {
                @Override
                String library(String libraryName) {
                    "org.quiltmc.qsl:${libraryName}:${qslVersion}"
                }

                @Override
                String module(String libraryName, String moduleName) {
                    "org.quiltmc.qsl.${libraryName}:${moduleName}:${qslVersion}"
                }
            }
        }
        return qslHelper
    }

    @Override
    FabricApiHelper fabricApi() {
        if (fabricApiHelper == null) {
            String fabricApiVersion = project.property("fabric_api_version")
            if (templateProject.platform == Platform.QUILT) {
                fabricApiHelper = new FabricApiHelper() {
                    @Override
                    String module(String moduleName) {
                        "org.quiltmc.quilted-fabric-api:${moduleName}:${fabricApiVersion}"
                    }

                    @Override
                    String full() {
                        "org.quiltmc.quilted-fabric-api:quilted-fabric-api:${fabricApiVersion}"
                    }

                    @Override
                    String deprecated() {
                        "org.quiltmc.quilted-fabric-api:quilted-fabric-api-deprecated:${fabricApiVersion}"
                    }
                }
            } else {
                fabricApiHelper = new FabricApiHelper() {
                    @Override
                    String module(String moduleName) {
                        helper.fabricApi(moduleName, fabricApiVersion)
                    }

                    @Override
                    String full() {
                        "net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}"
                    }

                    @Override
                    String deprecated() {
                        "net.fabricmc.fabric-api:fabric-api-deprecated:${fabricApiVersion}"
                    }
                }
            }
        }

        return fabricApiHelper;
    }
}
