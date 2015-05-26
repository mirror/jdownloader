package org.jdownloader.extensions.extraction.bindings.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.io.J7FileList;
import org.jdownloader.logging.LogController;

public class FileArchiveFactoryNIO {

    protected List<File> findFiles(Pattern pattern, File directory) {
        try {
            return J7FileList.findFiles(pattern, directory, true, false);
        } catch (IOException e) {
            LogController.GL.log(e);
            return new ArrayList<File>();
        }
    }
}
