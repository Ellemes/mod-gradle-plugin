package ellemes.gradle.mod.api.task

import ellemes.gradle.mod.impl.TransformingClassVisitor
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

import java.nio.file.FileSystems
import java.nio.file.Files

// todo: figure out why this increases jar size substantially
// todo: adapt into content filter when https://github.com/gradle/gradle/issues/4010 is resolved.
class ParamLocalObfuscatorTask extends Jar {
    private final RegularFileProperty input
    private final Property<String> paramNameFormat
    private final Property<String> localNameFormat

    ParamLocalObfuscatorTask() {
        input = this.project.objects.fileProperty()
        paramNameFormat = this.project.objects.property(String.class).convention("p{0}")
        localNameFormat = this.project.objects.property(String.class).convention("l{0}")

        input.finalizeValueOnRead()
        paramNameFormat.finalizeValueOnRead()
        localNameFormat.finalizeValueOnRead()

        this.from(this.project.provider(() -> this.project.zipTree(input.get())))
    }

    @TaskAction
    void execute() throws IOException {
        String paramNameFormat = this.paramNameFormat.get()
        String localNameFormat = this.localNameFormat.get()

        String jarUri = "jar:" + this.archiveFile.get().asFile.toURI()
        try (var fs = FileSystems.newFileSystem(new URI(jarUri), Map.of())) {
            Files.walk(fs.getPath("/")).filter(it -> it.toString().endsWith(".class")).forEach(path -> {
                byte[] classBytes = null
                try (var inputStream = Files.newInputStream(path)) {
                    classBytes = inputStream.readAllBytes()
                } catch (IOException e) {
                    System.out.println("Failed to read class: " + path)
                    e.printStackTrace()
                }
                if (classBytes != null) {
                    var reader = new ClassReader(classBytes)
                    var writer = new ClassWriter(reader, 0)
                    reader.accept(new TransformingClassVisitor(writer, paramNameFormat, localNameFormat), 0)
                    classBytes = writer.toByteArray()
                    try (OutputStream out = Files.newOutputStream(path)) {
                        out.write(classBytes)
                    } catch (IOException e) {
                        System.out.println("Failed to write class: " + path)
                        e.printStackTrace()
                    }
                }
            })
        } catch (URISyntaxException e) {
            e.printStackTrace()
        }
    }

    @InputFile
    RegularFileProperty getInput() {
        input
    }

    @Input
    Property<String> getParamNameFormat() {
        paramNameFormat
    }

    @Input
    Property<String> getLocalNameFormat() {
        localNameFormat
    }
}
