package ellemes.gradle.mod.impl.dependency;

import ellemes.gradle.mod.impl.dependency.local.CachedVersionCoordinates;
import ellemes.gradle.mod.impl.dependency.local.LibraryXml;
import ellemes.gradle.mod.impl.dependency.maven.POM;
import ellemes.gradle.mod.impl.dependency.maven.SnapshotVersion;
import ellemes.gradle.mod.impl.dependency.maven.Dependency;
import ellemes.gradle.mod.impl.dependency.maven.MavenMetadata;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// todo: handle errors...
// todo: allow usage of quilt release maven
public final class DependencyDownloadHelper {
    private static final long CHECK_DELAY = 1000 * 60 * 60 * 24; // 1 day
    private static final String FABRIC_API_FILE = "fabric_api.xml";
    private static final String QSL_FILE = "qsl.xml";
    private static final String QUILTED_FABRIC_API_FILE = "quilted_fabric_api.xml";

    private final Serializer serializer;
    private final URI fabricMaven;
    private final URI quiltSnapshotMaven;
    private final URI quiltReleaseMaven;
    private final Map<String, CachedVersionCoordinates> fabricApiDependables = new HashMap<>();
    private final Map<String, CachedVersionCoordinates> qslDependables = new HashMap<>();
    private final Map<String, CachedVersionCoordinates> quiltedFabricApiDependables = new HashMap<>();
    private Path cacheDir;

