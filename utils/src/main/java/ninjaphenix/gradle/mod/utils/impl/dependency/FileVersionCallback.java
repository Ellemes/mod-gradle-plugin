package ninjaphenix.gradle.mod.utils.impl.dependency;

import ninjaphenix.gradle.mod.utils.impl.dependency.maven.SnapshotVersion;

import java.net.URI;

public interface FileVersionCallback {
    void accept(URI maven, String eTag, SnapshotVersion version);
}
