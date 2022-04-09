package ninjaphenix.gradle.mod.impl.dependency.local;

import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

import java.util.Map;

@Root
public class LibraryXml {
    @ElementMap
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
