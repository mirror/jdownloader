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

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.extraction.multi.ArchiveType;
import org.jdownloader.extensions.extraction.multi.CheckException;

/**
 * Contains information about the archivefile.
 * 
 * @author botzi
 * 
 */
public class Archive {

    /**
     * Extractionpath
     */
    private File                   extractTo;

    /**
     * Encrypted archive
     */
    private boolean                protect          = false;

    /**
     * ArchiveFiles of the archive.
     */
    private ArrayList<ArchiveFile> archives;

    /**
     * First part of the archives.
     */
    private ArchiveFile            firstArchiveFile = null;

    /**
     * Overwrite existing files.
     */
    private boolean                overwriteFiles   = false;

    /**
     * Password for the archive.
     */
    private String                 password         = "";

    /**
     * Exitcode of the extrraction.
     */
    private int                    exitCode         = -1;

    /**
     * Extractionprocress got interrupted
     */
    private boolean                gotInterrupted   = false;

    /**
     * Total size of the extracted ffiles.
     */
    private long                   size             = 0;

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
     * Number of files in the archive.
     */
    private int                    numberOfFiles    = 0;

    /**
     * ArchiveFiles CRC error.
     */
    private ArrayList<ArchiveFile> crcError;

    /**
     * List of the extracted files.
     */
    private ArrayList<File>        extractedFiles;

    /**
     * Indicates that the archive has no folders.
     */
    private boolean                noFolder         = true;

    /**
     * The extractor for the archive.
     */
    private IExtraction            extractor;
    private ArchiveFactory         factory;

    private String                 name;

    public ArchiveFactory getFactory() {
        return factory;
    }

    public Archive(ArchiveFactory link) {
        factory = link;
        archives = new ArrayList<ArchiveFile>();
        crcError = new ArrayList<ArchiveFile>();
        extractedFiles = new ArrayList<File>();
    }

    public boolean isProtected() {
        return protect;
    }

    public void setProtected(final boolean b) {
        this.protect = b;
    }

    public boolean isOverwriteFiles() {
        return overwriteFiles;
    }

    public void setOverwriteFiles(boolean overwriteFiles) {
        this.overwriteFiles = overwriteFiles;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null) {
            password = "";
        }
        this.password = password;
    }

    public File getExtractTo() {
        return extractTo;
    }

    public void setExtractTo(File extractTo) {

        this.extractTo = extractTo;
        getFactory().fireExtractToChange(this);
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

    public void setSize(final long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public void setFirstArchiveFile(ArchiveFile firstArchiveFile) {
        this.firstArchiveFile = firstArchiveFile;
    }

    public ArchiveFile getFirstArchiveFile() {
        return firstArchiveFile;
    }

    /**
     * Returns how much bytes got extracted. this is NOT getSize() after
     * extracting in some cases. Because files may be filtered, or not extracted
     * due to overwrite rules. user {@link ExtractionController#getProgress()}
     * to get the extraction progress
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

    public void setNumberOfFiles(int numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
    }

    public int getNumberOfFiles() {
        return numberOfFiles;
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

    public void setNoFolder(boolean noFolder) {
        this.noFolder = noFolder;
    }

    public boolean isNoFolder() {
        return noFolder;
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
            Log.exception(e);
        }
        return false;
    }

    public File getFolder() {
        return getFactory().getFolder();
    }

}