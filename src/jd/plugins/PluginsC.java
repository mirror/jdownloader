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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.JDPluginLogger;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.nutils.JDFlags;
import jd.nutils.encoding.Encoding;
import jd.utils.JDUtilities;

import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.plugins.controller.container.LazyContainerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.translate._JDT;

/**
 * Dies ist die Oberklasse für alle Plugins, die Containerdateien nutzen können
 * 
 * @author astaldo/JD-Team
 */

public abstract class PluginsC extends Plugin {

    protected JDPluginLogger    logger = null;

    private LazyContainerPlugin lazyCo = null;

    public LazyContainerPlugin getLazyCo() {
        return lazyCo;
    }

    public void setLazyCo(LazyContainerPlugin lazyCo) {
        this.lazyCo = lazyCo;
    }

    public PluginsC(final PluginWrapper wrapper) {
        super(wrapper);
        this.lazyCo = (LazyContainerPlugin) wrapper.getLazy();
    }

    @Override
    public SubConfiguration getPluginConfig() {
        return SubConfiguration.getConfig(lazyCo.getDisplayName());
    }

    private static final int          STATUS_NOTEXTRACTED     = 0;

    private static final int          STATUS_ERROR_EXTRACTING = 1;

    protected ArrayList<DownloadLink> cls                     = new ArrayList<DownloadLink>();

    private ContainerStatus           containerStatus         = null;

    protected ArrayList<String>       dlU;

    protected String                  md5;
    protected byte[]                  k;

    private int                       status                  = STATUS_NOTEXTRACTED;

    public abstract ContainerStatus callDecryption(File file);

    // @Override
    public synchronized boolean canHandle(final String data) {
        if (data == null) { return false; }
        final String match = new Regex(data, this.getSupportedLinks()).getMatch(-1);
        return match != null && match.equalsIgnoreCase(data);
    }

    public String createContainerString(ArrayList<DownloadLink> downloadLinks) {
        return null;
    }

    @Override
    public Pattern getSupportedLinks() {
        return lazyCo.getPattern();
    }

    @Override
    public String getHost() {
        return lazyCo.getDisplayName();
    }

