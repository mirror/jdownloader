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

import jd.plugins.DownloadLink;

import org.appwork.utils.Exceptions;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.extraction.multi.ArchiveException;
import org.jdownloader.extensions.extraction.multi.CheckException;

/**
 * Interface for the individual extraction programs.
 * 
 * @author botzi
 * 
 */
public abstract class IExtraction {

    protected Archive              archive;
    protected ExtractionController controller;
    protected LogSource            logger;
    protected ExtractionConfig     config;
    private Exception              exception;
    private ArchiveFile            lastAccessedArchiveFile;
    private CrashDetectFile        crashLog;

    public void setLastAccessedArchiveFile(ArchiveFile lastAccessedArchiveFile) {

        this.lastAccessedArchiveFile = lastAccessedArchiveFile;
        writeCrashLog("Extracting from: " + lastAccessedArchiveFile);
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
        writeCrashLog("Exception: \r\n" + Exceptions.getStackTrace(exception));
    }

    /**
     * Sets the {@link Archive} which should be extracted.
     * 
     * @param archive
     *            The {@link Archive}.
     */
    public final void setArchiv(Archive archive) {
        this.archive = archive;
    }

    /**
     * Sets the {@link ExtractionController} which controlls the extraction.
     * 
     * @param controller
     *            The {@link ExtractionController}.
     */
    public final void setExtractionController(ExtractionController controller) {
        this.controller = controller;
    }

    /**
     * Sets the pluginconfiguration.
     * 
     * @param extractionConfig
     *            The configuration.
     */
    public final void setConfig(ExtractionConfig extractionConfig) {
        this.config = extractionConfig;
    }

    /**
     * The logger for the ouputs.
     * 
     * @param logger
     */
    public final void setLogger(LogSource logger) {
        this.logger = logger;
    }

    /**
     * Builds an {@link Archive} with an finished {@link DownloadLink}. If the {@link DownloadLink} contains only a part of an multipart
     * archive, it has not to be to first one (eg. test.part1.rar).
     * 
     * @param link
     *            An complete downloaded file.
     * @return An {@link Archive} that contains all {@link Downloadlink}s.
     * @throws ArchiveException
     */
    public abstract Archive buildArchive(ArchiveFactory link) throws ArchiveException;

    /**
     * Checks a single password if the archive is encrypted with it.
     * 
     * @param password
     *            The password.
     * @param optimized
     *            TODO
     * 
     * @return True if the password is correct.
     */
    public abstract boolean findPassword(ExtractionController controller, String password, boolean optimized) throws ExtractionException;

    /**
     * Starts the extraction of an {@link Archive}.
     */
    public abstract void extract(ExtractionController controller);

    /**
     * Checks if the extraction method can be used on the system. If it's not possible the method should try to fix that. If that fails it
     * should return false. The extraction method will not be used anymore in this session for extraction.
     * 
     * @return True if all works.
     */
    public abstract boolean checkCommand();

    /**
     * Returns the percent of the tested passowords.
     * 
     * @return The percent of the tested passwords.
     */
    public abstract int getCrackProgress();

    /**
     * Is used to let the extraction method prepare for the extraction. Will be called after the system started an extraction, but before
     * the {@link crackPassword} and {@link extract}.
     * 
     * @return False if we need a password to extract
     */
    public abstract boolean prepare() throws ExtractionException;

    /**
     * Returns the archivename of an {@link Archive}.
     * 
     * @param archive
     *            The {@link Archive}.
     * @return The name of the archive.
     */
    public abstract String getArchiveName(ArchiveFactory factory);

    /**
     * Checks if the file is supported.
     * 
     * @param factory
     *            TODO
     * 
     * @return
     */
    public abstract boolean isArchivSupported(ArchiveFactory factory);

    //
    // /**
    // * Checks if the file from the filefilter is supported.
    // *
    // * @param file
    // * The file which should be checked.
    // * @return
    // */
    // public abstract boolean isArchivSupportedFileFilter(String file);

    /**
     * Ends the extraction.
     */
    public abstract void close();

    /**
     * checks an Archive if any part is missing.
     * 
     * @param archive
     *            The archive.
     * @return A list of parts which are missing.
     * @throws CheckException
     */
    public abstract DummyArchive checkComplete(Archive archive) throws CheckException;

    /**
     * tries to create an ID for the archive the given filename belongs to.
     * 
     * @param factory
     * @return
     */
    public abstract String createID(ArchiveFactory factory);

    public abstract boolean isMultiPartArchive(ArchiveFactory factory);

    /**
     * Some extrators can set this file information. this helps us to tell the user in which file extraction failed.
     * 
     * @return
     */
    public ArchiveFile getLastAccessedArchiveFile() {
        return lastAccessedArchiveFile;
    }

    public void writeCrashLog(String string) {
        if (crashLog != null) {
            crashLog.write(string);
        }
    }

    public void setCrashLog(CrashDetectFile crashLog) {
        this.crashLog = crashLog;
    }

}