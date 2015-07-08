package jd.plugins.download;

import java.io.File;
import java.io.IOException;

public class SparseFile {
    // Bug in Windows 7...
    // https://support.microsoft.com/en-us/kb/2708811?wa=wsignin1.0
    public static boolean createSparseFile(File file) throws IOException {
        if (!file.exists()) {
            final java.nio.file.OpenOption[] options = { java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.SPARSE };
            try {
                final java.nio.channels.SeekableByteChannel channel = java.nio.file.Files.newByteChannel(file.toPath(), options);
                channel.close();
                return true;
            } catch (IllegalArgumentException e) {
                throw new IOException(e);
            }
        }
        return false;
    }

}
