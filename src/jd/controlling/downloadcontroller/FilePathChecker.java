package jd.controlling.downloadcontroller;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.translate._JDT;

public class FilePathChecker {
    /**
     * Creates path of given File instance and performs write-test if wanted.
     *
     * @throws InterruptedException
     */
    public static void createFilePath(final File fileOutput, final boolean isFile, boolean checkFileWrite, boolean allowCheckForTooLongFilename) throws BadDestinationException, SkipReasonException, IOException, InterruptedException {
        final boolean checkForFileIsDirectoryForbidden = false;
        if (fileOutput == null) {
            throw new IllegalArgumentException();
        } else if (checkForFileIsDirectoryForbidden && isFile && fileOutput.isDirectory()) {
            // TODO: Throw exception(?) -> Evaluate if we really want to check for this problem here.
        } else if (fileOutput.exists()) {
            /* Already exists -> No need to do anything. */
            return;
        }
        // try {
        // validateDestination(fileOutput);
        // } catch (final PathTooLongException e) {
        // throw new SkipReasonException(SkipReason.INVALID_DESTINATION_TOO_LONG_PATH, e);
        // } catch (final BadDestinationException e) {
        // throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
        // }
        if (fileOutput.getParentFile() == null) {
            /* This should never happen! */
            // TODO: Maybe move this up to "fileOutput.isDirectory()" statement.
            // controller.getLogger().severe("has no parentFile?! " + fileOutput);
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
        }
        if (!checkFileWrite) {
            return;
        }
        /**
         * Create a list of the full folder path structure.
         */
        final List<File> pathList = new ArrayList<File>();
        int loop = 0;
        File next = fileOutput;
        int folderCreateStartSegmentIndex = -1;
        while (true) {
            pathList.add(0, next);
            if (folderCreateStartSegmentIndex == -1 && !next.exists()) {
                folderCreateStartSegmentIndex = loop;
            }
            next = next.getParentFile();
            if (next == null) {
                /* We've reached the end. */
                break;
            }
            loop++;
        }
        if (folderCreateStartSegmentIndex != -1) {
            /**
             * Manually create all folders up until we are in our final folder where we want to write the file we want to download. </br>
             * This may look more complicated compared to File.mkdirs() but this way we can know exactly at which point a directory could
             * not be created.
             */
            folderCreateStartSegmentIndex = pathList.size() - folderCreateStartSegmentIndex - 1;
            for (int index = folderCreateStartSegmentIndex; index < pathList.size(); index++) {
                final boolean isLastItem = index == pathList.size() - 1;
                if (isFile && isLastItem) {
                    /* Last path segment is file -> Do not create folder! */
                    break;
                }
                final File thisfolder = pathList.get(index);
                if (!thisfolder.exists() && !thisfolder.mkdir() && !thisfolder.isDirectory()) {
                    /* Folder creation failed */
                    if (CrossSystem.isWindows() && looksLikeTooLongWindowsPathOrFilename(thisfolder)) {
                        /*
                         * Assume that path is too long. We could check it by writing a shorter folder but it would not change the end
                         * result: The path is not usable for us.
                         */
                        // controller.getLogger().severe("Looks like too long downloadpath for Windows: " + thisfolder.getAbsolutePath());
                        throw new BadDestinationExceptionPathTooLongV2(thisfolder, index);
                    } else {
                        throw new SkipReasonException(SkipReason.INVALID_DESTINATION_PERMISSION_ISSUE, _JDT.T.DownloadLink_setSkipped_statusmessage_invalid_path_permission_issue_file(thisfolder.getName()));
                    }
                }
            }
        }
        /* Check file writability if needed. */
        if (isFile && checkFileWrite) {
            /* TODO: Use specific write check functionality here */
            final File writeTest1 = fileOutput;
            try {
                fileWriteCheck(writeTest1);
            } catch (final IOException e1) {
                /* Check for a too long filename */
                if (allowCheckForTooLongFilename) {
                    /* Filename looks to be too long but we don#t check. */
                    throw e1;
                }
                // TODO: Do not use a static string here
                final String shortFilename = "writecheck.txt";
                final File writeTest2 = new File(writeTest1.getParent(), shortFilename);
                if (writeTest2.exists()) {
                    // logger.info("File with shortened filename already exists!");
                    throw e1;
                }
                try {
                    fileWriteCheck(writeTest2);
                    /* We know that the given filename is too long because writing a file with a shorter filename was successful. */
                    throw new BadDestinationExceptionPathTooLongV2(fileOutput, pathList.size() - 1);
                } catch (final IOException e2) {
                    /* Permission issue or some other length limitation is in place. */
                    // logger.log(e2);
                    throw e1;
                }
            }
        }
    }

    public static void fileWriteCheck(final File file) throws IOException, SkipReasonException {
        // TODO: Maybe throw exception if file already exists
        // if (file.exists()) {
        // return;
        // }
        final RandomAccessFile raffile = IO.open(file, "rw");
        raffile.close();
        if (!file.delete()) {
            /* This should never happen! */
            // logger.warning("Failed to delete test-written file with shortened filename");
            throw new IOException("Failed to delete written file");
        }
        return;
    }

    public static boolean looksLikeTooLongWindowsPathOrFilename(final File file) throws IOException {
        // TODO: Add exceptions for "non limited" paths starting with "\\?\'drive-letter'\"
        final String[] folders = CrossSystem.getPathComponents(file);
        for (final String folder : folders) {
            if (looksLikeTooLongWindowsPathSegment(folder)) {
                return true;
            }
        }
        return false;
    }

    public static boolean looksLikeTooLongWindowsPathSegment(final String str) throws IOException {
        return str.length() > 255;
    }
}
