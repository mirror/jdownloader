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

package jd.plugins;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.JDLogger;
import jd.controlling.JDPluginLogger;
import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.nutils.JDFlags;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;

import org.appwork.utils.AwReg;

/**
 * Dies ist die Oberklasse für alle Plugins, die Containerdateien nutzen können
 * 
 * @author astaldo/JD-Team
 */

public abstract class PluginsC extends Plugin {

    protected JDPluginLogger logger = null;

    public PluginsC(final PluginWrapper wrapper) {
        super(wrapper);
        br = new Browser();
        logger = new JDPluginLogger(wrapper.getHost() + System.currentTimeMillis());
    }

    private static final HashMap<String, PluginsC> PLUGINS                 = new HashMap<String, PluginsC>();

    private static final int                       STATUS_NOTEXTRACTED     = 0;

    private static final int                       STATUS_ERROR_EXTRACTING = 1;

    protected ArrayList<DownloadLink>              cls                     = new ArrayList<DownloadLink>();

    private ContainerStatus                        containerStatus         = null;

    protected ArrayList<String>                    dlU;

    protected String                               md5;
    protected byte[]                               k;
    protected ProgressController                   progress;

    private int                                    status                  = STATUS_NOTEXTRACTED;

    public abstract ContainerStatus callDecryption(File file);

    // @Override
    public synchronized boolean canHandle(final String data) {
        if (data == null) { return false; }
        final String match = new AwReg(data, this.getSupportedLinks()).getMatch(-1);

        return match != null && match.equalsIgnoreCase(data);
    }

    public String createContainerString(ArrayList<DownloadLink> downloadLinks) {
        return null;
    }

    // @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    /**
     * geht die containedLinks liste durch und decrypted alle links die darin
     * sind.
     */
    private void decryptLinkProtectorLinks() {
        final ArrayList<DownloadLink> tmpDlink = new ArrayList<DownloadLink>();
        final ArrayList<String> tmpURL = new ArrayList<String>();
        int i = 0;
        int c = 0;
        progress.addToMax(dlU.size());
        for (String string : dlU) {
            progress.increase(1);
            progress.setStatusText(JDL.LF("plugins.container.decrypt", "Decrypt link %s", i));

            final DistributeData distributeData = new DistributeData(string);
            final ArrayList<DownloadLink> links = distributeData.findLinks();

            final DownloadLink srcLink = cls.get(i);
            final Iterator<DownloadLink> it = links.iterator();
            progress.addToMax(links.size());

            while (it.hasNext()) {
                progress.increase(1);
                final DownloadLink next = it.next();
                tmpDlink.add(next);
                tmpURL.add(next.getDownloadURL());

                if (srcLink.getContainerFile() != null) {
                    next.setContainerFile(srcLink.getContainerFile());
                    next.setContainerIndex(c++);
                }
                next.getSourcePluginPasswordList().addAll(srcLink.getSourcePluginPasswordList());
                String comment = "";
                if (srcLink.getSourcePluginComment() != null) {
                    comment += srcLink.getSourcePluginComment();
                }
                if (next.getSourcePluginComment() != null) {
                    if (comment.length() == 0) {
                        comment += "->" + next.getSourcePluginComment();
                    } else {
                        comment += next.getSourcePluginComment();
                    }
                }
                next.setSourcePluginComment(comment);
                next.setLoadedPluginForContainer(this);
                /* forward custom package */
                if (!srcLink.isDefaultFilePackage()) next.setFilePackage(srcLink.getFilePackage());
                /* hide links? */
                if (hideLinks()) {
                    next.setLinkType(DownloadLink.LINKTYPE_CONTAINER);
                    next.setUrlDownload(null);
                } else {
                    next.setLinkType(DownloadLink.LINKTYPE_NORMAL);
                }
                if (links.size() == 1) {
                    /* only set those variables on 1:1 mapping */
                    next.setName(srcLink.getName());
                    if (srcLink.getForcedFileName() != null) next.forceFileName(srcLink.getForcedFileName());
                    if (srcLink.getDownloadSize() > 0) {
                        next.setDownloadSize(srcLink.getDownloadSize());
                    }
                    if (srcLink.isAvailabilityStatusChecked()) {
                        next.setAvailableStatus(srcLink.getAvailableStatus());
                    }
                }
                if (srcLink.gotBrowserUrl()) {
                    next.setBrowserUrl(srcLink.getBrowserUrl());
                }
            }
            i++;
        }
        cls = tmpDlink;
        dlU = tmpURL;
    }

