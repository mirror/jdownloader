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
import java.util.List;

import org.appwork.utils.logging.Log;

/**
 * Contains information about the archivefile.
 * 
 * @author botzi
 * 
 */
public class Archive {
    /**
     * Is a single file archive.
     */
    public static final int        SINGLE_FILE      = 0;
    /**
     * Is a 7zip or HJSplit multipar archive.
     */
    public static final int        MULTI            = 1;
    /**
     * Is a multipart rar archive.
     */
    public static final int        MULTI_RAR        = 2;

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
    private int                    type             = 0;

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
    private List<String>           missing;
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

    public void setExtracted(long extracted) {
        this.extracted = extracted;
    }

    public long getExtracted() {
        return extracted;
    }

    public boolean isComplete() {
        missing = new ArrayList<String>();

        for (ArchiveFile l : archives) {

            if (!l.isComplete()) {
                missing.add(l.getFilePath());

            }
        }
        List<String> ms = extractor.checkComplete(this);
        if (ms != null) this.missing.addAll(ms);
        if (missing.size() > 0) {
            for (String entry : missing) {
                Log.L.info("Missing archive file: " + entry);
            }
            return false;
        }

        return true;
    }

    public List<String> getMissing() {
        return missing;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
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
}