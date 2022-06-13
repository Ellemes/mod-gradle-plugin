package ellemes.gradle.mod.impl.dependency.maven

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(strict = false)
final class Dependency {
    @Element
    private String groupId
    @Element
    private String artifactId
    @Element
    private String version

    String getGroupId() {
        groupId
    }

    String getArtifactId() {
        artifactId
    }

    String getVersion() {
        version
    }

    String getGradleString() {
        "${groupId}:${artifactId}:${version}"
    }
}
