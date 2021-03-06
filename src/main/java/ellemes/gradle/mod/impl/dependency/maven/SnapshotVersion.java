package ellemes.gradle.mod.impl.dependency.maven;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(strict = false)
public final class SnapshotVersion {
    @Element
    private String value;
    @Element
    private String updated;
    @Element
    private String extension;

    public String getValue() {
        return value;
    }

    public String getUpdated() {
        return updated;
    }

    public String getExtension() {
        return extension;
    }
}
