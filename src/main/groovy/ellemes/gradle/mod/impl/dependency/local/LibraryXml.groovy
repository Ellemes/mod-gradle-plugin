package ellemes.gradle.mod.impl.dependency.local

import org.simpleframework.xml.ElementMap
import org.simpleframework.xml.Root

@Root(name = "versions")
final class LibraryXml {
    @ElementMap(attribute = true, key = "version", inline = true)
    private Map<String, CachedVersionCoordinates> entries

    LibraryXml() {

    }

    LibraryXml(Map<String, CachedVersionCoordinates> entries) {
        this.entries = entries
    }

    Map<String, CachedVersionCoordinates> getEntries() {
        entries
    }
}