    /* hide links by default */
    public boolean hideLinks() {
        return true;
    }

    /**
     * Erstellt eine Kopie des Containers im Homedir.
     */
    public synchronized void doDecryption(final String parameter) {
        logger.info("DO STEP");
        final String file = parameter;
        if (status == STATUS_ERROR_EXTRACTING) {
            logger.severe("Expired JD Version. Could not extract links");
            return;
        }
        if (file == null) {
            logger.severe("Containerfile == null");
            return;
        }
        final File f = JDUtilities.getResourceFile(file);
        if (md5 == null) {
            md5 = JDHash.getMD5(f);
        }

        final String extension = JDIO.getFileExtension(f);
        if (f.exists()) {
            final File res = JDUtilities.getResourceFile("container/" + md5 + "." + extension, true);
            if (!res.exists()) {
                JDIO.copyFile(f, res);
            }
            if (!res.exists()) {
                logger.severe("Could not copy file to homedir");
            }
            containerStatus = callDecryption(res);
        }
        return;
    }

    public abstract String[] encrypt(String plain);

    /**
     * Diese Methode liefert eine URL zurück, von der aus der Download gestartet
     * werden kann
     * 
     * @param downloadLink
     *            Der DownloadLink, dessen URL zurückgegeben werden soll
     * @return Die URL als String
     */
    public synchronized String extractDownloadURL(final DownloadLink downloadLink) {
        // logger.info("EXTRACT " + downloadLink);
        if (dlU == null) {
            initContainer(downloadLink.getContainerFile(), downloadLink.getGenericProperty("k", new byte[] {}));
        }
        checkWorkaround(downloadLink);
        if (dlU == null || dlU.size() <= downloadLink.getContainerIndex()) { return null; }
        downloadLink.setProperty("k", k);
        return dlU.get(downloadLink.getContainerIndex());
    }

