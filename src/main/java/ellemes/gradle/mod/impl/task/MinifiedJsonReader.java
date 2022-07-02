package ellemes.gradle.mod.impl.task;

import org.jetbrains.annotations.NotNull;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

public final class MinifiedJsonReader extends FilterReader {
    private boolean inString = false;
    private boolean escapeNextChar = false;

    public MinifiedJsonReader(@NotNull Reader reader) {
        super(reader);
    }

    @Override
    public int read() throws IOException {
        int character = super.read();
        boolean escapeThisChar = escapeNextChar;
        escapeNextChar = false;
        if (!inString) {
            if (character == '"') {
                inString = true;
            } else if (character == ' ' || character == '\r' || character == '\n' || character == '\t') {
                // Skip and return next character
                return this.read();
            }
        } else {
            if (!escapeThisChar) {
                if (character == '"') {
                    inString = false;
                } else if (character == '\\') {
                    escapeNextChar = true;
                }
            }
        }
        return character;
    }

    @Override
    public int read(char[] buffer, int offset, int length) throws IOException {
        for (int index = 0; index < length; ++index) {
            int character = this.read();
            if (character == -1) {
                return index == 0 ? -1 : index;
            }
            buffer[offset + index] = (char) character;
        }
        return length;
    }
}
