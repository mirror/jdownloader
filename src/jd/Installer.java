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

package jd;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.swing.dialog.InstallerDialog;
import jd.update.WebUpdater;

import org.appwork.storage.JacksonStorageChest;

/**
 * Der Installer erscheint nur beim ersten mal Starten der Webstartversion und
 * beim neuinstallieren der webstartversion der User kann Basiceinstellungen
 * festlegen
 * 
 * @author JD-Team
 */
public class Installer {

    private boolean aborted = false;

    public Installer() {
        if (!InstallerDialog.showDialog(null)) {
            JDLogger.getLogger().severe("downloaddir not set");
            this.aborted = true;
        } else {
            /* install not aborted */
            try {
                /* read default values from jddefaults */
                JacksonStorageChest defaults = new JacksonStorageChest("jddefaults", true);
                final SubConfiguration webConfig = SubConfiguration.getConfig("WEBUPDATE");
                webConfig.setProperty(WebUpdater.PARAM_BRANCH, defaults.get(WebUpdater.PARAM_BRANCH, null));
                webConfig.save();
            } catch (final Throwable e) {
                JDLogger.exception(e);
            }
        }
    }

    public boolean isAborted() {
        return this.aborted;
    }

}
