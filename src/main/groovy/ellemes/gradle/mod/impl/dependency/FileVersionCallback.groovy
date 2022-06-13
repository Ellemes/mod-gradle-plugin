package ellemes.gradle.mod.impl.dependency

import ellemes.gradle.mod.impl.dependency.maven.SnapshotVersion

interface FileVersionCallback {
    void accept(URI maven, String eTag, SnapshotVersion version);
}
