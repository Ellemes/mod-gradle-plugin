package ellemes.gradle.mod.impl.dependency.local

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementMap
import org.simpleframework.xml.Root

import java.time.Instant

@Root(name = "cached_coordinates")
final class CachedVersionCoordinates {
    @Element(name = "entity_tag")
    private String entityTag
    @Element(name = "last_checked_time")
    private long lastCheckedTime
    @ElementMap(attribute = true, key="id")
    private LinkedHashMap<String, String> coordinates

    CachedVersionCoordinates(String entityTag) {
        this.entityTag = entityTag
        lastCheckedTime = Date.from(Instant.now()).time
        coordinates = new LinkedHashMap<>()
    }

    CachedVersionCoordinates() {

    }

    String getEntityTag() {
        entityTag
    }

    long getLastCheckedTime() {
        lastCheckedTime
    }

    String get(String thing) {
        coordinates.get(thing)
    }

    void put(String thing, String coordinate) {
        coordinates.put(thing, coordinate)
    }

    int size() {
        coordinates.size()
    }

}
