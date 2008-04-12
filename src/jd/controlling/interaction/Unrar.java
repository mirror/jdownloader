//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;

import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.unrar.JUnrar;
import jd.unrar.Merge;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse fürs automatische Entpacken
 * 
 * @author DwD
 */
public class Unrar extends Interaction implements Serializable {
    private static Unrar INSTANCE = null;

    private DownloadLink lastFinishedDownload = null;

    // DIese Interaction sollte nur einmal exisitieren und wird deshalb über
    // eine factory aufgerufen.
    private Unrar() {
        super();
    }

    public static Unrar getInstance() {
        if (INSTANCE == null) {
            if (JDUtilities.getConfiguration().getProperty(Configuration.PARAM_UNRAR_INSTANCE, null) != null) {
                INSTANCE = (Unrar) JDUtilities.getConfiguration().getProperty(Configuration.PARAM_UNRAR_INSTANCE, null);
                return INSTANCE;
            }
            INSTANCE = new Unrar();
        }
        return INSTANCE;

    }

    /**
     * 
     */
    private static final long serialVersionUID = 2467582501274722811L;

    /**
     * serialVersionUID
     */
    private static boolean IS_RUNNING = false;

    private static final String NAME = JDLocale.L("interaction.unrar.name");

    public static final String PROPERTY_UNRARCOMMAND = "UNRAR_PROPERTY_UNRARCMD";

    public static final String PROPERTY_AUTODELETE = "UNRAR_PROPERTY_AUTODELETE";

    public static final String PROPERTY_OVERWRITE_FILES = "UNRAR_PROPERTY_OVERWRITE_FILES";

    public static final String PROPERTY_MAX_FILESIZE = "UNRAR_PROPERTY_MAX_FILESIZE";

    public static final String PROPERTY_ENABLED = "UNRAR_PROPERTY_ENABLED";

    public static final String PROPERTY_WAIT_FOR_TERMINATION = "UNRAR_WAIT_FOR_TERMINATION";

    public static final String PROPERTY_ENABLE_EXTRACTFOLDER = "UNRAR_PROPERTY_ENABLE_EXTRACTFOLDER";

    public static final String PROPERTY_EXTRACTFOLDER = "UNRAR_PROPERTY_EXTRACTFOLDER";

    public static final String PROPERTY_DELETE_INFOFILE = "PROPERTY_DELETE_INFOFILE";

    public static final String PROPERTY_USE_HJMERGE = "PROPERTY_USE_HJMERGE";

    public static final String PROPERTY_DELETE_MERGEDFILES = "PROPERTY_DELETE_MERGEDFILES";

    @Override
    public boolean doInteraction(Object arg) {
        lastFinishedDownload = (DownloadLink) arg;
        start();
        return true;

    }

    public boolean getWaitForTermination() {
        return JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_WAIT_FOR_TERMINATION, false);
    }

    @Override
    public String toString() {
        return JDLocale.L("interaction.unrar.name");
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    private JUnrar getUnrar() {
        JUnrar unrar;

        if (lastFinishedDownload != null) {
            logger.info("LastFinishedFile:" + lastFinishedDownload);
            String password = lastFinishedDownload.getSourcePluginPassword();
            if (password != null && password.matches("[\\s]*")) password = null;
            if (password == null) {
                password = lastFinishedDownload.getFilePackage().getPassword();
                if (password != null && password.matches("[\\s]*")) password = null;

            }
            unrar = new JUnrar(new File(lastFinishedDownload.getFileOutput()), password);
            unrar.useToextractlist = true;
        } else {
            unrar = new JUnrar();
            unrar.useToextractlist = false;
        }

        if (JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_ENABLE_EXTRACTFOLDER, false)) {
            String efolder = JDUtilities.getConfiguration().getStringProperty(Unrar.PROPERTY_EXTRACTFOLDER, null);
            if (efolder != null) unrar.extractFolder = new File(efolder);
        }

        unrar.overwriteFiles = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_OVERWRITE_FILES, false);
        unrar.deleteInfoFile = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_DELETE_INFOFILE, true);
        unrar.autoDelete = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_AUTODELETE, true);
        unrar.unrar = JDUtilities.getConfiguration().getStringProperty(Unrar.PROPERTY_UNRARCOMMAND);
        return unrar;
    }

    @Override
    public void run() {
        int c = 0;
        if (IS_RUNNING) logger.warning("Process is in queue, there is already an unrar process running");
        while (IS_RUNNING) {
            if (c++ == 1200) IS_RUNNING = false;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        IS_RUNNING = true;
        JUnrar unrar = getUnrar();
        if (lastFinishedDownload == null) {
            LinkedList<String> folders = new LinkedList<String>();
            // wird nur befoetigt wenn PARAM_USE_PACKETNAME_AS_SUBFOLDER
            // auch
            // true ist
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false)) {
                Iterator<DownloadLink> iter = JDUtilities.getController().getFinishedLinks().iterator();
                while (iter.hasNext()) {
                    DownloadLink element = (DownloadLink) iter.next();
                    logger.info("finished File: " + element.getFileOutput());
                    File folder = new File(element.getFileOutput()).getParentFile();
                    logger.info("Folder: " + folder);
                    if (folder.exists()) {
                        if (folders.indexOf(folder.getAbsolutePath()) == -1) {
                            logger.info("Add unrardir: " + folder.getAbsolutePath());
                            folders.add(folder.getAbsolutePath());
                        }
                    }
                }
            }
            folders.add(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY));
            logger.info("dirs: " + folders);
            unrar.setFolders(folders);
            // Entpacken bis nichts mehr gefunden wurde (wird jetzt von
            // unrar
            // erledigt indem entpackte dateien nochmal
            // durch unrar geschickt werden
            /*
             * if (newFolderList != null && newFolderList.size() > 0) { unrar =
             * getUnrar(); unrar.setFolders(newFolderList); newFolderList =
             * unrar.unrar(); } if (newFolderList != null &&
             * newFolderList.size() > 0) { unrar = getUnrar();
             * unrar.setFolders(newFolderList); newFolderList = unrar.unrar(); }
             */
        }
        LinkedList<String> followingFiles = new LinkedList<String>();
        Iterator<DownloadLink> ff = JDUtilities.getController().getDownloadLinks().iterator();
        while (ff.hasNext()) {
            DownloadLink dl = ff.next();
            if (dl.getStatus() != DownloadLink.STATUS_DONE && dl.getStatus() != DownloadLink.STATUS_DONE) {
                followingFiles.add(dl.getName());
            }
        }
        unrar.followingFiles = followingFiles.toArray(new String[followingFiles.size()]);

        unrar.unrar();
        if (lastFinishedDownload != null && JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_USE_HJMERGE, true)) {
            Merge.mergeIt(new File(lastFinishedDownload.getFileOutput()), unrar.followingFiles, JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_DELETE_MERGEDFILES, true));
        }
        IS_RUNNING = false;
        this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        Interaction.handleInteraction(Interaction.INTERACTION_AFTER_UNRAR, null);
    }

    @Override
    public void initConfig() {

    }

    @Override
    public void resetInteraction() {
    }
}