package ellemes.gradle.mod.api.publishing;

import com.modrinth.minotaur.ModrinthExtension;
import ellemes.gradle.mod.impl.misc.ActionClosure;
import ellemes.gradle.mod.impl.Constants;
import me.hypherionmc.cursegradle.CurseArtifact;
import me.hypherionmc.cursegradle.CurseExtension;
import me.hypherionmc.cursegradle.CurseProject;
import me.hypherionmc.cursegradle.Options;
import org.codehaus.groovy.runtime.ProcessGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UploadProperties {
    private final Project project;
    private final String version;
    private final String releaseType;
    private final HashSet<String> targetVersions;
    private final boolean debug;
    private final String changelog;

    public UploadProperties(Project project, String repoBaseUrl) {
        this.project = project;
        this.version = (String) project.property(Constants.MOD_VERSION_KEY);
        this.releaseType = version.contains("alpha") ? "alpha" : version.contains("beta") ? "beta" : "release";
        this.targetVersions = new HashSet<>();
        targetVersions.add((String) project.property(Constants.MINECRAFT_VERSION_KEY));
        for (String gameVersion : ((String) project.property("extra_game_versions")).split(",")) {
            if (!"".equals(gameVersion)) {
                targetVersions.add(gameVersion);
            }
        }

        this.debug = "true".equals(System.getProperty("MOD_UPLOAD_DEBUG", "false")); // -DMOD_UPLOAD_DEBUG=true
        String _changelog = getFileContents(project.getRootDir().toPath().resolve("changelog.md"));

        String commit = UploadProperties.getGitCommit();

        if (commit != null) {
            _changelog += "\nCommit: " + repoBaseUrl + "/commit/" + commit;
        }
        this.changelog = _changelog;
    }

    private static String getGitCommit() {
        try {
            return ProcessGroovyMethods.getText(ProcessGroovyMethods.execute("git rev-parse HEAD"));
        } catch (IOException e) {
            return null;
        }
    }

    private static String getFileContents(Path file) {
        try {
            return Files.readString(file).replace("\r\n", "\n");
        } catch (Exception e) {
            return null;
        }
    }

    private static String titleCase(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public void configureCurseForge(Action<CurseProject> action) {
        CurseExtension extension = project.getExtensions().getByType(CurseExtension.class);

        Options options = extension.getCurseGradleOptions();
        options.setDebug(debug);
        options.setJavaVersionAutoDetect(false);
        options.setJavaIntegration(false);
        options.setForgeGradleIntegration(false);
        options.setFabricIntegration(false);
        options.setDetectFabricApi(false);

        extension.project(new ActionClosure<CurseProject>(this, p -> {
            p.setApiKey(System.getenv("CURSEFORGE_TOKEN"));
            p.setId(project.property("curseforge_project_id"));
            p.setReleaseType(releaseType);
            Task minJar = project.getTasks().getByName("minJar");
            p.mainArtifact(minJar, new ActionClosure<CurseArtifact>(this, a -> {
                a.setDisplayName(UploadProperties.titleCase(project.getName()) + " " + version);
                a.setArtifact(minJar);
            }));
            p.setChangelogType("markdown");
            p.setChangelog(changelog);
            List<Object> gameVersions = new ArrayList<>();
            gameVersions.add(UploadProperties.titleCase(project.getName()));
            gameVersions.add("Java " + project.getExtensions().getByType(JavaPluginExtension.class).getTargetCompatibility().getMajorVersion());
            gameVersions.addAll(targetVersions);
            p.setGameVersionStrings(gameVersions);
            action.execute(p);

            project.afterEvaluate(e -> e.getTasks().getByName(Constants.MOD_UPLOAD_TASK).finalizedBy("curseforge" + p.getId()));
        }));
    }

    public void configureModrinth(Action<ModrinthExtension> action) {
        ModrinthExtension extension = project.getExtensions().getByType(ModrinthExtension.class);

        extension.getDebugMode().set(debug);
        extension.getDetectLoaders().set(false);
        extension.getProjectId().set((String) project.property("modrinth_project_id"));
        extension.getVersionType().set(releaseType);
        extension.getVersionNumber().set(version + "+" + project.getName());
        extension.getVersionName().set(UploadProperties.titleCase(project.getName()) + " " + version);
        extension.getUploadFile().set(project.getTasks().getByName("minJar"));
        extension.getChangelog().set(changelog);
        extension.getGameVersions().set(targetVersions);
        extension.getLoaders().set(List.of(project.getName()));

        action.execute(extension);

        project.afterEvaluate(e -> e.getTasks().getByName(Constants.MOD_UPLOAD_TASK).finalizedBy("modrinth"));
    }
}