    public DependencyDownloadHelper(Path cacheDir) {
        try {
            fabricMaven = new URI("https://maven.fabricmc.net/");
            quiltSnapshotMaven = new URI("https://maven.quiltmc.org/repository/snapshot/");
            quiltReleaseMaven = new URI("https://maven.quiltmc.org/repository/release/");
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Author made a typo... ", e);
        }
        serializer = new Persister();
        if (Files.notExists(cacheDir)) {
            try {
                Files.createDirectories(cacheDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (Files.isDirectory(cacheDir)) {
            this.cacheDir = cacheDir;
            this.loadCache();
        }
    }

    // <editor-fold desc="# Module getters">
    public String fabricApi(String module, String version) {
        if (!fabricApiDependables.containsKey(version) || this.isCacheOutdated(fabricApiDependables.get(version), "fabric " + version)) {
            this.populateFabricApiCache(version);
        }
        return fabricApiDependables.get(version).get(module);
    }

    private boolean isCacheOutdated(CachedVersionCoordinates coordinates, String friendlyName) {
        boolean rv = Boolean.getBoolean("mod.ignoreCacheCheckDelay") || Date.from(Instant.now()).getTime() - coordinates.getLastCheckedTime() >= CHECK_DELAY;

        if (rv) {
            System.out.println("Checking cache is valid for " + friendlyName);
        }

        return rv;
    }

    public String qsl(String identifier, String version) {
        String key;
        if (identifier.contains("/")) {
            String[] parts = identifier.split("/");
            if (parts.length != 2) {
                throw new IllegalArgumentException("qsl module must take form: library/module");
            }
            key = parts[0] + "/" + parts[1];
        } else {
            key = identifier;
        }

        if (!qslDependables.containsKey(version) || this.isCacheOutdated(qslDependables.get(version), "qsl " + version)) {
            this.populateQslCache(version);
        }
        return qslDependables.get(version).get(key);
    }

    public String quiltedFabricApi(String module, String version) {
        if (!quiltedFabricApiDependables.containsKey(version) || this.isCacheOutdated(quiltedFabricApiDependables.get(version), "quilted " + version)) {
            this.populateQuiltedFabricApiCache(version);
        }
        return quiltedFabricApiDependables.get(version).get(module);
    }
    // </editor-fold>

    private void loadCache() {
        this.loadCacheFile(fabricApiDependables, FABRIC_API_FILE);
        this.loadCacheFile(qslDependables, QSL_FILE);
        this.loadCacheFile(quiltedFabricApiDependables, QUILTED_FABRIC_API_FILE);
    }

    private void loadCacheFile(Map<String, CachedVersionCoordinates> dependables, String relativeFilePath) {
        Path filePath = cacheDir.resolve(relativeFilePath);
        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                LibraryXml data = serializer.read(LibraryXml.class, reader);
                dependables.putAll(data.getEntries());
            } catch (IOException e) {
                // IO error
                e.printStackTrace();
            } catch (Exception e) {
                // Cannot convert object to xml
                e.printStackTrace();
            }
        }
    }

    // <editor-fold desc="# Cache populators">
    private void populateFabricApiCache(String version) {
        boolean hasPreviousEntry = fabricApiDependables.containsKey(version);
        String fabricApiPom = "net/fabricmc/fabric-api/fabric-api/" + version + "/fabric-api-" + version + ".pom";
        this.withMavenOf(List.of(fabricMaven), fabricApiPom, "fabric api", (maven, eTag) -> {
            CachedVersionCoordinates coordinates = new CachedVersionCoordinates(eTag);
            this.iterateDependencies(maven.resolve(fabricApiPom), dependency -> {
                coordinates.put(dependency.getArtifactId(), dependency.getGradleString());
            });
            if (coordinates.size() > 0) {
                fabricApiDependables.put(version, coordinates);
                this.saveUpdatedCacheToFile(fabricApiDependables, FABRIC_API_FILE);
            } else if (hasPreviousEntry) {
                fabricApiDependables.remove(version);
                this.saveUpdatedCacheToFile(fabricApiDependables, FABRIC_API_FILE);
            }
        });
    }

    private void populateQslCache(String version) {
        boolean hasPreviousEntry = qslDependables.containsKey(version);
        String qslBaseUrl = "org/quiltmc/qsl/" + version + "/";
        this.withTargetVersionOf(List.of(quiltSnapshotMaven), qslBaseUrl + "maven-metadata.xml", "qsl", version, (maven, eTag, targetQslVersion) -> {
            CachedVersionCoordinates dependencies = new CachedVersionCoordinates(eTag);
            this.iterateDependencies(maven.resolve(qslBaseUrl + "qsl-" + targetQslVersion.getValue() + ".pom"), qslLib -> {
                dependencies.put(qslLib.getArtifactId(), qslLib.getGradleString());
                String libraryBaseUrl = qslLib.getGroupId().replace(".", "/") + "/" + qslLib.getArtifactId() + "/" + qslLib.getVersion() + "/";
                this.withTargetVersionOf(List.of(maven), libraryBaseUrl + "maven-metadata.xml", "qsl-" + qslLib.getArtifactId(), qslLib.getVersion(), (_unused, moduleEtag, targetModuleVersion) -> {
                    this.iterateDependencies(maven.resolve(libraryBaseUrl + qslLib.getArtifactId() + "-" + targetModuleVersion.getValue() + ".pom"), qslMod -> {
                        dependencies.put(qslLib.getArtifactId() + "/" + qslMod.getArtifactId(), qslMod.getGradleString());
                    });
                });
            });
            if (dependencies.size() > 0) {
                qslDependables.put(version, dependencies);
                this.saveUpdatedCacheToFile(qslDependables, QSL_FILE);
            } else if (hasPreviousEntry) {
                qslDependables.remove(version);
                this.saveUpdatedCacheToFile(qslDependables, QSL_FILE);
            }
        });
    }

    private void populateQuiltedFabricApiCache(String version) {
        boolean hasPreviousEntry = quiltedFabricApiDependables.containsKey(version);
        String baseUrl = "org/quiltmc/quilted-fabric-api/quilted-fabric-api/" + version + "/";
        this.withTargetVersionOf(List.of(quiltSnapshotMaven), baseUrl + "maven-metadata.xml", "quilted fabric api", version, (maven, eTag, targetVersion) -> {
            CachedVersionCoordinates coordinates = new CachedVersionCoordinates(eTag);
            this.iterateDependencies(maven.resolve(baseUrl + "quilted-fabric-api-" + targetVersion.getValue() + ".pom"), dependency -> {
                coordinates.put(dependency.getArtifactId(), dependency.getGradleString());
            });
            if (coordinates.size() > 0) {
                quiltedFabricApiDependables.put(version, coordinates);
                this.saveUpdatedCacheToFile(quiltedFabricApiDependables, QUILTED_FABRIC_API_FILE);
            }else if (hasPreviousEntry) {
                quiltedFabricApiDependables.remove(version);
                this.saveUpdatedCacheToFile(quiltedFabricApiDependables, QUILTED_FABRIC_API_FILE);
            }
        });
    }
    // </editor-fold>

    private String getEtag(URI maven, String file) {
        try {
            HttpURLConnection connection = (HttpURLConnection) maven.resolve(file).toURL().openConnection();
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            connection.setRequestMethod("HEAD");

            if (connection.getResponseCode() == 200) {
                return connection.getHeaderField("ETag");
            }
        } catch (IOException ignored) {

        }
        return null;
    }

    private void iterateDependencies(URI pom, Consumer<Dependency> dependencyConsumer) {
        try {
            POM value = serializer.read(POM.class, pom.toURL().openStream());
            for (Dependency dependency : value.getDependencies()) {
                dependencyConsumer.accept(dependency);
            }
        } catch (IOException e) {
            //  Networking error
        } catch (Exception e) {
            // Invalid file
        }
    }

    private void saveUpdatedCacheToFile(Map<String, CachedVersionCoordinates> cachedCoordinates, String file) {
        if (cacheDir == null) {
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(cacheDir.resolve(file), StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            serializer.write(new LibraryXml(cachedCoordinates), writer);
        } catch (IOException e) {
            // IO error
            e.printStackTrace();
        } catch (Exception e) {
            // Cannot convert object to xml
            e.printStackTrace();
        }
    }

    private void withMavenOf(List<URI> mavens, String type, String friendlyName, BiConsumer<URI, String> callback) {
        for (URI maven : mavens) {
            String eTag;
            if ((eTag = this.getEtag(maven, type)) != null) {
                callback.accept(maven, eTag);
                return;
            }
        }
        throw new RuntimeException("Unable to find specified " + friendlyName + " version");
    }

    private void withTargetVersionOf(List<URI> mavens, String metadata, String friendlyName, String version, FileVersionCallback callback) {
        this.withMavenOf(mavens, metadata, friendlyName, (maven, eTag) -> {
            try {
                MavenMetadata value = serializer.read(MavenMetadata.class, maven.resolve(metadata).toURL().openStream());
                String lastUpdated = value.getLastUpdated();
                for (SnapshotVersion snapshotVersion : value.getSnapshotVersions()) {
                    if (snapshotVersion.getUpdated().equals(lastUpdated) && snapshotVersion.getExtension().equals("pom")) {
                        callback.accept(maven, eTag, snapshotVersion);
                        return;
                    }
                }
            } catch (IOException e) {
                //  Networking error
            } catch (Exception e) {
                // Invalid file
            }
            throw new RuntimeException("Encountered invalid maven metadata for " + friendlyName + " " + version);
        });
    }
}
