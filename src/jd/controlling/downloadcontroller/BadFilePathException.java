package jd.controlling.downloadcontroller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.config.annotations.LabelInterface;

public class BadFilePathException extends IOException {
    public static enum Reason implements LabelInterface {
        PATH_SEGMENT_TOO_LONG {
            @Override
            public String getLabel() {
                return "A segment of path is too long to be used [in the current OS]";
            }
        },
        PATH_TOO_LONG {
            @Override
            public String getLabel() {
                return "Total length of path is too long";
            }
        },
        PERMISSION_PROBLEMS {
            @Override
            public String getLabel() {
                return "Permission to write to path is not given";
            }
        },
        FILE_EXISTS_AS_DIR {
            @Override
            public String getLabel() {
                return "File can't be created because a directory with the same name already exists";
            }
        },
        INVALID_DESTINATION {
            @Override
            public String getLabel() {
                return "This path cannot be used for unknown reasons";
            }
        };
    }

    private File file;
    private int  index;

    public BadFilePathException(final File file) {
        init(file, Reason.INVALID_DESTINATION, -1);
    }

    public BadFilePathException(final File file, final Reason reason) {
        init(file, reason, -1);
    }

    public BadFilePathException(final File file, final Reason reason, final int index) {
        // super(file);
        init(file, reason, index);
    }

    private void init(File file, final Reason reason, final int index) {
        this.file = file;
        this.index = index;
    }

    public File getFile() {
        return this.file;
    }

    public int getIndex() {
        return index;
    }

    public File getProblematicPathSegment() {
        /* TODO: Decide what this should return if no problematic path segment is known */
        if (this.index == -1) {
            return null;
        }
        final List<File> pathList = new ArrayList<File>();
        File next = this.getFile();
        while (true) {
            pathList.add(0, next);
            next = next.getParentFile();
            if (next == null) {
                /* We've reached the end. */
                break;
            }
        }
        return pathList.get(index);
    }
}
