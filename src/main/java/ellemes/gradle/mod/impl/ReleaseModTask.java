package ellemes.gradle.mod.impl;

import org.gradle.api.DefaultTask;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ReleaseModTask extends DefaultTask {
    private final File gitParentDirectory;

    @Inject
    public ReleaseModTask(File gitParentDirectory) {
        this.gitParentDirectory = gitParentDirectory;
    }

    public void doPreReleaseChecks() {
        if (!System.getProperty("MOD_IGNORE_CHANGES", "false").equalsIgnoreCase("false")) {
            return;
        }
        Exception error = null;
        boolean hasChanges = false;
        try {
            Process process = new ProcessBuilder()
                    .directory(gitParentDirectory)
                    .command("git", "status", "--porcelain")
                    .start();
            process.waitFor();
            try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
                hasChanges = reader.readLine() != null;
            }
        } catch (IOException | InterruptedException e) {
            error = e;
        }
        if (hasChanges) {
            throw new IllegalStateException("Cannot release with uncommitted changes.");
        } else if (error != null) {
            throw new IllegalStateException("Error occurred whilst checking for uncommitted changes.", error);
        }
        boolean sameAsRemote = false;
        try {
            Process process = new ProcessBuilder()
                    .directory(gitParentDirectory)
                    .command("git", "status", "-b", "--porcelain=2")
                    .start();
            process.waitFor();
            try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(" ");
                    if (parts.length >= 2 && "branch.ab".equals(parts[1])) {
                        sameAsRemote = parts[2].equals("+0") && parts[3].equals("-0");
                        break;
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            error = e;
        }
        if (!sameAsRemote) {
            throw new IllegalStateException("Cannot release with un-pushed changes.");
        } else if (error != null) {
            throw new IllegalStateException("Error occurred whilst checking for un-pushed changes.", error);
        }
    }
}
