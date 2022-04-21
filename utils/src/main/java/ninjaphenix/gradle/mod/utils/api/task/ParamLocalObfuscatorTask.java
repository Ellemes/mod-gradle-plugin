package ninjaphenix.gradle.mod.utils.api.task;

import ninjaphenix.gradle.mod.utils.impl.task.TransformingClassVisitor;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Map;

// todo: figure out why this increases jar size substantially
// todo: adapt into content filter when https://github.com/gradle/gradle/issues/4010 is resolved.
public class ParamLocalObfuscatorTask extends Jar {
    private final RegularFileProperty input;
    private final Property<String> paramNameFormat;
    private final Property<String> localNameFormat;

    public ParamLocalObfuscatorTask() {
        input = this.getProject().getObjects().fileProperty();
        paramNameFormat = this.getProject().getObjects().property(String.class).convention("p{0}");
        localNameFormat = this.getProject().getObjects().property(String.class).convention("l{0}");

        input.finalizeValueOnRead();
        paramNameFormat.finalizeValueOnRead();
        localNameFormat.finalizeValueOnRead();

        this.from(this.getProject().provider(() -> this.getProject().zipTree(input.get())));
    }

    @TaskAction
    public void execute() throws IOException {
        String paramNameFormat = this.paramNameFormat.get();
        String localNameFormat = this.localNameFormat.get();

        String jarUri = "jar:" + this.getArchiveFile().get().getAsFile().toURI();
        try (var fs = FileSystems.newFileSystem(new URI(jarUri), Map.of())) {
            Files.walk(fs.getPath("/")).filter(it -> it.toString().endsWith(".class")).forEach(path -> {
                byte[] clazz = null;
                try (var in = Files.newInputStream(path)) {
                    clazz = in.readAllBytes();
                } catch (IOException e) {
                    System.out.println("Failed to read class: " + path);
                    e.printStackTrace();
                }
                if (clazz != null) {
                    var reader = new ClassReader(clazz);
                    var writer = new ClassWriter(reader, 0);
                    reader.accept(new TransformingClassVisitor(writer, paramNameFormat, localNameFormat), 0);
                    clazz = writer.toByteArray();
                    try (var out = Files.newOutputStream(path)) {
                        out.write(clazz);
                    } catch (IOException e) {
                        System.out.println("Failed to write class: " + path);
                        e.printStackTrace();
                    }
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @InputFile
    public RegularFileProperty getInput() {
        return input;
    }

    @Input
    public Property<String> getParamNameFormat() {
        return paramNameFormat;
    }

    @Input
    public Property<String> getLocalNameFormat() {
        return localNameFormat;
    }
}
