package ellemes.gradle.mod.impl.dependency.local;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;

@Root(name = "cached_coordinates")
public final class CachedVersionCoordinates {
    @Element(name = "entity_tag")
    private String entityTag;
    @Element(name = "last_checked_time")
    private long lastCheckedTime;
    @ElementMap(attribute = true, key="id")
    private LinkedHashMap<String, String> coordinates;

    public CachedVersionCoordinates(String entityTag) {
        this.entityTag = entityTag;
        this.lastCheckedTime = Date.from(Instant.now()).getTime();
        this.coordinates = new LinkedHashMap<>();
    }

    public CachedVersionCoordinates() {

    }

    public String getEntityTag() {
        return entityTag;
    }

    public long getLastCheckedTime() {
        return lastCheckedTime;
    }

    public String get(String thing) {
        return coordinates.get(thing);
    }

    public void put(String thing, String coordinate) {
        coordinates.put(thing, coordinate);
    }

    public int size() {
        return coordinates.size();
    }

}
