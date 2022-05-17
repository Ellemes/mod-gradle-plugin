package ellemes.gradle.mod.api.ext;

public interface ModGradleExtension {
    void fabricApi(String... modules);

    void qsl(String... modules);
}
