package jd.controlling.downloadcontroller.tests;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appwork.testframework.AWTest;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;

import jd.controlling.downloadcontroller.BadFilePathException;
import jd.controlling.downloadcontroller.FilePathChecker;
import jd.controlling.downloadcontroller.FilePathChecker.CheckFlag;

public class FilePathCheckerTest extends AWTest {
    public static void main(String[] args) {
        run();
    }

    public void runTest() throws Exception {
        boolean allowDeleteFolderRecursive = false;
        final String windowsTestBasePath = "jdownloaderfilefoldertests";
        final File testBasePath = new File(windowsTestBasePath);
        System.out.print(testBasePath.getAbsolutePath());
        try {
            /* Prepare test: create required files and folders */
            if (testBasePath.exists()) {
                /* Folder existed before this test got executed thus we're not allowed to delete it once our tests are done. */
                final File[] filesfolders = testBasePath.listFiles();
                if (filesfolders == null || filesfolders.length == 0) {
                    allowDeleteFolderRecursive = true;
                } else {
                    allowDeleteFolderRecursive = false;
                }
            } else {
                /*
                 * Folder did not exist before this test was executed thus we can delete that folder and all files inside of it once our
                 * test is done.
                 */
                testBasePath.mkdirs();
                allowDeleteFolderRecursive = true;
            }
            final File testfile = new File(testBasePath, "testfile.txt");
            final RandomAccessFile raffile = IO.open(testfile, "rw");
            raffile.close();
            CLEANUP.add(testfile);
            final File testfolderWithFilename = new File(testBasePath, "test_folder_with_filename.txt");
            testfolderWithFilename.mkdirs();
            CLEANUP.add(testfolderWithFilename);
            if (CrossSystem.isWindows()) {
                /* Windows specific tests */
                /********************************************************************/
                /* Folder tests */
                // testBasePathNoWrite.setWritable(false);
                final File testBasePathNoWrite = new File(windowsTestBasePath, "nowrite");
                testBasePathNoWrite.setReadOnly();
                testBasePathNoWrite.mkdirs();
                /* Invalid path */
                final File testInvalidFilePath = new File(testBasePath, "invalid:folder_because_of_invalid_char_colon");
                CLEANUP.add(testInvalidFilePath);
                testFolderCreationFail(testInvalidFilePath, new BadFilePathException(testInvalidFilePath, BadFilePathException.PathFailureReason.PERMISSION_PROBLEM));
                /*
                 * Windows invalid path due to permission issue. If this test fails, application might have been started with admin
                 * permissions.
                 */
                final boolean doPermissionTest = false;
                if (doPermissionTest) {
                    // TODO: Fix this test
                    final File testPermissionIssue = new File(testBasePathNoWrite, "we_cant_write_this_subfolder");
                    CLEANUP.add(testPermissionIssue);
                    testFolderCreationFail(testPermissionIssue, new BadFilePathException(testPermissionIssue, BadFilePathException.PathFailureReason.PERMISSION_PROBLEM));
                }
                /********************************************************************/
                /* File tests */
                /* Windows too long filename test | If this fails, OS of tester != Windows */
                final File longfilenameTest = new File(testBasePath, "too_long_filename_test_Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy.txt");
                CLEANUP.add(longfilenameTest);
                testFileCreationFail(longfilenameTest, new BadFilePathException(longfilenameTest, BadFilePathException.PathFailureReason.PATH_SEGMENT_TOO_LONG));
            }
            /* Test with file that already exists */
            testFileCreationFail(testfile, new BadFilePathException(testfile, BadFilePathException.PathFailureReason.FILE_ALREADY_EXISTS), new CheckFlag[] { CheckFlag.ERROR_ON_ALREADY_EXIST });
            /* Test: File we want to create but it already exists as a folder */
            final File testInvalidFilePath = new File(testfolderWithFilename.getAbsolutePath());
            testFileCreationFail(testInvalidFilePath, new BadFilePathException(testInvalidFilePath, BadFilePathException.PathFailureReason.FILE_ALREADY_EXISTS_AS_FOLDER));
            /* Valid folder path */
            final File normalFolderAndSubfolderTest = new File(testBasePath, "subfolder");
            CLEANUP.add(normalFolderAndSubfolderTest);
            testFolderCreationSuccess(normalFolderAndSubfolderTest);
            /* Valid file path */
            final File normalFileTest = new File(testBasePath, "testfile_valid_filename_length.txt");
            CLEANUP.add(normalFileTest);
            testFileCreationSuccess(normalFileTest);
            System.out.println("SUCCESS");
        } finally {
            cleanup();
            if (allowDeleteFolderRecursive) {
                deleteRecursive(testBasePath);
            } else {
                System.out.println("!! TEST ENDED BUT SOME TEST FILES MAY BE LEFT !!");
            }
        }
    }

    private final List<File> CLEANUP = new CopyOnWriteArrayList<File>();

    /** Deletes files created during tests. */
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

    private static void deleteRecursive(final File folder) {
        if (folder.isDirectory()) {
            final File[] filesfolders = folder.listFiles();
            if (filesfolders != null) {
                for (final File file : filesfolders) {
                    deleteRecursive(file);
                }
            }
        }
        folder.delete();
    }

    public static void testFolderCreationFail(final File file, final BadFilePathException expectedException) throws Exception {
        try {
            FilePathChecker.createFolderPath(file);
            throw new Exception("Expected Exception did not occur");
        } catch (final BadFilePathException bf) {
            if (bf.getReason() != expectedException.getReason()) {
                bf.printStackTrace();
                throw new Exception("Expected FailureReason " + expectedException.getReason() + " | Got: " + bf.getReason());
            }
        }
    }

    public static void testFileCreationFail(final File file, final BadFilePathException expectedException) throws Exception {
        try {
            FilePathChecker.createFilePath(file);
            throw new Exception("Expected Exception did not occur");
        } catch (final BadFilePathException bf) {
            if (bf.getReason() != expectedException.getReason()) {
                bf.printStackTrace();
                throw new Exception("Expected FailureReason " + expectedException.getReason() + " | Got: " + bf.getReason());
            }
        }
    }

    public static void testFileCreationFail(final File file, final BadFilePathException expectedException, final CheckFlag... flags) throws Exception {
        try {
            FilePathChecker.createFilePath(file, flags);
            throw new Exception("Expected Exception did not occur");
        } catch (final BadFilePathException bf) {
            if (bf.getReason() != expectedException.getReason()) {
                bf.printStackTrace();
                throw new Exception("Expected FailureReason " + expectedException.getReason() + " | Got: " + bf.getReason());
            }
        }
    }

    public static void testFolderCreationSuccess(final File file) throws Exception {
        try {
            FilePathChecker.createFolderPath(file);
        } catch (final BadFilePathException bf) {
            bf.printStackTrace();
            throw new Exception("Folder creation failed");
        }
    }

    public static void testFileCreationSuccess(final File file) throws Exception {
        try {
            FilePathChecker.createFilePath(file);
        } catch (final BadFilePathException bf) {
            bf.printStackTrace();
            throw new Exception("File creation failed");
        }
    }

    public static void assertSuccessfulFileFolderCreation(final File File) throws Exception {
    }
}
