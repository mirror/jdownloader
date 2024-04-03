package jd.controlling.downloadcontroller;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.plugins.SkipReasonException;

public class FilePathChecker {
    /**
     * The idea is that you can provide a list of flags to the folder create function so it knows to which extend it is allowed to perform
     * actions such as write-checks.
     */
    public static enum CheckFlags {
        FILE_FOLDER_CREATE,
        CHECK_FOR_TOO_LONG_FILENAME
    }

    /**
     * Creates path of given File instance and performs write-test if wanted.
     *
     * @throws InterruptedException
     */
    public static void createFilePath(final File fileOutput, final boolean isFile, boolean checkFileWrite, boolean allowCheckForTooLongFilename) throws BadDestinationException, SkipReasonException, IOException, InterruptedException {
        final boolean checkForFileIsDirectoryForbidden = false;
        if (fileOutput == null) {
            throw new IllegalArgumentException("fileOutput can't be null");
        } else if (checkForFileIsDirectoryForbidden && isFile && fileOutput.isDirectory()) {
            // TODO: Evaluate if we really want to check for this problem here.
            throw new BadFilePathException(fileOutput, BadFilePathException.Reason.FILE_EXISTS_AS_DIR);
        } else if (fileOutput.exists()) {
            /* Already exists -> No need to do anything. */
            return;
        }
        if (fileOutput.getParentFile() == null) {
            /* This should never happen! */
            // TODO: Maybe move this up to "fileOutput.isDirectory()" statement.
            // controller.getLogger().severe("has no parentFile?! " + fileOutput);
            throw new BadFilePathException(fileOutput, BadFilePathException.Reason.INVALID_DESTINATION);
        }
        /* Validate path without writing anything */
        {
            File checking = null;
            String[] folders;
            switch (CrossSystem.getOSFamily()) {
            case LINUX:
                folders = CrossSystem.getPathComponents(fileOutput);
                if (folders.length >= 3) {
                    final String userName = System.getProperty("user.name");
                    if (folders.length >= 4 && "run".equals(folders[1]) && "media".equals(folders[2]) && folders[3].equals(userName)) {
                        /* 0:/ | 1:run | 2:media | 3:user | 4:mounted volume */
                        checking = new File("/run/media/" + userName + "/" + folders[4]);
                    } else if ("media".equals(folders[1])) {
                        /* 0:/ | 1:media | 2:mounted volume */
                        checking = new File("/media/" + folders[2]);
                    } else if ("mnt".equals(folders[1])) {
                        /* 0:/ | 1:media | 2:mounted volume */
                        checking = new File("/mnt/" + folders[2]);
                    }
                }
                break;
            case MAC:
                folders = CrossSystem.getPathComponents(fileOutput);
                if (folders.length >= 3) {
                    if ("media".equals(folders[1])) {
                        /* 0:/ | 1:media | 2:mounted volume */
                        checking = new File("/media/" + folders[2]);
                    } else if ("mnt".equals(folders[1])) {
                        /* 0:/ | 1:media | 2:mounted volume */
                        checking = new File("/mnt/" + folders[2]);
                    } else if ("Volumes".equals(folders[1])) {
                        /* 0:/ | 1:Volumes | 2:mounted volume */
                        checking = new File("/Volumes/" + folders[2]);
                    }
                }
                break;
            case WINDOWS:
            default:
                if (CrossSystem.getOS().isMaximum(OperatingSystem.WINDOWS_NT) && fileOutput.getAbsolutePath().length() > 259) {
                    // old windows API does not allow longer paths
                    checking = fileOutput;
                    throw new BadFilePathException(fileOutput, BadFilePathException.Reason.PATH_TOO_LONG);
                } else {
                    folders = CrossSystem.getPathComponents(fileOutput);
                    if (folders.length > 0) {
                        String root = folders[0];
                        if (root.matches("^[a-zA-Z]{1}:\\\\$") || root.matches("^[a-zA-Z]{1}://$")) {
                            /* X:/ or X:\ */
                            checking = new File(folders[0]);
                        } else if (root.equals("\\\\")) {
                            if (folders.length >= 3) {
                                /* \\\\computer\\folder\\ in network */
                                checking = new File(folders[0] + folders[1] + "\\" + folders[2]);
                            }
                        }
                    }
                }
            }
            if (checking != null && checking.exists() && checking.isDirectory()) {
                checking = null;
            }
            if (checking != null) {
                // throw new BadDestinationException(checking);
                throw new BadFilePathException(fileOutput, BadFilePathException.Reason.INVALID_DESTINATION);
            }
        }
        if (!checkFileWrite) {
            /* No errors until now and we're not allowed to write -> Cann it success */
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
                        throw new BadFilePathException(thisfolder, BadFilePathException.Reason.PATH_SEGMENT_TOO_LONG, index);
                    } else {
                        throw new BadFilePathException(thisfolder, BadFilePathException.Reason.PERMISSION_PROBLEMS, index);
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
                if (!allowCheckForTooLongFilename) {
                    /* Filename looks to be too long but we don't check. */
                    // throw e1;
                    /* We're not checking for too long filename -> Assume it is a permission problem */
                    throw new BadFilePathException(fileOutput, BadFilePathException.Reason.PERMISSION_PROBLEMS);
                }
                // TODO: Do not use a static string here
                final String shortFilename = "writecheck.txt";
                final File writeTest2 = new File(writeTest1.getParent(), shortFilename);
                if (writeTest2.exists()) {
                    // logger.info("File with shortened filename already exists!");
                    // throw e1;
                    /*
                     * Assume that we didn't write this file -> We don't know if the problem is that the filename is too long or if there is
                     * a permission issue -> Assume permission issue.
                     */
                    throw new BadFilePathException(fileOutput, BadFilePathException.Reason.PERMISSION_PROBLEMS);
                }
                try {
                    fileWriteCheck(writeTest2);
                    /* We know that the given filename is too long because writing a file with a shorter filename was successful. */
                    // TODO: Provide index of failed path segment
                    throw new BadFilePathException(fileOutput, BadFilePathException.Reason.PATH_SEGMENT_TOO_LONG);
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
