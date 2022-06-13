package ellemes.gradle.mod.impl.dependency.maven

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(strict = false)
final class SnapshotVersion {
    @Element
    private String value
    @Element
    private String updated
    @Element
    private String extension

    String getValue() {
        return value
    }

    String getUpdated() {
        return updated
    }

    String getExtension() {
        return extension
    }
}