    @Override
    public long getVersion() {
        return 0;
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    /**
     * geht die containedLinks liste durch und decrypted alle links die darin
     * sind.
     */
    private void decryptLinkProtectorLinks(ProgressController progress) {
        final ArrayList<DownloadLink> tmpDlink = new ArrayList<DownloadLink>();
        final ArrayList<String> tmpURL = new ArrayList<String>();
        int i = 0;
        int c = 0;
        progress.addToMax(dlU.size());
        for (String string : dlU) {
            progress.increase(1);
            progress.setStatusText(_JDT._.plugins_container_decrypt(i));
            LinkCrawler lc = new LinkCrawler();
            lc.setFilter(LinkFilterController.getInstance());
            lc.crawlNormal(string);
            lc.waitForCrawling();
            final ArrayList<CrawledLink> links = lc.getCrawledLinks();

            final DownloadLink srcLink = cls.get(i);
            final Iterator<CrawledLink> it = links.iterator();
            progress.addToMax(links.size());

            while (it.hasNext()) {
                progress.increase(1);
                final CrawledLink nextc = it.next();
                DownloadLink next = nextc.getDownloadLink();
                if (next == null) continue;
                tmpDlink.add(next);
                tmpURL.add(next.getDownloadURL());

                if (srcLink.getContainerFile() != null) {
                    next.setContainerFile(srcLink.getContainerFile());
                    next.setContainerIndex(c++);
                }
                if (srcLink.getSourcePluginPasswordList() != null && srcLink.getSourcePluginPasswordList().size() > 0) {
                    next.addSourcePluginPasswordList(srcLink.getSourcePluginPasswordList());
                }
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
                srcLink.getFilePackage().add(next);
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
     * 
     * @throws IOException
     */
    private synchronized void doDecryption(final String parameter) throws IOException {
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
            md5 = Hash.getMD5(f);
        }

        final String extension = Files.getExtension(f.getAbsolutePath());
        if (f.exists()) {
            final File res = JDUtilities.getResourceFile("container/" + md5 + "." + extension, true);
            if (!res.exists()) {
                IO.copyFile(f, res);
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
        try {
            if (dlU == null) {
                initContainer(downloadLink.getContainerFile(), downloadLink.getGenericProperty("k", new byte[] {}));
            }
            checkWorkaround(downloadLink);
            if (dlU == null || dlU.size() <= downloadLink.getContainerIndex()) { return null; }
            downloadLink.setProperty("k", k);
            return dlU.get(downloadLink.getContainerIndex());
        } catch (final Throwable e) {
            Log.exception(e);
            return null;
        }
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
        for (final LazyHostPlugin pHost : HostPluginController.getInstance().list()) {
            if (pHost.canHandle(data)) return pHost.getPrototype();
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

    public synchronized void initContainer(String filename, final byte[] bs) throws IOException {
        if (filename == null) return;
        final File rel = JDUtilities.getResourceFile(filename);
        final File ab = new File(filename);
        final String md;

        if (!rel.exists() && ab.exists()) {
            final String extension = Files.getExtension(filename);
            md = Hash.getMD5(ab);
            final File newFile = JDUtilities.getResourceFile("container/" + md + "." + extension, true);
            if (!newFile.exists()) {
                IO.copyFile(ab, newFile);
            }
            filename = "container/" + md + "." + extension;
        }

        if (cls == null || cls.size() == 0) {
            logger.info("Init Container");
            fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, this);

            ProgressController progress = null;
            Color color = null;
            String errorTxt = null;
            int wait = 0;
            try {
                progress = new ProgressController(_JDT._.plugins_container_open(), 10, null);
                progress.increase(1);
                if (bs != null) k = bs;
                try {
                    doDecryption(filename);
                } catch (Throwable e) {
                    logger.severe(e.toString());
                }
                progress.increase(1);

                logger.info(filename + " Parse");
                if (cls != null && dlU != null) {
                    progress.setStatusText(_JDT._.plugins_container_found(cls.size()));
                    decryptLinkProtectorLinks(progress);
                    progress.setStatusText(_JDT._.plugins_container_exit(cls.size()));
                    final Iterator<DownloadLink> it = cls.iterator();
                    while (it.hasNext()) {
                        it.next().setLinkType(DownloadLink.LINKTYPE_CONTAINER);
                    }
                    progress.increase(1);
                }
                if (this.containerStatus == null) {
                    color = Color.RED;
                    errorTxt = _JDT._.plugins_container_exit_error("Container not found!");
                    wait = 500;
                } else if (!this.containerStatus.hasStatus(ContainerStatus.STATUS_FINISHED)) {
                    color = Color.RED;
                    errorTxt = _JDT._.plugins_container_exit_error(containerStatus.getStatusText());
                    wait = 1000;
                }
            } finally {
                fireControlEvent(ControlEvent.CONTROL_PLUGIN_INACTIVE, this);
                try {
                    if (color != null) progress.setColor(color);
                    if (errorTxt != null) progress.setStatusText(errorTxt);
                    if (wait > 0) {
                        progress.doFinalize(wait);
                    } else {
                        progress.doFinalize();
                    }
                } catch (final Throwable e) {
                }
            }
        }
    }

    public ArrayList<CrawledLink> getContainerLinks(String data) {
        /*
         * we dont need memory optimization here as downloadlink, crypted link
         * itself take care of this
         */
        String[] hits = new Regex(data, getSupportedLinks()).setMemoryOptimized(false).getColumn(-1);
        ArrayList<CrawledLink> chits = null;
        if (hits != null && hits.length > 0) {
            chits = new ArrayList<CrawledLink>(hits.length);
        } else {
            chits = new ArrayList<CrawledLink>();
        }
        if (hits != null && hits.length > 0) {
            for (String hit : hits) {
                String file = hit;
                file = file.trim();
                /* cut of any unwanted chars */
                while (file.length() > 0 && file.charAt(0) == '"') {
                    file = file.substring(1);
                }
                while (file.length() > 0 && file.charAt(file.length() - 1) == '"') {
                    file = file.substring(0, file.length() - 1);
                }
                file = file.trim();

                CrawledLink cli;
                chits.add(cli = new CrawledLink(file));
                cli.setcPlugin(this);
            }
        }
        return chits;
    }

    public PluginsC getNewInstance() {
        if (lazyCo == null) return null;
        return lazyCo.newInstance();
    }

    public void setBrowser(Browser br) {
        this.br = br;
    }

    public void setLogger(JDPluginLogger logger) {
        this.logger = logger;
    }

    public ArrayList<DownloadLink> decryptContainer(CrawledLink source) {
        ProgressController progress = null;
        int progressShow = 0;
        Color color = null;
        try {
            if (source.getURL() == null) return null;
            progress = new ProgressController(_JDT._.jd_plugins_PluginForDecrypt_decrypting(getHost()), null);
            ArrayList<DownloadLink> tmpLinks = null;
            boolean showException = true;
            try {
                /*
                 * we now lets log into plugin specific loggers with all
                 * verbose/debug on
                 */
                br.setLogger(logger);
                br.setVerbose(true);
                br.setDebug(true);
                /* extract filename from url */
                String file = new Regex(source.getURL(), "file://(.+)").getMatch(0);
                file = Encoding.urlDecode(file, false);
                if (file != null && new File(file).exists()) {
                    initContainer(file, null);
                    tmpLinks = getContainedDownloadlinks();
                } else {
                    throw new Throwable("Invalid Container: " + source.getURL());
                }
            } catch (Throwable e) {
                /*
                 * damn, something must have gone really really bad, lets keep
                 * the log
                 */
                progress.setStatusText(this.getHost() + ": " + e.getMessage());
                logger.log(Level.SEVERE, "Exception", e);
                color = Color.RED;
                progressShow = 15000;
            }
            if (tmpLinks == null && showException) {
                /*
                 * null as return value? something must have happened, do not
                 * clear log
                 */
                logger.severe("ContainerPlugin out of date: " + this + " :" + getVersion());
                progress.setStatusText(_JDT._.jd_plugins_PluginForDecrypt_error_outOfDate(this.getHost()));
                color = Color.RED;
                progressShow = 15000;

                /* lets forward the log */
                if (logger instanceof JDPluginLogger) {
                    /* make sure we use the right logger */
                    ((JDPluginLogger) logger).logInto(JDLogger.getLogger());
                }
            }
            return tmpLinks;
        } finally {
            try {
                if (progressShow > 0) {
                    if (color != null) {
                        progress.setColor(color);
                    }
                    progress.doFinalize(progressShow);
                } else {
                    progress.doFinalize();
                }
            } catch (Throwable e) {
            }
        }
    }

}