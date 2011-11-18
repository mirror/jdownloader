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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.regex.Pattern;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.gui.UserIO;
import jd.nutils.Formatter;
import jd.nutils.JDFlags;
import jd.nutils.encoding.Encoding;
import jd.utils.JDUtilities;

import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

/**
 * Dies ist die Oberklasse für alle Plugins, die Containerdateien nutzen können
 * 
 * @author astaldo/JD-Team
 */

public abstract class PluginsC {

    private Pattern pattern;

    private String  name;

    private long    version;

    public PluginsC(String name, String pattern, String rev) {

        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        this.name = name;
        try {
            version = Formatter.getRevision(rev);
        } catch (Throwable e) {
            version = -1;
        }
    }

    private static final int          STATUS_NOTEXTRACTED     = 0;

    private static final int          STATUS_ERROR_EXTRACTING = 1;

    protected ArrayList<DownloadLink> cls                     = new ArrayList<DownloadLink>();

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

    public Pattern getSupportedLinks() {
        return pattern;
    }

    public String getName() {
        return name;
    }

    public long getVersion() {
        return version;
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

        for (String string : dlU) {
            LinkCrawler lc = new LinkCrawler();
            lc.setFilter(LinkFilterController.getInstance());
            lc.crawlNormal(string);
            lc.waitForCrawling();
            final ArrayList<CrawledLink> links = lc.getCrawledLinks();

            final DownloadLink srcLink = cls.get(i);
            final Iterator<CrawledLink> it = links.iterator();

            while (it.hasNext()) {
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
                if (srcLink.getComment() != null) {
                    comment += srcLink.getComment();
                }
                if (next.getComment() != null) {
                    if (comment.length() == 0) {
                        comment += "->" + next.getComment();
                    } else {
                        comment += next.getComment();
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
        Log.L.info("DO STEP");
        final String file = parameter;
        if (status == STATUS_ERROR_EXTRACTING) {
            Log.L.severe("Expired JD Version. Could not extract links");
            return;
        }
        if (file == null) {
            Log.L.severe("Containerfile == null");
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
                Log.L.severe("Could not copy file to homedir");
            }
            callDecryption(res);
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
            Log.L.info("Init Container");

            if (bs != null) k = bs;
            try {
                doDecryption(filename);
            } catch (Throwable e) {
                Log.L.severe(e.toString());
            }

            Log.L.info(filename + " Parse");
            if (cls != null && dlU != null) {

                decryptLinkProtectorLinks();

                final Iterator<DownloadLink> it = cls.iterator();
                while (it.hasNext()) {
                    it.next().setLinkType(DownloadLink.LINKTYPE_CONTAINER);
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

    public ArrayList<DownloadLink> decryptContainer(CrawledLink source) {

        if (source.getURL() == null) return null;
        ArrayList<DownloadLink> tmpLinks = null;
        boolean showException = true;
        try {

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
             * damn, something must have gone really really bad, lets keep the
             * log
             */

            Log.L.log(Level.SEVERE, "Exception", e);
        }
        if (tmpLinks == null && showException) {
            /*
             * null as return value? something must have happened, do not clear
             * log
             */
            Log.L.severe("ContainerPlugin out of date: " + this + " :" + getVersion());

        }
        return tmpLinks;
    }

}