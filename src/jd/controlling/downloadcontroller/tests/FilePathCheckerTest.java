package jd.controlling.downloadcontroller.tests;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appwork.testframework.AWTest;

public class FilePathCheckerTest extends AWTest {
    public static void main(String[] args) {
        run();
    }

    public void runTest() throws Exception {
        try {
        } finally {
            cleanup();
        }
    }

    private final List<File> CLEANUP = new CopyOnWriteArrayList<File>();

    private void cleanup() {
        for (File next : CLEANUP) {
            if (next.delete() || !next.exists()) {
                CLEANUP.remove(next);
            } else if (next.isFile()) {
                CLEANUP.remove(next);
                next.deleteOnExit();
            }
        }
    }
}
