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
import java.util.ArrayList;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.multi.ArchiveType;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.logging.LogController;

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
    private ArrayList<ArchiveFile> archives;

    /**
     * First part of the archives.
     */
    private ArchiveFile            firstArchiveFile = null;

    /**
     * Exitcode of the extrraction.
     */
    private int                    exitCode         = -1;

    /**
     * Extractionprocress got interrupted
     */
    private boolean                gotInterrupted   = false;

    /**
     * Size of the corrent extracted files.
     */
    private long                   extracted        = 0;

    /**
     * Is extraction process active.
     */
    private boolean                active           = false;

    /**
     * Type of the archive.
     */
    private ArchiveType            type             = null;

    /**
     * ArchiveFiles CRC error.
     */
    private ArrayList<ArchiveFile> crcError;

    /**
     * List of the extracted files.
     */
    private ArrayList<File>        extractedFiles;

    /**
     * The extractor for the archive.
     */
    private IExtraction            extractor;
    private ArchiveFactory         factory;

    private String                 name;

    private ContentView            contents;

    private ArchiveSettings        settings;

    public ArchiveFactory getFactory() {
        return factory;
    }

    public Archive(ArchiveFactory link) {
        factory = link;
        archives = new ArrayList<ArchiveFile>();
        crcError = new ArrayList<ArchiveFile>();
        extractedFiles = new ArrayList<File>();
        contents = new ContentView();
    }

    public boolean isProtected() {
        return protect;
    }

    public void setProtected(final boolean b) {
        this.protect = b;
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

    public ArrayList<ArchiveFile> getArchiveFiles() {
        return archives;
    }

    public void setArchiveFiles(ArrayList<ArchiveFile> collection) {

        this.archives = collection;
    }

    public void setGotInterrupted(final boolean gotInterrupted) {
        this.gotInterrupted = gotInterrupted;
    }

    public boolean getGotInterrupted() {
        return gotInterrupted;
    }

    public void setFirstArchiveFile(ArchiveFile firstArchiveFile) {
        this.firstArchiveFile = firstArchiveFile;
    }

    public ArchiveFile getFirstArchiveFile() {
        return firstArchiveFile;
    }

    /**
     * Returns how much bytes got extracted. this is NOT getSize() after extracting in some cases. Because files may be filtered, or not
     * extracted due to overwrite rules. user {@link ExtractionController#getProgress()} to get the extraction progress
     * 
     * @return
     */

    public long getExtractedFilesSize() {
        return extracted;
    }

    public DummyArchive createDummyArchive() throws CheckException {
        return extractor.checkComplete(this);
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

    public ArrayList<ArchiveFile> getCrcError() {
        return crcError;
    }

    public void addExtractedFiles(File file) {
        this.extractedFiles.add(file);
        extracted += file.length();
    }

    public ArrayList<File> getExtractedFiles() {
        return extractedFiles;
    }

    public void setExtractor(IExtraction extractor) {
        this.extractor = extractor;
    }

    public IExtraction getExtractor() {

        return extractor;
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

    public boolean isComplete() {
        try {
            return createDummyArchive().isComplete();
        } catch (CheckException e) {
            LogController.CL().log(e);
        }
        return false;
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
        if (settings != null) return settings;
        synchronized (this) {
            if (settings != null) return settings;
            Application.getResource("cfg/archives/").mkdirs();
            settings = JsonConfig.create(Application.getResource("cfg/archives/" + getFactory().getID()), ArchiveSettings.class);

        }
        return settings;
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

}