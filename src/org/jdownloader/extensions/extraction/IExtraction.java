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

import java.util.List;
import java.util.logging.Logger;

import jd.config.ConfigContainer;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.plugins.DownloadLink;

/**
 * Interface for the individual extraction programs.
 * 
 * @author botzi
 * 
 */
public interface IExtraction {
    /**
     * Builds an {@link Archive} with an finished {@link DownloadLink}. If the
     * {@link DownloadLink} contains only a part of an multipart archive, it has
     * not to be to first one (eg. test.part1.rar).
     * 
     * @param link
     *            An complete downloaded file.
     * @return An {@link Archive} that contains all {@link Downloadlink}s.
     */
    public Archive buildArchive(DownloadLink link);

    /**
     * Similar to the buildArchive, but contains only dummy {@link DownloadLink}
     * s. It's used to extract arcvhivs that are not in the downloadlist.
     * 
     * @param file
     *            Path the first archive.
     * @return An {@link Archive} that contains all dummy {@link Downloadlink}s.
     */
    public Archive buildDummyArchive(String file);

    /**
     * Checks a single password if the archive is encrypted with it.
     * 
     * @param password
     *            The password.
     * @return True if the password is correct.
     */
    public boolean findPassword(String password);

    /**
     * Starts the extraction of an {@link Archive}.
     */
    public void extract();

    /**
     * Checks if the extraction method can be used on the system. If it's not
     * possible the method should try to fix that. If that fails it should
     * return false. The extraction method will not be used anymore in this
     * session for extraction.
     * 
     * @return True if all works.
     */
    public boolean checkCommand();

    /**
     * Returns the percent of the tested passowords.
     * 
     * @return The percent of the tested passwords.
     */
    public int getCrackProgress();

    /**
     * Is used to let the extraction method prepare for the extraction. Will be
     * called after the system started an extraction, but before the
     * {@link crackPassword} and {@link extract}.
     * 
     * @return False if it's not possible to extract that archive.
     */
    public boolean prepare();

    /**
     * Sets a configuration.
     * 
     * @param config
     * @param subConfig
     */
    public void initConfig(ConfigContainer config, JSonWrapper subConfig);

    /**
     * Sets the {@link Archive} which should be extracted.
     * 
     * @param archive
     *            The {@link Archive}.
     */
    public void setArchiv(Archive archive);

    /**
     * Sets the {@link ExtractionController} which controlls the extraction.
     * 
     * @param controller
     *            The {@link ExtractionController}.
     */
    public void setExtractionController(ExtractionController controller);

    /**
     * Returns the archivename of an {@link Archive}.
     * 
     * @param archive
     *            The {@link Archive}.
     * @return The name of the archive.
     */
    public String getArchiveName(DownloadLink link);

    /**
     * Checks if the file is supported.
     * 
     * @param file
     *            The file which should be checked
     * @return
     */
    public boolean isArchivSupported(String file);

    /**
     * Checks if the file from the filefilter is supported.
     * 
     * @param file
     *            The file which should be checked.
     * @return
     */
    public boolean isArchivSupportedFileFilter(String file);

    /**
     * Sets the pluginconfiguration.
     * 
     * @param config
     *            The configuration.
     */
    public void setConfig(JSonWrapper config);

    /**
     * Ends the extraction.
     */
    public void close();

    /**
     * checks an Archive if any part is missing.
     * 
     * @param archive
     *            The archive.
     * @return A list of parts which are missing.
     */
    public List<String> checkComplete(Archive archive);

    /**
     * The logger for the ouputs.
     * 
     * @param logger
     */
    public void setLogger(Logger logger);
}