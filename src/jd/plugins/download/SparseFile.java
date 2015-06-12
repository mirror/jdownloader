package jd.plugins.download;

import java.io.File;
import java.io.IOException;

public class SparseFile {
    public static boolean createSparseFile(File file) throws IOException {
        if (!file.exists()) {
            final java.nio.file.OpenOption[] options = { java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.SPARSE };
            final java.nio.channels.SeekableByteChannel channel = java.nio.file.Files.newByteChannel(file.toPath(), options);
            channel.close();
            return true;
        }
        return false;
    }
}
