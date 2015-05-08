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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appwork.utils.Application;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.multi.ArchiveType;
import org.jdownloader.extensions.extraction.split.SplitType;

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
     * do not use this setter. if you feel like setting a password outside the extracting internals, use getSettings().setPasswords.. this
     * setter is used to set the CORRECT password in the password finding algorithm only
     *
     * @param password
     */
    public void setFinalPassword(String password) {
        getSettings().setFinalPassword(password);
    }

    /**
     * ArchiveFiles of the archive.
     */
    private List<ArchiveFile> archives;

    /**
     * Exitcode of the extrraction.
     */
    private int               exitCode    = -1;

    /**
     * Type of the archive.
     */
    private ArchiveType       archiveType = null;

    private SplitType         splitType   = null;

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    /**
     * ArchiveFiles CRC error.
     */
    private final List<ArchiveFile> crcError;

    /**
     * List of the extracted files.
     */
    private final List<File>        extractedFiles;

    private final List<File>        skippedFiles;

    private final ArchiveFactory    factory;

    private String                  name;

    private ContentView             contents;

    private boolean                 passwordRequiredToOpen;
    private String                  archiveID = null;

    public String getArchiveID() {
        return archiveID;
    }

    public void setArchiveID(String archiveID) {
        this.archiveID = archiveID;
    }

    public ArchiveFactory getFactory() {
        return factory;
    }

    public Archive(ArchiveFactory link) {
        factory = link;
        archives = new CopyOnWriteArrayList<ArchiveFile>();
        crcError = new CopyOnWriteArrayList<ArchiveFile>();
        extractedFiles = new CopyOnWriteArrayList<File>();
        skippedFiles = new CopyOnWriteArrayList<File>();
        contents = new ContentView();
    }

    public Archive getParentArchive() {
        return null;
    }

    public Archive getRootArchive() {
        if (getParentArchive() != null) {
            return getParentArchive().getRootArchive();
        }
        return this;
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
        try {
            return "Archive:" + getName();
        } catch (final Throwable e) {
            return e.getMessage();
        }
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public java.util.List<ArchiveFile> getArchiveFiles() {
        return archives;
    }

    public void setArchiveFiles(java.util.List<ArchiveFile> collection) {
        final List<ArchiveFile> archives = new CopyOnWriteArrayList<ArchiveFile>(collection);
        for (ArchiveFile af : archives) {
            af.setArchive(this);
        }
        this.archives = archives;
    }

    public void setArchiveType(ArchiveType singleFile) {
        this.archiveType = singleFile;
    }

    public ArchiveType getArchiveType() {
        return archiveType;
    }

    public void addCrcError(ArchiveFile crc) {
        this.crcError.add(crc);
    }

    public List<ArchiveFile> getCrcError() {
        return crcError;
    }

    public void addExtractedFiles(File file) {
        this.extractedFiles.add(file);
    }

    public List<File> getExtractedFiles() {
        return extractedFiles;
    }

    public List<File> getSkippedFiles() {
        return skippedFiles;
    }

    public void addSkippedFiles(File file) {
        this.skippedFiles.add(file);
    }

    public boolean contains(Object contains) {
        if (contains != null) {
            for (ArchiveFile file : getArchiveFiles()) {
                if (file.equals(contains)) {
                    return true;
                }
            }
        }
        return false;
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

    public ContentView getContentView() {
        return contents;
    }

    public void setContentView(ContentView view) {
        this.contents = view;
    }

    public ArchiveSettings getSettings() {
        return ArchiveController.getInstance().getArchiveSettings(this.getFactory());
    }

    public ArchiveFile getArchiveFileByPath(String filename) {
        if (filename != null) {
            for (ArchiveFile af : getArchiveFiles()) {
                if (filename.equals(af.getFilePath())) {
                    return af;
                }
            }
        }
        return null;
    }

    protected void onControllerAssigned(ExtractionController extractionController) {
    }

    protected void onStartExtracting() {
        crcError.clear();
        extractedFiles.clear();
        skippedFiles.clear();
        exitCode = -1;
        contents = new ContentView();
    }

    protected void onCleanUp() {
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
        if (getSettings().getAutoExtract() != booleanStatus) {
            getSettings().setAutoExtract(booleanStatus);
            notifyChanges(ArchiveSettings.AUTO_EXTRACT);
        }
    }

}