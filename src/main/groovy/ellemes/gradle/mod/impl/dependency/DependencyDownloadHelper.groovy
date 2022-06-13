//file:noinspection UnnecessaryQualifiedReference
package ellemes.gradle.mod.impl.dependency

import ellemes.gradle.mod.impl.dependency.local.CachedVersionCoordinates
import ellemes.gradle.mod.impl.dependency.local.LibraryXml
import ellemes.gradle.mod.impl.dependency.maven.Dependency
import ellemes.gradle.mod.impl.dependency.maven.MavenMetadata
import ellemes.gradle.mod.impl.dependency.maven.ProjectObjectModel
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.function.BiConsumer
import java.util.function.Consumer

final class DependencyDownloadHelper {
    private static final long CHECK_DELAY = 1000 * 60 * 60 * 24 // 1 day
    private static final String FABRIC_API_FILE = "fabric_api.xml"
    private static final String QSL_FILE = "qsl.xml"
    private static final String QUILTED_FABRIC_API_FILE = "quilted_fabric_api.xml"

    private final Serializer serializer
    private final URI fabricMaven
    private final URI quiltSnapshotMaven
    private final URI quiltReleaseMaven
    private final Map<String, CachedVersionCoordinates> fabricApiDependables = new HashMap<>()
    private final Map<String, CachedVersionCoordinates> qslDependables = new HashMap<>()
    private final Map<String, CachedVersionCoordinates> quiltedFabricApiDependables = new HashMap<>()
    private Path cacheDir

    DependencyDownloadHelper(Path cacheDir) {
        fabricMaven = new URI("https://maven.fabricmc.net/")
        quiltSnapshotMaven = new URI("https://maven.quiltmc.org/repository/snapshot/")
        quiltReleaseMaven = new URI("https://maven.quiltmc.org/repository/release/")
        serializer = new Persister()
        if (Files.notExists(cacheDir)) {
            try {
                Files.createDirectories(cacheDir)
            } catch (IOException e) {
                e.printStackTrace()
            }
        }
        if (Files.isDirectory(cacheDir)) {
            this.cacheDir = cacheDir
            this.loadCache()
        }
    }

    // <editor-fold desc="# Module getters">
    String fabricApi(String module, String version) {
        if (!fabricApiDependables.containsKey(version) || DependencyDownloadHelper.isCacheOutdated(fabricApiDependables.get(version), "fabric ${version}")) {
            this.populateFabricApiCache(version)
        }
        return fabricApiDependables.get(version).get(module)
    }

    private static boolean isCacheOutdated(CachedVersionCoordinates coordinates, String friendlyName) {
        boolean shouldUpdateCache = Boolean.getBoolean("MOD_IGNORE_CACHE_CHECK_DELAY") || Date.from(Instant.now()).time - coordinates.lastCheckedTime >= CHECK_DELAY

        if (shouldUpdateCache) {
            System.out.println("Checking cache is valid for ${friendlyName}")
        }

        return shouldUpdateCache
    }

    String qsl(String identifier, String version) {
        String key
        if (identifier.contains("/")) {
            String[] parts = identifier.split("/")
            if (parts.length != 2) {
                throw new IllegalArgumentException("qsl module must take form: library/module")
            }
            key = "${parts[0]}/${parts[1]}"
        } else {
            key = identifier
        }

        if (!qslDependables.containsKey(version) || DependencyDownloadHelper.isCacheOutdated(qslDependables.get(version), "qsl ${version}")) {
            this.populateQslCache(version)
        }
        return qslDependables.get(version).get(key)
    }

    String quiltedFabricApi(String module, String version) {
        if (!quiltedFabricApiDependables.containsKey(version) || DependencyDownloadHelper.isCacheOutdated(quiltedFabricApiDependables.get(version), "quilted ${version}")) {
            this.populateQuiltedFabricApiCache(version)
        }
        return quiltedFabricApiDependables.get(version).get(module)
    }
    // </editor-fold>

    private void loadCache() {
        this.loadCacheFile(fabricApiDependables, FABRIC_API_FILE)
        this.loadCacheFile(qslDependables, QSL_FILE)
        this.loadCacheFile(quiltedFabricApiDependables, QUILTED_FABRIC_API_FILE)
    }

