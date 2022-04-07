package ninjaphenix.gradle.mod.impl.xml;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "project", strict = false)
public class POM {
    @ElementList
    private List<Dependency> dependencies;

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
