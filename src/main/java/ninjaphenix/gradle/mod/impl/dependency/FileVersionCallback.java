package ninjaphenix.gradle.mod.impl.dependency;

import ninjaphenix.gradle.mod.impl.dependency.maven.SnapshotVersion;

import java.net.URI;

public interface FileVersionCallback {
    void accept(URI maven, String eTag, SnapshotVersion version);
}
