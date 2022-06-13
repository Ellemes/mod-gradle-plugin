package ellemes.gradle.mod.api.task

import ellemes.gradle.mod.impl.MinifiedJsonReader
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.jvm.tasks.Jar

class MinifyJsonTask extends Jar {
    private final RegularFileProperty input
    private final ListProperty<String> filePatterns

    MinifyJsonTask() {
        input = this.project.objects.fileProperty()
        filePatterns = this.project.objects.listProperty(String.class).convention(List.of("**/*.json", "**/*.mcmeta"))
    }

    @Override
    protected void copy() {
        input.finalizeValue()
        filePatterns.finalizeValue()
        this.from(this.project.zipTree(input.get()))
        this.filesMatching(filePatterns.get()) {
            filter(MinifiedJsonReader.class)
        }
        super.copy()
    }

    @Input
    ListProperty<String> getFilePatterns() {
        filePatterns
    }

    @InputFile
    RegularFileProperty getInput() {
        input
    }
}
