package jd.controlling.downloadcontroller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BadDestinationExceptionPathTooLongV2 extends BadDestinationException {
    private final int index;

    public BadDestinationExceptionPathTooLongV2(final File file, final int index) {
        super(file);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public File getTooLongPathSegment() {
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
