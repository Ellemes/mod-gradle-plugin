package ellemes.gradle.mod.impl.dependency.maven

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "versioning", strict = false)
final class Versioning {
    @ElementList
    private List<SnapshotVersion> snapshotVersions
    @Element
    private String lastUpdated

    List<SnapshotVersion> getSnapshotVersions() {
        snapshotVersions
    }

    String getLastUpdated() {
        lastUpdated
    }
}
