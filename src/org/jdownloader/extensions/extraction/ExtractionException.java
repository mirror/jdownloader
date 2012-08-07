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

/**
 * Exception for the ExtractionWrapper.
 * 
 * @author botzi
 * 
 */
public class ExtractionException extends Exception {
    private static final long serialVersionUID = -5064334224764462157L;
    private ArchiveFile       latestAccessedArchiveFile;

    public ExtractionException(String string) {
        super(string);
    }

    public ExtractionException(Throwable e, ArchiveFile archiveFile) {
        super(archiveFile == null ? e.getMessage() : "Bad Archive: " + archiveFile.getName(), e);
        latestAccessedArchiveFile = archiveFile;

    }

    public ArchiveFile getLatestAccessedArchiveFile() {
        return latestAccessedArchiveFile;
    }
}