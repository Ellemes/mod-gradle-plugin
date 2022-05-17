package ellemes.gradle.mod.impl.dependency.local;

import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

import java.util.Map;

@Root(name = "versions")
public final class LibraryXml {
    @ElementMap(attribute = true, key = "version", inline = true)
    private Map<String, CachedVersionCoordinates> entries;

    public LibraryXml() {

    }

    public LibraryXml(Map<String, CachedVersionCoordinates> entries) {
        this.entries = entries;
    }

    public Map<String, CachedVersionCoordinates> getEntries() {
        return entries;
    }
}
