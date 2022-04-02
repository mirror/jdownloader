package org.jdownloader.extensions.extraction.bindings.file;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.io.J7FileList;

public class FileArchiveFactoryNIO {
    protected static List<File> findFiles(Pattern pattern, File directory) throws IOException {
        return J7FileList.findFiles(pattern, directory, true, false);
    }
}
