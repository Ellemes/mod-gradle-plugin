package ninjaphenix.gradle.mod.impl.xml;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(strict = false)
public class SnapshotVersion {
    @Element
    private String value;
    @Element
    private String updated;

    public String getValue() {
        return value;
    }

    public String getUpdated() {
        return updated;
    }
}
