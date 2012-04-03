//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.controlling;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

import jd.plugins.DownloadLink;

import org.jdownloader.dlc.DLCFactory;

/**
 * Im JDController wird das ganze App gesteuert. Events werden deligiert.
 * 
 * @author JD-Team/astaldo
 */
public class JDController {

    public static JDController getInstance() {
        return INSTANCE;
    }

    /**
     * Der Logger
     */
    private static final Logger LOGGER       = JDLogger.getLogger();

    /**
     * Der Download Watchdog verwaltet die Downloads
     */

    private static JDController INSTANCE     = new JDController();

    private static final Object SHUTDOWNLOCK = new Object();

    public ArrayList<DownloadLink> getContainerLinks(File file) {
        return new DLCFactory().getContainerLinks(file);

    }

}