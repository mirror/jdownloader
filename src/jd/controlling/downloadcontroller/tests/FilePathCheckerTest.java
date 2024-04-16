package jd.controlling.downloadcontroller.tests;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appwork.testframework.AWTest;

import jd.controlling.downloadcontroller.BadFilePathException;
import jd.controlling.downloadcontroller.FilePathChecker;

public class FilePathCheckerTest extends AWTest {
    public static void main(String[] args) {
        run();
    }

    public void runTest() throws Exception {
        try {
            /********************************************************************/
            /* Folder tests */
            // TODO: Maybe add better detection of such invalid paths
            /* Invalid path */
            final File testInvalidFilePath = new File("JD:\\\\Windows\\\\jdfilefoldertest");
            CLEANUP.add(testInvalidFilePath);
            testFolderCreationFail(testInvalidFilePath, new BadFilePathException(testInvalidFilePath, BadFilePathException.Reason.PERMISSION_PROBLEM));
            /*
             * Windows invalid path due to permission issue. If this test fails, application might have been started with admin permissions.
             */
            final File testPermissionIssue = new File("C:\\\\Windows\\\\jdfilefoldertest_test_we_cannot_write_here");
            CLEANUP.add(testPermissionIssue);
            testFolderCreationFail(testPermissionIssue, new BadFilePathException(testPermissionIssue, BadFilePathException.Reason.PERMISSION_PROBLEM));
            /********************************************************************/
            /* File tests */
            /* Windows too long filename test | If this fails, OS of tester != Windows */
            final File longfilenameTest = new File("C:\\\\jdfilefoldertest\\\\Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy.txt");
            CLEANUP.add(longfilenameTest);
            testFileCreationFail(longfilenameTest, new BadFilePathException(longfilenameTest, BadFilePathException.Reason.PATH_SEGMENT_TOO_LONG));
            /* Windows valid file path */
            final File normalFileTest = new File("C:\\\\jdfilefoldertest\\\\testfile.txt");
            CLEANUP.add(normalFileTest);
            testFileCreationSuccess(normalFileTest);
            System.out.println("SUCCESS");
        } finally {
            cleanup();
        }
    }

    private final List<File> CLEANUP = new CopyOnWriteArrayList<File>();

    private void cleanup() {
        for (final File next : CLEANUP) {
            if (next.delete() || !next.exists()) {
                CLEANUP.remove(next);
            } else if (next.isFile()) {
                CLEANUP.remove(next);
                next.deleteOnExit();
            }
        }
    }

    public static void testFolderCreationFail(final File file, final BadFilePathException expectedException) throws Exception {
        try {
            FilePathChecker.createFolderPath(file);
            throw new Exception("Expected Exception did not occur");
        } catch (final BadFilePathException bf) {
            // bf.printStackTrace();
            if (bf.getReason() != expectedException.getReason()) {
                throw new Exception("Expected FailureReason " + expectedException.getReason() + " | Got: " + bf.getReason());
            }
        }
    }

    public static void testFileCreationFail(final File file, final BadFilePathException expectedException) throws Exception {
        try {
            FilePathChecker.createFilePath(file);
            throw new Exception("Expected Exception did not occur");
        } catch (final BadFilePathException bf) {
            // bf.printStackTrace();
            if (bf.getReason() != expectedException.getReason()) {
                throw new Exception("Expected FailureReason " + expectedException.getReason() + " | Got: " + bf.getReason());
            }
        }
    }

    public static void testFolderCreationSuccess(final File file, final BadFilePathException expectedException) throws Exception {
        try {
            FilePathChecker.createFolderPath(file);
        } catch (final BadFilePathException bf) {
            // bf.printStackTrace();
            throw new Exception("Folder creation failed");
        }
    }

    public static void testFileCreationSuccess(final File file) throws Exception {
        try {
            FilePathChecker.createFilePath(file);
        } catch (final BadFilePathException bf) {
            // bf.printStackTrace();
            throw new Exception("File creation failed");
        }
    }

    public static void assertSuccessfulFileFolderCreation(final File File) throws Exception {
    }
}
