//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.nutils;

/**
 * Utilities to move, copy or delete files
 * @author scr4ve
 * @version 1.0
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jd.controlling.JDLogger;
import jd.utils.JDUtilities;

public class FileUtils {

    private static String win_RecyclePath = null;
    public static final int DeletePermanently = 0;
    public static final int MoveToRecycleBin = 1;

    // Don't change except you know what you're doing
    private static int DefaultDeleteAction = DeletePermanently;

    private static boolean CopyOverwriteAllowedByDefault = false;
    private static boolean MoveOverwriteAllowedByDefault = false;

    /**
     * @return the default action for deleting files.
     */
    public static int getDefaultDeleteAction() {
        return DefaultDeleteAction;
    }

    /**
     * @param defaultDeleteAction
     *            the default method for deleting files to set
     */
    public static void setDefaultDeleteAction(int defaultDeleteAction) {
        DefaultDeleteAction = defaultDeleteAction;
    }

    /**
     * @return the default behavior for overwriting files while copying
     */
    public static boolean isCopyOverwriteAllowedByDefault() {
        return CopyOverwriteAllowedByDefault;
    }

    /**
     * @param copyOverwriteAllowedByDefault
     *            the default behavior for overwriting files while copying to
     *            set
     */
    public static void setCopyOverwriteAllowedByDefault(boolean copyOverwriteAllowedByDefault) {
        CopyOverwriteAllowedByDefault = copyOverwriteAllowedByDefault;
    }

    /**
     * @return the default behavior for overwriting files while moving
     */
    public static boolean isMoveOverwriteAllowedByDefault() {
        return MoveOverwriteAllowedByDefault;
    }

    /**
     * @param moveOverwriteAllowedByDefault
     *            the default behavior for overwriting files while moving to set
     */
    public static void setMoveOverwriteAllowedByDefault(boolean moveOverwriteAllowedByDefault) {
        MoveOverwriteAllowedByDefault = moveOverwriteAllowedByDefault;
    }

    private static boolean deletePermanently(File file) {
        if (!file.isAbsolute()) {
            JDLogger.getLogger().warning("ATTENTION! Deleting file without absolute path (unrecommended): " + file);
        }
        if (!file.delete()) {
            JDLogger.getLogger().warning("Couldn't delete file instantly, deleteOnExit instead.");
            file.deleteOnExit();
        }
        JDLogger.getLogger().fine("File deleted: " + file);
        return true;
    }

    private static String getWin_RecyclePath() {
        return (win_RecyclePath == null) ? (win_RecyclePath = JDUtilities.getResourceFile("tools\\windows\\recycle.exe").getAbsolutePath()) : win_RecyclePath;
    }

    private static boolean movetoRecycleBin(File file) {
        boolean success = false;
        if (OSDetector.isWindows()) {
            if (new File(getWin_RecyclePath()).exists() && new File(getWin_RecyclePath()).canExecute()) {
                Executer ex = new Executer(getWin_RecyclePath());
                ex.addParameter(file.getAbsolutePath());
                ex.run();
                if (ex.getExitValue() == 1) {
                    success = true;
                } else {
                    JDLogger.getLogger().warning("recycle.exe returned with exit code " + ex.getExitValue());
                }
            }
        }
        /*
         * else if(OSDetector.isLinux()) { success = true; } else
         * if(OSDetector.isMac()) { success = true; }
         */

        if (success) {
            JDLogger.getLogger().fine("File moved to recycle bin: " + file);
            return true;
        }
        return false;
    }

    /**
     * @return Returns {@code true} if the current operating system has recycle
     *         bin support, {@code false} otherwise
     */
    public static boolean OShasRecycleBinSupport() {
        if (OSDetector.isWindows()) { return true; }
        // TODO: Implement recycle bin support for other operating systems
        return false;
    }

    /**
     * Deletes a file with a specified method, for example by moving it into the
     * recycle bin
     * 
     * @return {@code true} if and only if the file is successfully deleted;
     *         {@code false} otherwise
     * @param file
     *            the File to delete
     * @param method
     *            the method for deleting the file
     */
    public static boolean delete(File file, int method) {
        if (!file.exists()) {
            JDLogger.getLogger().warning("Trying to delete a nonexisting file: " + file.getAbsolutePath());
            return true;
        }
        if ((method == MoveToRecycleBin) // if recycle method is specified
                && (OShasRecycleBinSupport()) // and is available on the OS
                && (movetoRecycleBin(file))) { // and is successful
            return true; // return success, otherwise
        } else { // delete permanently
            if ((method == MoveToRecycleBin) && (!OShasRecycleBinSupport())) {
                JDLogger.getLogger().fine("No RecycleBin support for " + OSDetector.getOSString() + ". Deleting permanently instead.");
            }
            if ((method == MoveToRecycleBin) && (OShasRecycleBinSupport())) {
                JDLogger.getLogger().fine("Moving to recycle bin failed. OS[" + OSDetector.getOSString() + "] File[" + file.getAbsolutePath() + "]. Deleting permanently instead.");
            }
            return deletePermanently(file);
        }
    }

    /**
     * Deletes a file with the default method for deleting files, for example by
     * moving it into the recycle bin
     * 
     * @see {@code FileUtils.delete(file, method)}
     * @return {@code true} if and only if the file is successfully deleted;
     *         {@code false} otherwise
     * @param file
     *            the File to delete
     */
    public static boolean delete(File file) {
        return delete(file, DefaultDeleteAction);
    }

    /**
     * Deletes a file with a specified method, for example by moving it into the
     * recycle bin
     * 
     * @return {@code true} if and only if the file is successfully deleted;
     *         {@code false} otherwise
     * @param file
     *            the absolute path of the file
     * @param method
     *            the method for deleting the file
     */
    public static boolean delete(String file, int method) {
        if (file != null && file.trim().length() > 0) { return delete(new File(file), method); }
        return false;
    }

    /**
     * Deletes a file with the default method for deleting files, for example by
     * moving it into the recycle bin
     * 
     * @see {@code FileUtils.delete(file, method)}
     * @return {@code true} if and only if the file is successfully deleted;
     *         {@code false} otherwise
     * @param file
     *            the absolute path of the file
     */
    public static boolean delete(String file) {
        return delete(file, DefaultDeleteAction);
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[0xFFFF];
        for (int len; (len = in.read(buffer)) != -1;)
            out.write(buffer, 0, len);
    }

    /**
     * Copies a file with specified options
     * 
     * @param src
     *            the source File
     * @param dest
     *            the destination File
     * @param allowOverwrite
     *            {@code true} if files should be overwritten by default
     * @return {@code true} if and only if the file is successfully copied;
     *         {@code false} otherwise
     */
    public static boolean copy(File src, File dest, boolean allowOverwrite) {

        if (dest.exists()) {
            if (allowOverwrite) {
                if (!dest.delete()) {
                    JDLogger.getLogger().warning("Destination file [" + dest.getAbsolutePath() + "] already exists and could not be deleted");
                    return false;
                }
            } else {
                JDLogger.getLogger().warning("Destination file [" + dest.getAbsolutePath() + "] already exists (Overwriting permitted)");
                return false;
            }
        }

        boolean success = true;
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dest);

            copy(fis, fos);
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        } finally {
            if (fis != null) try {
                fis.close();
            } catch (IOException e) {
                success = false;
            }
            if (fos != null) try {
                fos.close();
            } catch (IOException e) {
                success = false;
            }
        }

        if (success) {
            JDLogger.getLogger().fine(src.getAbsolutePath() + " has been copied to " + dest.getAbsolutePath());
            return true;
        } else {
            JDLogger.getLogger().warning("Couldn't copy file " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
        }
        return false;
    }

    /**
     * Copies a file with default options
     * 
     * @see {@code FileUtils.copy(src, dest, allowOverwrite)}
     * @param src
     *            the source File
     * @param dest
     *            the destination File
     * @return {@code true} if and only if the file is successfully copied;
     *         {@code false} otherwise
     */
    public static boolean copy(File src, File dest) {
        return copy(src, dest, CopyOverwriteAllowedByDefault);
    }

    /**
     * Moves a file with specified options
     * 
     * @param src
     *            the source File
     * @param dest
     *            the destination File
     * @param allowOverwrite
     *            {@code true} if files should be overwritten by default
     * @return {@code true} if and only if the file is successfully moved;
     *         {@code false} otherwise
     */
    public static boolean move(File src, File dest, boolean allowOverwrite) {

        if ((src == null) || (!src.exists())) { throw new IllegalStateException("File [" + src != null ? src.getAbsolutePath() : "" + "] to move doesnt exist."); }

        if (dest.exists()) {
            if (allowOverwrite) {
                if (!delete(dest, FileUtils.DeletePermanently)) {
                    JDLogger.getLogger().warning("Destination file [" + dest.getAbsolutePath() + "] already exists and could not be deleted");
                    return false;
                }
            } else {
                JDLogger.getLogger().warning("Destination file [" + dest.getAbsolutePath() + "] already exists (Overwriting permitted)");
                return false;
            }
        }

        boolean moved = false;
        if (src != null) {
            moved = src.renameTo(dest);
            if (!moved) {
                JDLogger.getLogger().fine("renameTo failed, trying to copy file now src[" + src.getAbsolutePath() + "] dest[" + dest.getAbsolutePath() + "]");
                if (copy(src, dest, allowOverwrite)) {
                    if (dest.exists() && (dest.length() == src.length())) {
                        delete(src, FileUtils.DeletePermanently);
                        moved = true;
                    } else if (dest.exists() && (dest.length() != src.length())) {
                        JDLogger.getLogger().warning("Error while copying file: new file has a diffent lenght! src[" + src.getAbsolutePath() + "," + src.length() + "] dest[" + dest.getAbsolutePath() + "," + dest.length() + "]");
                    }
                }
            }
        }

        if (moved) {
            JDLogger.getLogger().fine(src.getAbsolutePath() + " has been moved to " + dest.getAbsolutePath());
        } else {
            JDLogger.getLogger().warning("Couldn't move file " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
        }
        return moved;
    }

    /**
     * Moves a file with default options
     * 
     * @param src
     *            the source File
     * @param dest
     *            the destination File
     * @return {@code true} if and only if the file is successfully moved;
     *         {@code false} otherwise
     */
    public static boolean move(File src, File dest) {
        return move(src, dest, MoveOverwriteAllowedByDefault);
    }

    /*
     * Use this for testing Tested on: Windows Vista, 32bit
     * 
     * public static void main(String[] args) {
     * 
     * String a = "C:\\test.txt"; // only this file exists at the beginning of
     * // the test (and afterwards) String b = "C:\\test2.txt"; // same hard
     * drive as a String c = "K:\\test3.txt"; // different hard drive than a and
     * b
     * 
     * if (!new File(a).exists() || new File(b).exists() || new
     * File(c).exists()) {
     * JDLogger.getLogger().warning("only testfile a has to exist!"); return; }
     * 
     * // tests
     * 
     * JDLogger.getLogger().warning("Test Copy on same hd"); if (copy(new
     * File(a), new File(b), false)) {
     * 
     * JDLogger.getLogger().warning("Test Delete (Permanently)"); if (delete(new
     * File(a), FileUtils.DeletePermanently)) {
     * 
     * JDLogger.getLogger().warning("Test Move on same hd"); if (move(new
     * File(b), new File(a), false)) {
     * 
     * JDLogger.getLogger().warning("Test Copy to different hd"); if (copy(new
     * File(a), new File(c), false)) {
     * 
     * JDLogger.getLogger().warning("Test Delete (Recycle Bin)"); if (delete(new
     * File(a), FileUtils.MoveToRecycleBin)) {
     * 
     * JDLogger.getLogger().warning("Test Move to different hd"); if (move(new
     * File(c), new File(a), false)) {
     * JDLogger.getLogger().warning("Tests finished successfull!");
     * System.exit(0); } } } } } } JDLogger.getLogger().warning("Error!");
     * 
     * }
     * 
     * /*
     */

}