    /**
     * workaround and cortrection of a dlc bug
     * 
     * @param downloadLink
     */
    private void checkWorkaround(final DownloadLink downloadLink) {
        final ArrayList<DownloadLink> links = JDUtilities.getDownloadController().getAllDownloadLinks();
        final ArrayList<DownloadLink> failed = new ArrayList<DownloadLink>();
        int biggestIndex = 0;
        final String dlContainerFile = downloadLink.getContainerFile();
        for (final DownloadLink l : links) {
            final String containerFile = l.getContainerFile();
            if (containerFile != null && containerFile.equalsIgnoreCase(dlContainerFile)) {
                failed.add(l);
                biggestIndex = Math.max(biggestIndex, l.getContainerIndex());
            }
        }

        if (biggestIndex >= dlU.size()) {
            final ArrayList<DownloadLink> rename = new ArrayList<DownloadLink>();
            System.err.println("DLC missmatch found");
            String ren = "";
            for (final DownloadLink l : failed) {
                if (new File(l.getFileOutput()).exists() && l.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                    rename.add(l);
                    ren += l.getFileOutput() + "<br>";
                }
            }
            if (JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.STYLE_HTML, "DLC Missmatch", "<b>JD discovered an error while downloading DLC links.</b> <br>The following files may have errors:<br>" + ren + "<br><u> Do you want JD to try to correct them?</u>"), UserIO.RETURN_OK)) {
                int ffailed = 0;
                ren = "";
                for (final DownloadLink l : rename) {
                    String name = l.getName();
                    String filename = new File(l.getFileOutput()).getName();
                    l.setUrlDownload(dlU.get(l.getContainerIndex() / 2));
                    if (l.isAvailable()) {

                        String newName = l.getName();

                        if (!name.equals(newName)) {
                            if (JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.STYLE_HTML, "Rename file", "<b>Filename missmatch</b> <br>This file seems to have the wrong name:" + filename + "<br><u> Rename it to " + newName + "?</u>"), UserIO.RETURN_OK)) {
                                File newFile = new File(new File(l.getFileOutput()).getParent() + "/restore/" + newName);
                                newFile.mkdirs();
                                if (newFile.exists()) {
                                    ffailed++;
                                    ren += l.getFileOutput() + " -> RENAME TO " + newFile + " FAILED<br>";
                                } else {
                                    if (new File(l.getFileOutput()).renameTo(newFile)) {
                                        ren += l.getFileOutput() + " -> " + newFile + "<br>";
                                    } else {
                                        ren += l.getFileOutput() + " -> RENAME TO " + newFile + " FAILED<br>";
                                    }
                                }
                            }
                        }
                    }
                    l.setUrlDownload(null);
                }
                JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.STYLE_HTML, "DLC Correction", "<b>Correction result:</b> <br>" + ren + ""), UserIO.RETURN_OK);
                ren = null;
            }
            for (final DownloadLink l : failed) {
                l.setContainerIndex(l.getContainerIndex() / 2);
            }
        }
    }

    /**
     * Findet anhand des Hostnamens ein passendes Plugiln
     * 
     * @param data
     *            Hostname
     * @return Das gefundene Plugin oder null
     */
    protected PluginForHost findHostPlugin(final String data) {
        for (final HostPluginWrapper pHost : HostPluginWrapper.getHostWrapper()) {
            if (pHost.canHandle(data)) return pHost.getPlugin();
        }
        return null;
    }

    /**
     * Liefert alle in der Containerdatei enthaltenen Dateien als DownloadLinks
     * zurück.
     * 
     * @param filename
     *            Die Containerdatei
     * @return Ein ArrayList mit DownloadLinks
     */
    public ArrayList<DownloadLink> getContainedDownloadlinks() {
        return cls == null ? new ArrayList<DownloadLink>() : cls;
    }

    /**
     * Gibt das passende plugin für diesen container zurück. falls schon eins
     * exestiert wird dieses zurückgegeben.
     * 
     * @param containerFile
     * @return
     */
    public PluginsC getPlugin(final String containerFile) {
        if (PLUGINS.containsKey(containerFile)) { return PLUGINS.get(containerFile); }
        try {
            final PluginsC newPlugin = this.getClass().newInstance();
            PLUGINS.put(containerFile, newPlugin);
            return newPlugin;
        } catch (InstantiationException e) {
            JDLogger.exception(e);
        } catch (IllegalAccessException e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public synchronized void initContainer(String filename, final byte[] bs) {
        if (filename == null) return;
        final File rel = JDUtilities.getResourceFile(filename);
        final File ab = new File(filename);
        final String md;

        if (!rel.exists() && ab.exists()) {
            final String extension = JDIO.getFileExtension(ab);
            md = JDHash.getMD5(ab);
            final File newFile = JDUtilities.getResourceFile("container/" + md + "." + extension, true);
            if (!newFile.exists()) {
                JDIO.copyFile(ab, newFile);
            }
            filename = "container/" + md + "." + extension;
        }

        if (cls == null || cls.size() == 0) {
            logger.info("Init Container");
            fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, this);
            if (progress != null) {
                progress.doFinalize();
            }
            progress = new ProgressController(JDL.L("plugins.container.open", "Open Container"), 10, null);
            progress.increase(1);
            if (bs != null) k = bs;
            try {
                doDecryption(filename);
            } catch (Throwable e) {
                JDLogger.exception(e);
            }
            progress.increase(1);

            logger.info(filename + " Parse");
            if (cls != null && dlU != null) {
                progress.setStatusText(JDL.LF("plugins.container.found", "Prozess %s links", cls.size()));
                decryptLinkProtectorLinks();
                progress.setStatusText(JDL.LF("plugins.container.exit", "Finished. Found %s links", cls.size()));
                final Iterator<DownloadLink> it = cls.iterator();
                while (it.hasNext()) {
                    it.next().setLinkType(DownloadLink.LINKTYPE_CONTAINER);
                }
                progress.increase(1);
            }
            if (this.containerStatus == null) {
                progress.setColor(Color.RED);
                progress.setStatusText(JDL.LF("plugins.container.exit.error", "Container error: %s", "Container not found!"));
                progress.doFinalize(500);
            } else if (!this.containerStatus.hasStatus(ContainerStatus.STATUS_FINISHED)) {
                progress.setColor(Color.RED);
                progress.setStatusText(JDL.LF("plugins.container.exit.error", "Container error: %s", containerStatus.getStatusText()));
                progress.doFinalize(5000);
                WebUpdate.doUpdateCheck(false);
            } else {
                progress.doFinalize();
            }
            fireControlEvent(ControlEvent.CONTROL_PLUGIN_INACTIVE, this);
        }
    }

    public void initContainer(final String absolutePath) {
        this.initContainer(absolutePath, null);
    }

}
