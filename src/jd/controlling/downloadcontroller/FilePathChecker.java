package jd.controlling.downloadcontroller;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.controlling.UniqueAlltimeID;

public class FilePathChecker {
    /**
     * The idea is that you can provide a list of flags to the folder create function so it knows to which extend it is allowed to perform
     * actions such as write-checks.
     */
    public static enum CheckFlag {
        CHECK_FOLDER_CREATE,
        CHECK_FILE_WRITE,
        CHECK_FILE_FOR_TOO_LONG_FILENAME,
        IS_FILE,
        ERROR_ON_ALREADY_EXIST
    }

    public static void createFilePath(final File file) throws BadDestinationException, IOException {
        createFilePath(file, new CheckFlag[] { CheckFlag.IS_FILE, CheckFlag.CHECK_FILE_WRITE, CheckFlag.CHECK_FILE_FOR_TOO_LONG_FILENAME });
    }

    public static void createFolderPath(final File file) throws BadDestinationException, IOException {
        createFilePath(file, new CheckFlag[] { CheckFlag.CHECK_FOLDER_CREATE });
    }

    /**
     * Creates path of given File instance and performs write-test if wanted.
     *
     * @throws InterruptedException
     */
    public static void createFilePath(final File fileOutput, final CheckFlag... flags) throws BadDestinationException, IOException {
        boolean isFile = false;
        boolean checkFileWrite = false;
        boolean checkFolderCreate = false;
        boolean allowCheckForTooLongFilename = false;
        boolean errorOnAlreadyExist = false;
        if (flags != null) {
            for (final CheckFlag flag : flags) {
                if (flag == CheckFlag.IS_FILE) {
                    isFile = true;
                } else if (flag == CheckFlag.CHECK_FILE_WRITE) {
                    checkFileWrite = true;
                } else if (flag == CheckFlag.CHECK_FOLDER_CREATE) {
                    checkFolderCreate = true;
                } else if (flag == CheckFlag.CHECK_FILE_FOR_TOO_LONG_FILENAME) {
                    allowCheckForTooLongFilename = true;
                } else if (flag == CheckFlag.ERROR_ON_ALREADY_EXIST) {
                    errorOnAlreadyExist = true;
                }
            }
        }
        if (fileOutput == null) {
            throw new IllegalArgumentException("fileOutput can't be null");
        } else if (isFile && fileOutput.isDirectory()) {
            throw new BadFilePathException(fileOutput, BadFilePathException.Reason.FILE_ALREADY_EXISTS_AS_FOLDER);
        } else if (fileOutput.exists()) {
            /* Already exists -> No need to do anything. */
            return;
        }
        if (fileOutput.getParentFile() == null) {
            // OS root
            /* This should never happen! */
            // TODO: Maybe move this up to "fileOutput.isDirectory()" statement.
            // controller.getLogger().severe("has no parentFile?! " + fileOutput);
            throw new BadFilePathException(fileOutput, BadFilePathException.Reason.INVALID_DESTINATION);
        }
        /* Validate path without writing anything */
        pathValidation: {
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
            // TODO: Make this nicer
            if (checking == null) {
                break pathValidation;
            } else if (checking.exists() && checking.isDirectory()) {
                break pathValidation;
            } else {
                /* Invalid path according to path validation */
                throw new BadFilePathException(fileOutput, BadFilePathException.Reason.INVALID_DESTINATION);
            }
        }
        if (!checkFolderCreate && !checkFileWrite) {
            /* No errors until now and we're not allowed to write -> Call it success */
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
            if (folderCreateStartSegmentIndex != -1 || (folderCreateStartSegmentIndex == -1 && !next.exists())) {
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
                    /* Folder creation failed -> Check/assume why */
                    if (CrossSystem.isWindows() && looksLikeTooLongWindowsPathOrFilename(thisfolder)) {
                        /*
                         * Assume that path is too long. We could check it by writing a shorter folder but it would not change the end
                         * result: The path is not usable for us.
                         */
                        // controller.getLogger().severe("Looks like too long downloadpath for Windows: " + thisfolder.getAbsolutePath());
                        throw new BadFilePathException(thisfolder, BadFilePathException.Reason.PATH_SEGMENT_TOO_LONG, index);
                    } else {
                        throw new BadFilePathException(thisfolder, BadFilePathException.Reason.PERMISSION_PROBLEM, index);
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
                    throw new BadFilePathException(fileOutput, BadFilePathException.Reason.PERMISSION_PROBLEM, pathList.size() - 1);
                }
                final File writeTest2 = new File(writeTest1.getParent(), "jd_accessCheck_" + new UniqueAlltimeID().getID());
                if (writeTest2.exists()) {
                    /* This shall never happen */
                    // throw e1;
                    /*
                     * Assume that we didn't write this file -> We don't know if the problem is that the filename is too long or if there is
                     * a permission issue -> Assume permission issue.
                     */
                    throw new BadFilePathException(fileOutput, BadFilePathException.Reason.PERMISSION_PROBLEM, pathList.size() - 1);
                }
                try {
                    fileWriteCheck(writeTest2);
                } catch (final IOException e2) {
                    /* Permission issue because we were unable to write any file in this directory. */
                    throw new BadFilePathException(fileOutput, BadFilePathException.Reason.PERMISSION_PROBLEM, pathList.size() - 1);
                }
                /*
                 * We assume that the given filename is too long because writing a file with a shorter filename was successful.
                 */
                throw new BadFilePathException(fileOutput, BadFilePathException.Reason.PATH_SEGMENT_TOO_LONG, pathList.size() - 1);
            }
        }
    }

    /** Writes file and deletes it again. */
    public static void fileWriteCheck(final File file) throws IOException {
        // TODO: Maybe throw exception if file already exists
        final boolean checkForExists = false;
        if (checkForExists && file.isDirectory()) {
            // return;
            throw new BadFilePathException(file, BadFilePathException.Reason.FILE_ALREADY_EXISTS_AS_FOLDER);
        } else if (checkForExists && file.exists()) {
            // return;
            throw new BadFilePathException(file, BadFilePathException.Reason.FILE_ALREADY_EXISTS);
        }
        final RandomAccessFile raffile = IO.open(file, "rw");
        raffile.close();
        if (!file.delete()) {
            /* This should never happen! */
            // logger.warning("Failed to delete test-written file with shortened filename");
            throw new IOException("Failed to delete written file");
        }
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

    public static void main(String[] args) throws BadDestinationException, IOException {
        final File testfile = new File("JD:\\\\Windows\\\\jdfoldertest");
        final BadFilePathException permissionErrorExpectedResult = new BadFilePathException(testfile, BadFilePathException.Reason.PERMISSION_PROBLEM);
    }
}
