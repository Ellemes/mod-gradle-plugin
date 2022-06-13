package ellemes.gradle.mod.impl.dependency.maven

import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "project", strict = false)
final class ProjectObjectModel {
    @ElementList
    private List<Dependency> dependencies

    List<Dependency> getDependencies() {
        dependencies
    }
}
