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

package org.jdownloader.extensions.extraction;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appwork.utils.Application;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.extraction.multi.ArchiveType;

/**
 * Contains information about the archivefile.
 * 
 * @author botzi
 * 
 */
public class Archive {

    /**
     * Encrypted archive
     */
    private boolean protect = false;

    /**
     * returns null or the correct password
     * 
     * @return
     */
    public String getFinalPassword() {
        return getSettings().getFinalPassword();
    }

    /**
     * do not use this setter. if you feel like setting a password outside the extracting internals, use getSettings().setPasswords.. this setter is used to set
     * the CORRECT password in the password finding algorithm only
     * 
     * @param password
     */
    public void setFinalPassword(String password) {
        getSettings().setFinalPassword(password);
    }

    /**
     * ArchiveFiles of the archive.
     */
    private java.util.List<ArchiveFile> archives;

    /**
     * First part of the archives.
     */
    private ArchiveFile                 firstArchiveFile = null;

    /**
     * Exitcode of the extrraction.
     */
    private int                         exitCode         = -1;

    /**
     * Extractionprocress got interrupted
     */
    private boolean                     gotInterrupted   = false;

    /**
     * Size of the corrent extracted files.
     */
    private long                        extracted        = 0;

    /**
     * Is extraction process active.
     */
    private boolean                     active           = false;

    /**
     * Type of the archive.
     */
    private ArchiveType                 type             = null;

    /**
     * ArchiveFiles CRC error.
     */
    private java.util.List<ArchiveFile> crcError;

    /**
     * List of the extracted files.
     */
    private java.util.List<File>        extractedFiles;

    private ArchiveFactory              factory;

    private String                      name;

    private ContentView                 contents;

    private boolean                     passwordRequiredToOpen;

    public ArchiveFactory getFactory() {
        return factory;
    }

    public Archive(ArchiveFactory link) {
        factory = link;
        archives = new CopyOnWriteArrayList<ArchiveFile>();
        crcError = new CopyOnWriteArrayList<ArchiveFile>();
        extractedFiles = new CopyOnWriteArrayList<File>();
        contents = new ContentView();
    }

    public boolean isProtected() {
        return protect;
    }

    public void setProtected(final boolean b) {
        this.protect = b;
    }

    public boolean isPasswordRequiredToOpen() {
        return passwordRequiredToOpen;
    }

    public void setPasswordRequiredToOpen(final boolean b) {
        this.passwordRequiredToOpen = b;
    }

    public String toString() {
        if (getFirstArchiveFile() == null) return "Incomplete Archive";
        return "Archive " + getFirstArchiveFile().getFilePath();
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public java.util.List<ArchiveFile> getArchiveFiles() {
        return Collections.unmodifiableList(archives);
    }

    public void setArchiveFiles(java.util.List<ArchiveFile> collection) {
        this.archives = collection;
        for (ArchiveFile af : archives) {
            af.setArchive(this);

        }
    }

    public void setGotInterrupted(final boolean gotInterrupted) {
        this.gotInterrupted = gotInterrupted;
    }

    public boolean getGotInterrupted() {
        return gotInterrupted;
    }

    public void setFirstArchiveFile(ArchiveFile firstArchiveFile) {
        if (this.firstArchiveFile != null) throw new IllegalStateException("firstArchiveFile is already set!");
        this.firstArchiveFile = firstArchiveFile;
    }

    public ArchiveFile getFirstArchiveFile() {
        return firstArchiveFile;
    }

    /**
     * Returns how much bytes got extracted. this is NOT getSize() after extracting in some cases. Because files may be filtered, or not extracted due to
     * overwrite rules. user {@link ExtractionController#getProgress()} to get the extraction progress
     * 
     * @return
     */

    public long getExtractedFilesSize() {
        return extracted;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void setType(ArchiveType singleFile) {
        this.type = singleFile;
    }

    public ArchiveType getType() {
        return type;
    }

    public void addCrcError(ArchiveFile crc) {
        this.crcError.add(crc);
    }

    public java.util.List<ArchiveFile> getCrcError() {
        return crcError;
    }

    public void addExtractedFiles(File file) {

        if (this.extractedFiles.add(file)) {
            extracted += file.length();
        }

    }

    public java.util.List<File> getExtractedFiles() {
        return extractedFiles;
    }

    public boolean contains(ArchiveFile link) {
        return getArchiveFiles().contains(link);
    }

    public String getName() {
        return name;
    }

    public void setName(String archiveName) {
        this.name = archiveName;
    }

    public File getFolder() {
        return getFactory().getFolder();
    }

    public synchronized ContentView getContentView() {
        if (contents == null || (contents.getTotalFileCount() + contents.getTotalFolderCount() == 0)) {

            java.util.List<ArchiveItem> files = getSettings().getArchiveItems();
            if (files != null && files.size() > 0) {
                ContentView newView = new ContentView();
                for (ArchiveItem item : files) {
                    if (item.getPath().trim().equals("")) continue;
                    newView.add(new PackedFile(item.isFolder(), item.getPath(), item.getSize()));

                }
                contents = newView;
            }

        }
        return contents;
    }

    public void setContentView(ContentView view) {
        this.contents = view;
    }

    public ArchiveSettings getSettings() {
        return ArchiveController.getInstance().getArchiveSettings(this.getFactory());
    }

    public ArchiveFile getArchiveFileByPath(String filename) {
        for (ArchiveFile af : archives) {
            if (filename.equals(af.getFilePath())) { return af; }
        }
        return null;
    }

    public void onControllerAssigned(ExtractionController extractionController) {
    }

    public void onStartExtracting() {
    }

    public void onCleanUp() {
    }

    public void setStatus(ExtractionController controller, ExtractionStatus status) {
        for (ArchiveFile link : getArchiveFiles()) {
            if (link == null) continue;
            link.setStatus(controller, status);
        }
    }

    public File getExtractLogFile() {
        return getArchiveLogFileById(getFactory().getID());
    }

    public static File getArchiveLogFileById(String id) {
        return Application.getResource("logs/extracting/" + id + ".txt");
    }

    public void setPasswords(List<String> list) {
        getSettings().setPasswords(list);
        notifyChanges(ArchiveSettings.PASSWORD);
    }

    private void notifyChanges(Object identifier) {
        for (ArchiveFile af : getArchiveFiles()) {
            af.notifyChanges(identifier);

        }
    }

    public void setAutoExtract(BooleanStatus booleanStatus) {
        if (getSettings().getAutoExtract() == booleanStatus) return;

        getSettings().setAutoExtract(booleanStatus);
        notifyChanges(ArchiveSettings.AUTO_EXTRACT);
    }

    public Archive getPreviousArchive() {
        return null;
    }

}