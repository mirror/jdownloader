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

package jd.plugins.optional.extraction;

import java.io.File;
import java.util.ArrayList;

import jd.plugins.DownloadLink;

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
    public static final int SINGLE_FILE = 0;
    /**
     * Is a 7zip or HJSplit multipar archive.
     */
    public static final int MULTI = 1;
    /**
     * Is a multipart rar archive.
     */
    public static final int MULTI_RAR = 2;
    
    /**
     * Extractionpath
     */
    private File extractTo;

    /**
     * Encrypted archive
     */
    private boolean protect = false;
    
    /**
     * DownloadLinks of the archive.
     */
    private ArrayList<DownloadLink> archives;
    
    /**
     * First part of the archives.
     */
    private DownloadLink firstDownloadLink;
    
    /**
     * Overwrite existing files.
     */
    private boolean overwriteFiles = false;
    
    /**
     * Password for the archive.
     */
    private String password = "";
    
    /**
     * Exitcode of the extrraction.
     */
    private int exitCode;
    
    /**
     * Status of the extraction.
     */
    private int status = -1;
    
    /**
     * Extractionprocress got interrupted
     */
    private boolean gotInterrupted = false;
    
    /**
     * Total size of the extracted ffiles.
     */
    private long size = 0;
    
    /**
     * Size of the corrent extracted files.
     */
    private long extracted = 0;
    
    /**
     * Is extraction process active.
     */
    private boolean active = false;
    
    /**
     * Type of the archive.
     */
    private int type = 0;
    
    /**
     * Number of files in the archive.
     */
    private int numberOfFiles = 0;
    
    /**
     * Downloadlinks CRC error.
     */
    private ArrayList<DownloadLink> crcError;
    
    /**
     * List of the extracted files.
     */
    private ArrayList<File> extractedFiles;
    
    public Archive() {
        archives = new ArrayList<DownloadLink>();
        crcError = new ArrayList<DownloadLink>();
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
        this.password = password;
    }
    
    public File getExtractTo() {
        return extractTo;
    }
    
    public void setExtractTo(File extractTo) {
        this.extractTo = extractTo;
    }
    
    public int getExitCode() {
        return exitCode;
    }
    
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public ArrayList<DownloadLink> getDownloadLinks() {
        return archives;
    }
    
    public void setDownloadLinks(ArrayList<DownloadLink> archives) {
        this.archives = archives;
    }
    
    public void addDownloadLink(DownloadLink link) {
        archives.add(link);
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

    public void setFirstDownloadLink(DownloadLink firstDownloadLink) {
        this.firstDownloadLink = firstDownloadLink;
    }

    public DownloadLink getFirstDownloadLink() {
        return firstDownloadLink;
    }

    public void setExtracted(long extracted) {
        this.extracted = extracted;
    }

    public long getExtracted() {
        return extracted;
    }
    
    public boolean isComplete() {
       for(DownloadLink l: archives) {
           if(!l.getLinkStatus().isFinished()) { //&& !l.getFilePackage().isPostProcessing()) {
               return false;
           }
       }
       
       return true;
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

    public void addCrcError(DownloadLink crc) {
        this.crcError.add(crc);
    }

    public ArrayList<DownloadLink> getCrcError() {
        return crcError;
    }

    public void addExtractedFiles(File file) {
        this.extractedFiles.add(file);
    }

    public ArrayList<File> getExtractedFiles() {
        return extractedFiles;
    }
}