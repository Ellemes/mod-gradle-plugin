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
        this.project = project.project
        this.helper = helper
    }

    @Override
    QslHelper qsl() {
        if (qslHelper == null) {
            String qslVersion = project.property(Constants.Keys.QSL_VERSION)
            qslHelper = new QslHelper() {
                @Override
                String full() {
                    "org.quiltmc:qsl:${qslVersion}"
                }

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
            String fabricApiVersion = project.property(Constants.Keys.FABRIC_API_VERSION)
            if (templateProject.platform == Platform.QUILT) {
                fabricApiHelper = new FabricApiHelper() {
                    @Override
                    String full() {
                        "org.quiltmc.quilted-fabric-api:quilted-fabric-api:${fabricApiVersion}"
                    }

                    @Override
                    String module(String moduleName) {
                        "org.quiltmc.quilted-fabric-api:${moduleName}:${fabricApiVersion}"
                    }

                    @Override
                    String deprecated() {
                        "org.quiltmc.quilted-fabric-api:quilted-fabric-api-deprecated:${fabricApiVersion}"
                    }
                }
            } else {
                fabricApiHelper = new FabricApiHelper() {
                    @Override
                    String full() {
                        "net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}"
                    }

                    @Override
                    String module(String moduleName) {
                        helper.fabricApi(moduleName, fabricApiVersion)
                    }

                    @Override
                    String deprecated() {
                        "net.fabricmc.fabric-api:fabric-api-deprecated:${fabricApiVersion}"
                    }
                }
            }
        }
        return fabricApiHelper
    }
}
