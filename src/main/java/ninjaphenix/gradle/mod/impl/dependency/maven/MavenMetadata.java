package ninjaphenix.gradle.mod.impl.dependency.maven;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "metadata", strict = false)
public class MavenMetadata {
    @Element
    private Versioning versioning;

    public List<SnapshotVersion> getSnapshotVersions() {
        return versioning.getSnapshotVersions();
    }

    public String getLastUpdated() {
        return versioning.getLastUpdated();
    }
}
