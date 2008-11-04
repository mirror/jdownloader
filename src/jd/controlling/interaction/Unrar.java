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
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.unrar.JUnrar;
import jd.unrar.hjsplitt.Merge;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

@Deprecated
class mergeFile extends File {

    private static final long serialVersionUID = 1L;

    public mergeFile(String pathname) {
        super(pathname);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof File) {
            File file = (File) obj;
            if (!file.getParentFile().equals(getParentFile())) { return false; }
            String oName = file.getName(), name = getName();
            String matcher = name.matches(".*\\.\\a.$") ? name.replaceFirst("\\.a.$", "") : name.replaceFirst("\\.[\\d]+($|\\..*)", "");
            String oMatcher = oName.matches(".*\\.\\a.$") ? oName.replaceFirst("\\.a.$", "") : oName.replaceFirst("\\.[\\d]+($|\\..*)", "");
            return matcher.equalsIgnoreCase(oMatcher);

        }
        return false;
    }

}

/**
 * Diese Klasse fürs automatische Entpacken
 * 
 * @author DwD
 */
@Deprecated
public class Unrar extends Interaction implements Serializable {
    /**
     * soll nicht mitserialisiert werden fals sich die instanz aufhängt
     */
    private transient static Unrar INSTANCE = null;
    private transient static boolean IS_RUNNING = false;
    /**
     * serialVersionUID
     */

    private transient static final String NAME = JDLocale.L("interaction.unrar.name", "Extractor");

    public transient static final String PROPERTY_UNRARCOMMAND = "UNRAR_PROPERTY_UNRARCMD", PROPERTY_AUTODELETE = "UNRAR_PROPERTY_AUTODELETE", PROPERTY_OVERWRITE_FILES = "UNRAR_PROPERTY_OVERWRITE_FILES", PROPERTY_ENABLED = "UNRAR_PROPERTY_ENABLED", PROPERTY_WAIT_FOR_TERMINATION = "UNRAR_WAIT_FOR_TERMINATION", PROPERTY_ENABLE_EXTRACTFOLDER = "UNRAR_PROPERTY_ENABLE_EXTRACTFOLDER", PROPERTY_EXTRACTFOLDER = "UNRAR_PROPERTY_EXTRACTFOLDER", PROPERTY_DELETE_INFOFILE = "PROPERTY_DELETE_INFOFILE", PROPERTY_USE_HJMERGE = "PROPERTY_USE_HJMERGE", PROPERTY_DELETE_MERGEDFILES = "PROPERTY_DELETE_MERGEDFILES", PROPERTY_AUTOFOLDER_COUNT = "PROPERTY_AUTOFOLDER_COUNT";

    /**
     * 
     */
    private static final long serialVersionUID = 2467582501274722811L;

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

    private transient DownloadLink lastFinishedDownload = null;

    // DIese Interaction sollte nur einmal exisitieren und wird deshalb über
    // eine factory aufgerufen.
    private Unrar() {
        super();
    }

    @Override
    public boolean doInteraction(Object arg) {
        lastFinishedDownload = (DownloadLink) arg;
        start();
        return true;

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
            if (password != null && password.matches("[\\s]*")) {
                password = null;
            }
            if (password == null) {
                password = lastFinishedDownload.getFilePackage().getPassword();
                if (password != null && password.matches("[\\s]*")) {
                    password = null;
                }

            }
            unrar = new JUnrar(new File(lastFinishedDownload.getFileOutput()), password);
            unrar.useToextractlist = true;
            Vector<DownloadLink> links = lastFinishedDownload.getFilePackage().getDownloadLinks();
            if (!links.contains(lastFinishedDownload)) {
                links.add(lastFinishedDownload);
            }
            for (int i = 0; i < links.size(); i++) {
                unrar.link += links.get(i).getDownloadURL() + "\r\n";
            }

        } else {
            unrar = new JUnrar();
            unrar.useToextractlist = false;
        }

        unrar.autoFolder = JDUtilities.getConfiguration().getIntegerProperty(PROPERTY_AUTOFOLDER_COUNT, 2);

        if (JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_ENABLE_EXTRACTFOLDER, false)) {
            String efolder = JDUtilities.getConfiguration().getStringProperty(Unrar.PROPERTY_EXTRACTFOLDER, null);
            if (efolder != null) {
                unrar.extractFolder = new File(efolder);
            }
        }

        unrar.overwriteFiles = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_OVERWRITE_FILES, false);
        unrar.deleteInfoFile = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_DELETE_INFOFILE, true);
        unrar.autoDelete = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_AUTODELETE, true);
        unrar.unrar = JDUtilities.getConfiguration().getStringProperty(Unrar.PROPERTY_UNRARCOMMAND);
        return unrar;
    }

    @Override
    public boolean getWaitForTermination() {
        return JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_WAIT_FOR_TERMINATION, false);
    }

    @Override
    public void initConfig() {

    }

    @Override
    public void resetInteraction() {
    }

    @Override
    public void run() {
        int c = 0;
        if (IS_RUNNING) {
            logger.warning("Process is in queue, there is already an unrar process running");
        }
        while (IS_RUNNING) {
            if (c++ == 1200) {
                IS_RUNNING = false;
            }
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
                    DownloadLink element = iter.next();
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
            folders.add(JDUtilities.getConfiguration().getDefaultDownloadDirectory());
            logger.info("dirs: " + folders);
            unrar.setFolders(folders);
        }
        LinkedList<String> followingFiles = new LinkedList<String>();
        Iterator<DownloadLink> ff = JDUtilities.getController().getDownloadLinks().iterator();
        while (ff.hasNext()) {
            DownloadLink dl = ff.next();
            if (!dl.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                followingFiles.add(dl.getName());
            }
        }
        unrar.followingFiles = followingFiles.toArray(new String[followingFiles.size()]);

        LinkedList<File> unpacked = unrar.unrar();
        Iterator<File> iter = unpacked.iterator();
        LinkedList<mergeFile> mergeFiles = new LinkedList<mergeFile>();
        while (iter.hasNext()) {
            File file = iter.next();
            mergeFile mergeFile = new mergeFile(file.getAbsolutePath());
            if (!mergeFiles.contains(mergeFile)) {
                mergeFiles.add(mergeFile);
            }
            if (JDUtilities.getController().isContainerFile(file)) {
                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RELOADCONTAINER, true)) {
                    JDUtilities.getController().loadContainerFile(file);
                }
            }
        }
        Boolean DELETE_MERGEDFILES = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_DELETE_MERGEDFILES, true);
        if (lastFinishedDownload != null && JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_USE_HJMERGE, true)) {
            Merge.mergeIt(new File(lastFinishedDownload.getFileOutput()), unrar.followingFiles, DELETE_MERGEDFILES, unrar.extractFolder);
            Iterator<mergeFile> miter = mergeFiles.iterator();
            while (miter.hasNext()) {
                mergeFile mergeFile = miter.next();
                Merge.mergeIt(mergeFile, unrar.followingFiles, DELETE_MERGEDFILES, unrar.extractFolder);
            }
        }
        IS_RUNNING = false;
        setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        Interaction.handleInteraction(Interaction.INTERACTION_AFTER_UNRAR, null);
    }

    @Override
    public String toString() {
        return JDLocale.L("interaction.unrar.name", "Extractor");
    }
}