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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;

import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
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

    private ExtractionController controller;
    protected LogSource          logger;
    private ExtractionConfig     config;
    private Exception            exception;
    private ArchiveFile          lastAccessedArchiveFile;
    private ExtractLogFileWriter crashLog;

    private List<Matcher>        filters = new ArrayList<Matcher>();

    protected void initFilters() {
        final String[] patternStrings = getConfig().getBlacklistPatterns();
        final List<Matcher> filters = new ArrayList<Matcher>();
        if (patternStrings != null && patternStrings.length > 0) {
            for (final String patternString : patternStrings) {
                try {
                    if (StringUtils.isNotEmpty(patternString) && !patternString.startsWith("##")) {
                        filters.add(Pattern.compile(patternString).matcher(""));
                    }
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            }
        }
        this.filters = filters;
    }

    protected boolean isFiltered(final String path) {
        final String check = "/".concat(path);
        for (final Matcher regex : filters) {
            try {
                if (regex.reset(check).matches()) {
                    return true;
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        return false;
    }

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
     * Sets the {@link ExtractionController} which controlls the extraction.
     *
     * @param controller
     *            The {@link ExtractionController}.
     */
    protected final void setExtractionController(ExtractionController controller) {
        this.controller = controller;
    }

    protected final ExtractionController getExtractionController() {
        return this.controller;
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

    public final ExtractionConfig getConfig() {
        return config;
    }

    /**
     * The logger for the ouputs.
     *
     * @param logger
     */
    public final void setLogger(LogSource logger) {
        this.logger = logger;
    }

    public final LogSource getLogger() {
        return logger;
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
    public abstract Archive buildArchive(ArchiveFactory link, boolean allowDeepInspection) throws ArchiveException;

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
    public abstract boolean isAvailable(ExtractionExtension extractionExtension);

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

    public abstract Boolean isSupported(ArchiveFactory archiveFile, boolean allowDeepInspection);

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
     * Some extrators can set this file information. this helps us to tell the user in which file extraction failed.
     *
     * @return
     */
    public ArchiveFile getLastAccessedArchiveFile() {
        return lastAccessedArchiveFile;
    }

    public void writeCrashLog(String string) {
        final ExtractLogFileWriter crashLog = this.crashLog;
        if (crashLog != null) {
            crashLog.write(string);
        }
    }

    public void setCrashLog(ExtractLogFileWriter crashLog) {
        this.crashLog = crashLog;
    }

}