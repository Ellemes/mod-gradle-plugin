package ellemes.gradle.mod.api.task;

import ellemes.gradle.mod.impl.MinifiedJsonReader;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.jvm.tasks.Jar;

import java.util.List;

public class MinifyJsonTask extends Jar {
    private final RegularFileProperty input;
    private final ListProperty<String> filePatterns;

    public MinifyJsonTask() {
        input = this.getProject().getObjects().fileProperty();
        filePatterns = this.getProject().getObjects().listProperty(String.class).convention(List.of("**/*.json", "**/*.mcmeta"));
    }

    @Override
    protected void copy() {
        input.finalizeValue();
        filePatterns.finalizeValue();
        this.from(this.getProject().zipTree(input.get()));
        this.filesMatching(filePatterns.get(), details -> details.filter(MinifiedJsonReader.class));
        super.copy();
    }

    @Input
    public ListProperty<String> getFilePatterns() {
        return filePatterns;
    }

    @InputFile
    public RegularFileProperty getInput() {
        return input;
    }
}