    private void loadCacheFile(Map<String, CachedVersionCoordinates> dependables, String relativeFilePath) {
        Path filePath = cacheDir.resolve(relativeFilePath)
        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                LibraryXml data = serializer.read(LibraryXml.class, reader)
                dependables.putAll(data.entries)
            } catch (IOException e) {
                // IO error
                e.printStackTrace()
            } catch (Exception e) {
                // Cannot convert object to xml
                e.printStackTrace()
            }
        }
    }

    // <editor-fold desc="# Cache populators">
    private void populateFabricApiCache(String version) {
        boolean hasPreviousEntry = fabricApiDependables.containsKey(version)
        String fabricApiPom = "net/fabricmc/fabric-api/fabric-api/${version}/fabric-api-${version}.pom"
        DependencyDownloadHelper.withMavenOf(List.of(fabricMaven), fabricApiPom, "fabric api", (maven, eTag) -> {
            CachedVersionCoordinates coordinates = new CachedVersionCoordinates(eTag)
            this.iterateDependencies(maven.resolve(fabricApiPom)) {
                coordinates.put(it.artifactId, it.gradleString)
            }
            if (coordinates.size() > 0) {
                fabricApiDependables.put(version, coordinates)
                this.saveUpdatedCacheToFile(fabricApiDependables, FABRIC_API_FILE)
            } else if (hasPreviousEntry) {
                fabricApiDependables.remove(version)
                this.saveUpdatedCacheToFile(fabricApiDependables, FABRIC_API_FILE)
            }
        })
    }

    private void populateQslCache(String version) {
        boolean hasPreviousEntry = qslDependables.containsKey(version)
        String qslBaseUrl = "org/quiltmc/qsl/${version}"
        this.withTargetVersionOf(List.of(quiltSnapshotMaven), "${qslBaseUrl}/maven-metadata.xml", "qsl", version, (maven, eTag, targetQslVersion) -> {
            CachedVersionCoordinates dependencies = new CachedVersionCoordinates(eTag)
            this.iterateDependencies(maven.resolve("${qslBaseUrl}/qsl-${targetQslVersion.value}.pom")){ qslLib ->
                dependencies.put(qslLib.artifactId, qslLib.gradleString)
                String libraryBaseUrl = "${qslLib.groupId.replace(".", "/")}/${qslLib.artifactId}/${qslLib.version}"
                this.withTargetVersionOf(List.of(maven), "${libraryBaseUrl}/maven-metadata.xml", "qsl-${qslLib.artifactId}", qslLib.version, (_unused, moduleEtag, targetModuleVersion) -> {
                    this.iterateDependencies(maven.resolve("${libraryBaseUrl}/${qslLib.artifactId}-${targetModuleVersion.value}.pom"), qslMod -> {
                        dependencies.put("${qslLib.artifactId}/${qslMod.artifactId}", qslMod.gradleString)
                    })
                })
            }
            if (dependencies.size() > 0) {
                qslDependables.put(version, dependencies)
                this.saveUpdatedCacheToFile(qslDependables, QSL_FILE)
            } else if (hasPreviousEntry) {
                qslDependables.remove(version)
                this.saveUpdatedCacheToFile(qslDependables, QSL_FILE)
            }
        })
    }

    private void populateQuiltedFabricApiCache(String version) {
        boolean hasPreviousEntry = quiltedFabricApiDependables.containsKey(version)
        String baseUrl = "org/quiltmc/quilted-fabric-api/quilted-fabric-api/${version}"
        this.withTargetVersionOf(List.of(quiltSnapshotMaven), "${baseUrl}/maven-metadata.xml", "quilted fabric api", version, (maven, eTag, targetVersion) -> {
            CachedVersionCoordinates coordinates = new CachedVersionCoordinates(eTag)
            this.iterateDependencies(maven.resolve("${baseUrl}/quilted-fabric-api-${targetVersion.value}.pom")) {
                coordinates.put(it.artifactId, it.gradleString)
            }
            if (coordinates.size() > 0) {
                quiltedFabricApiDependables.put(version, coordinates)
                this.saveUpdatedCacheToFile(quiltedFabricApiDependables, QUILTED_FABRIC_API_FILE)
            }else if (hasPreviousEntry) {
                quiltedFabricApiDependables.remove(version)
                this.saveUpdatedCacheToFile(quiltedFabricApiDependables, QUILTED_FABRIC_API_FILE)
            }
        })
    }
    // </editor-fold>

    private static String getEtag(URI maven, String file) {
        try {
            HttpURLConnection connection = (HttpURLConnection) maven.resolve(file).toURL().openConnection()
            connection.connectTimeout = 60000
            connection.readTimeout = 60000
            connection.requestMethod = "HEAD"

            if (connection.responseCode == 200) {
                return connection.getHeaderField("ETag")
            }
        } catch (IOException ignored) {

        }
        return null
    }

    private void iterateDependencies(URI uri, Consumer<Dependency> dependencyConsumer) {
        try {
            ProjectObjectModel pom = serializer.read(ProjectObjectModel.class, uri.toURL().openStream())
            pom.dependencies.forEach {
                dependencyConsumer.accept(it)
            }
        } catch (IOException e) {
            //  Networking error
        } catch (Exception e) {
            // Invalid file
        }
    }

    private void saveUpdatedCacheToFile(Map<String, CachedVersionCoordinates> cachedCoordinates, String file) {
        if (cacheDir == null) {
            return
        }
        try (BufferedWriter writer = Files.newBufferedWriter(cacheDir.resolve(file), StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            serializer.write(new LibraryXml(cachedCoordinates), writer)
        } catch (IOException e) {
            // IO error
            e.printStackTrace()
        } catch (Exception e) {
            // Cannot convert object to xml
            e.printStackTrace()
        }
    }

    private static void withMavenOf(List<URI> mavens, String type, String friendlyName, BiConsumer<URI, String> callback) {
        mavens.forEach {
            String eTag = DependencyDownloadHelper.getEtag(it, type)
            if (eTag != null) {
                callback.accept(it, eTag)
                return
            }
        }
        throw new RuntimeException("Unable to find specified " + friendlyName + " version")
    }

    private void withTargetVersionOf(List<URI> mavens, String metadata, String friendlyName, String version, FileVersionCallback callback) {
        DependencyDownloadHelper.withMavenOf(mavens, metadata, friendlyName, (maven, eTag) -> {
            try {
                MavenMetadata value = serializer.read(MavenMetadata.class, maven.resolve(metadata).toURL().openStream())
                String lastUpdated = value.lastUpdated
                value.snapshotVersions.forEach {
                    if (it.updated == lastUpdated && it.extension == "pom") {
                        callback.accept(maven, eTag, it)
                        return
                    }
                }
            } catch (IOException e) {
                //  Networking error
            } catch (Exception e) {
                // Invalid file
            }
            throw new RuntimeException("Encountered invalid maven metadata for ${friendlyName} ${version}")
        })
    }
}
