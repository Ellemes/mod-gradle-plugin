package ellemes.gradle.mod.impl.dependency.maven

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "metadata", strict = false)
final class MavenMetadata {
    @Element
    private Versioning versioning

    List<SnapshotVersion> getSnapshotVersions() {
        versioning.snapshotVersions
    }

    String getLastUpdated() {
        versioning.lastUpdated
    }
}
