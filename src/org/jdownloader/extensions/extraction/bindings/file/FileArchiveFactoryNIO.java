package org.jdownloader.extensions.extraction.bindings.file;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jdownloader.logging.LogController;

public class FileArchiveFactoryNIO {

    protected List<File> findFiles(Pattern pattern, File directory) {
        final ArrayList<File> ret = new ArrayList<File>();
        if (pattern != null && directory != null && directory.exists()) {
            DirectoryStream<Path> stream = null;
            try {
                final Path directoryPath = directory.toPath();
                final FileSystem fs = directoryPath.getFileSystem();
                final PathMatcher matcher = fs.getPathMatcher("regex:" + pattern.pattern());
                DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
                    @Override
                    public boolean accept(Path entry) {
                        return matcher.matches(entry.toAbsolutePath());
                    }
                };
                stream = fs.provider().newDirectoryStream(directoryPath, filter);
                for (final Path path : stream) {
                    final BasicFileAttributes pathAttr = Files.readAttributes(path, BasicFileAttributes.class);
                    if (pathAttr.isRegularFile()) {
                        ret.add(path.toFile());
                    }
                }
            } catch (final Throwable e) {
                LogController.CL().log(e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Throwable e) {
                    }
                }
            }
        }
        return ret;
    }
}
