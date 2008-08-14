//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.controlling.interaction;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import jd.config.Configuration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.update.PackageData;
import jd.update.WebUpdater;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse führt ein Webupdate durch
 * 
 * @author JD-Team
 */
public class WebUpdate extends Interaction implements Serializable {
    /**
     * serialVersionUID
     */
    private static final String NAME = JDLocale.L("interaction.webupdate.name", "WebUpdate");

    /**
     * 
     */
    private static final long serialVersionUID = 5345996658356704386L;

    private WebUpdater updater;

    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting WebUpdate");
        updater = new WebUpdater(null);
        start();
        return true;
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    /**
     * Gibt den verwendeten Updater zurück
     * 
     * @return updater
     */
    public WebUpdater getUpdater() {
        return updater;
    }

    @Override
    public void initConfig() {
    }

    @Override
    public void resetInteraction() {
    }

    /**
     * Der eigentlich UpdaterVorgang läuft in einem eigenem Thread ab
     */
    @Override
    public void run() {
        ProgressController progress = new ProgressController("Webupdater", 5);
        Vector<Vector<String>> files = updater.getAvailableFiles();
     PackageManager pm = new PackageManager();
ArrayList<PackageData> packages = pm.getDownloadedPackages();
        if ((files == null || files.size() == 0) && packages.size() == 0) { return; }
        int org;
        progress.setRange(org = files.size());
        progress.setStatusText(JDLocale.L("interaction.webupdate.progress.updateCheck", "Update Check"));

        if (files != null || packages.size() > 0) {

            updater.filterAvailableUpdates(files, JDUtilities.getResourceFile("."));

            progress.setStatus(org - files.size());
            if (files.size() > 0 || packages.size() > 0) {
                logger.info("New Updates Available! " + files);
                logger.info("New Packages to install: " + packages.size());
                Browser.download(JDUtilities.getResourceFile("webupdater.jar"), "http://jdownloaderwebupdate.ath.cx");

                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, true)) {
                    // Eine checkfile schreiben. Diese CheckFile wird vom
                    // webupdater gelöscht. Wird sie beim restart von JD
                    // wiedergefunden wird eine warnmeldung angezeigt, weild as
                    // darauf hindeutet dass der webupdater fehlerhaft
                    // funktioniert hat
                    JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                    logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, false) ? "/all" : "", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                    System.exit(0);
                } else {
                    if (JDUtilities.getController().getUiInterface().showConfirmDialog(packages.size() + files.size() + " update(s) available. Start Webupdater now?")) {
                        JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                        logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, false) ? "/all" : "", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                        System.exit(0);
                    }

                }

            }

        }

        setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        progress.finalize();
        logger.info(updater.getLogger().toString());
        // updater.run();
        // if (updater.getUpdatedFiles() > 0) {
        // this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        // }
        // else {
        // this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
        // }
    }

    @Override
    public String toString() {
        return JDLocale.L("interaction.webupdate.toString", "WebUpdate durchführen");
    }
}
