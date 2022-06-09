package ellemes.gradle.mod.api.ext;

import ellemes.gradle.mod.api.helpers.FabricApiHelper;
import ellemes.gradle.mod.api.helpers.QslHelper;

public interface ModGradleExtension {
    @Deprecated
    void fabricApi(String... modules);

    @Deprecated
    void qsl(String... modules);

    QslHelper qsl();

    FabricApiHelper fabricApi();
}
