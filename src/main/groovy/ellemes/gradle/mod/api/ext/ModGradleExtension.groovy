package ellemes.gradle.mod.api.ext

import ellemes.gradle.mod.api.helpers.FabricApiHelper
import ellemes.gradle.mod.api.helpers.QslHelper

interface ModGradleExtension {
    QslHelper qsl()
    FabricApiHelper fabricApi()
}
