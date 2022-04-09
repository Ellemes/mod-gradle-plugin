package ninjaphenix.gradle.mod.impl.dependency.local;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

import java.util.HashMap;

@Root
public class CachedVersionCoordinates {
    @Element
    private String etag;
    @ElementMap
    private HashMap<String, String> coordinates;

    public CachedVersionCoordinates(String etag) {
        this.etag = etag;
        this.coordinates = new HashMap<>();
    }

    public CachedVersionCoordinates() {

    }

    public String getEtag() {
        return etag;
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
