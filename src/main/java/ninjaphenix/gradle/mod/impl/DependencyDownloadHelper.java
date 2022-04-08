package ninjaphenix.gradle.mod.impl;

import ninjaphenix.gradle.mod.impl.xml.Dependency;
import ninjaphenix.gradle.mod.impl.xml.MavenMetadata;
import ninjaphenix.gradle.mod.impl.xml.POM;
import ninjaphenix.gradle.mod.impl.xml.SnapshotVersion;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

// todo: implement a cache, handle errors...
public final class DependencyDownloadHelper {
    private final URI fabricMaven;
    private final URI quiltSnapshotMaven;
    private final URI quiltReleaseMaven;
    private final Map<String, Object> fabricApiModules = new HashMap<>();
    private final Map<String, Object> qslModules = new HashMap<>();
    private final Map<String, Object> quiltedFabricApiModules = new HashMap<>();

    public DependencyDownloadHelper() throws URISyntaxException {
        fabricMaven = new URI("https://maven.fabricmc.net/");
        quiltSnapshotMaven = new URI("https://maven.quiltmc.org/repository/snapshot/");
        quiltReleaseMaven = new URI("https://maven.quiltmc.org/repository/release/");
    }

    public Object qslModule(String library, String module, String version) {
        String key = module.length() == 0 ? library + "_" + version : library + "_" + module + "_" + version;
        if (qslModules.containsKey(key)) {
            return qslModules.get(key);
        }
        String qslMetadata = "org/quiltmc/qsl/qsl/" + version + "/maven-metadata.xml";
        URI maven;
        if (this.mavenFileExists(quiltSnapshotMaven, qslMetadata)) {
            maven = quiltSnapshotMaven;
        } else if (this.mavenFileExists(quiltReleaseMaven, qslMetadata)) {
            maven = quiltReleaseMaven;
        } else {
            throw new RuntimeException("Unable to find specified qsl version");
        }
        Serializer serializer = new Persister();
        SnapshotVersion targetQslVersion = null;
        try {
            MavenMetadata value = serializer.read(MavenMetadata.class, maven.resolve(qslMetadata).toURL().openStream());
            String lastUpdated = value.getLastUpdated();
            for (SnapshotVersion snapshotVersion : value.getSnapshotVersions()) {
                if (snapshotVersion.getUpdated().equals(lastUpdated)) {
                    targetQslVersion = snapshotVersion;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        if (targetQslVersion == null) {
            throw new RuntimeException("Encountered invalid maven metadata for qsl " + version);
        }
        String qslPomPath = "org/quiltmc/qsl/qsl/" + version + "/qsl-" + targetQslVersion.getValue() + ".pom";
        try {
            POM qslPom = serializer.read(POM.class, maven.resolve(qslPomPath).toURL().openStream());
            for (Dependency qslDependency : qslPom.getDependencies()) {
                qslModules.put(qslDependency.getArtifactId() + "_" + version, qslDependency.getGradleString());
                // Search for sub modules here...
                String moduleMavenMetadata = qslDependency.getGroupId().replace(".", "/") + "/" + qslDependency.getArtifactId() + "/" + qslDependency.getVersion() + "/maven-metadata.xml";
                MavenMetadata moduleMetadata = serializer.read(MavenMetadata.class, maven.resolve(moduleMavenMetadata).toURL().openStream());
                SnapshotVersion targetModuleVersion = null;
                String lastUpdated = moduleMetadata.getLastUpdated();
                for (SnapshotVersion snapshotVersion : moduleMetadata.getSnapshotVersions()) {
                    if (snapshotVersion.getUpdated().equals(lastUpdated)) {
                        targetModuleVersion = snapshotVersion;
                        break;
                    }
                }
                if (targetModuleVersion == null) {
                    throw new RuntimeException("Encountered invalid maven metadata for qsl-" + qslDependency.getArtifactId() + " " + version);
                }
                String qslModulePomPath = qslDependency.getGroupId().replace(".", "/") + "/" + qslDependency.getArtifactId() + "/" + qslDependency.getVersion() +  "/" + qslDependency.getArtifactId() + "-" + targetModuleVersion.getValue() + ".pom";
                POM qslModulePom = serializer.read(POM.class, maven.resolve(qslModulePomPath).toURL().openStream());
                for (Dependency moduleDependency : qslModulePom.getDependencies()) {
                    qslModules.put(qslDependency.getArtifactId() + "_" + moduleDependency.getArtifactId() + "_" + version, moduleDependency.getGradleString());
                }
            }
        } catch (Exception ignored) {
        }

        return qslModules.get(key);
    }

    public Object qslModule(String combined, String version) {
        String library = combined;
        String module = "";
        if (combined.contains("/")) {
            var parts = combined.split("/");
            if (parts.length != 2) {
                throw new IllegalArgumentException("qsl sub-module must take form: library/module");
            }
            library = parts[0];
            module = parts[1];
        }
        return qslModule(library, module, version);
    }

    private boolean mavenFileExists(URI maven, String file) {
        try {
            HttpURLConnection connection = (HttpURLConnection) maven.resolve(file).toURL().openConnection();
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            connection.setRequestMethod("HEAD");

            if (connection.getResponseCode() == 200) {
                return true;
            }
        } catch (IOException ignored) {

        }
        return false;
    }

    public Object quiltedFabricApiModule(String module, String version) {
        if (quiltedFabricApiModules.containsKey(module + "_" + version)) {
            return quiltedFabricApiModules.get(module + "_" + version);
        }
        String fabricApiMetadata = "org/quiltmc/fabric_api_qsl/fabric-api/" + version + "/maven-metadata.xml";
        URI maven;
        if (this.mavenFileExists(quiltSnapshotMaven, fabricApiMetadata)) {
            maven = quiltSnapshotMaven;
        } else if (this.mavenFileExists(quiltReleaseMaven, fabricApiMetadata)) {
            maven = quiltReleaseMaven;
        } else {
            throw new RuntimeException("Unable to find specified quilted fabric api version");
        }
        Serializer serializer = new Persister();
        SnapshotVersion targetVersion = null;
        try {
            MavenMetadata value = serializer.read(MavenMetadata.class, maven.resolve(fabricApiMetadata).toURL().openStream());
            String lastUpdated = value.getLastUpdated();
            for (SnapshotVersion snapshotVersion : value.getSnapshotVersions()) {
                if (snapshotVersion.getUpdated().equals(lastUpdated)) {
                    targetVersion = snapshotVersion;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        if (targetVersion == null) {
            throw new RuntimeException("Encountered invalid maven metadata for quilted fabric api " + version);
        }
        String fabricApiPom = "org/quiltmc/fabric_api_qsl/fabric-api/" + version + "/fabric-api-" + targetVersion.getValue() + ".pom";
        try {
            POM value = serializer.read(POM.class, maven.resolve(fabricApiPom).toURL().openStream());
            for (Dependency dependency : value.getDependencies()) {
                quiltedFabricApiModules.put(dependency.getArtifactId() + "_" + version, dependency.getGradleString());
            }
        } catch (Exception ignored) {
        }
        return quiltedFabricApiModules.get(module + "_" + version);
    }

    public Object fabricApiModule(String module, String version) {
        if (fabricApiModules.containsKey(module + "_" + version)) {
            return fabricApiModules.get(module + "_" + version);
        }
        String fabricApiPom = "net/fabricmc/fabric-api/fabric-api/" + version + "/fabric-api-" + version + ".pom";
        URI maven;
        if (this.mavenFileExists(fabricMaven, fabricApiPom)) {
            maven = fabricMaven;
        } else {
            throw new RuntimeException("Unable to find specified fabric api version");
        }
        Serializer serializer = new Persister();
        try {
            POM value = serializer.read(POM.class, maven.resolve(fabricApiPom).toURL().openStream());
            for (Dependency dependency : value.getDependencies()) {
                fabricApiModules.put(dependency.getArtifactId() + "_" + version, dependency.getGradleString());
            }
        } catch (Exception ignored) {
        }
        return fabricApiModules.get(module + "_" + version);
    }
}
