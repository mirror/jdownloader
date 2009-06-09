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

package jd.controlling;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTMLEntities;
import jd.nutils.JDFlags;
import jd.nutils.jobber.JDRunnable;
import jd.nutils.jobber.Jobber;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Diese Klasse läuft in einem Thread und verteilt den Inhalt der Zwischenablage
 * an (unter Umständen auch mehrere) Plugins Die gefundenen Treffer werden
 * ausgeschnitten.
 * 
 * @author astaldo
 */
public class DistributeData extends Thread {
    /**
     * Der Logger
     */
    private static Logger logger = jd.controlling.JDLogger.getLogger();

    /**
     * Aufruf von Clipboard Überwachung
     */
    private boolean disableDeepEmergencyScan = false;

    /**
     * Die zu verteilenden Daten
     */
    private String data;

    /**
     * keinen Linkgrabber öffnen sondern direkt hinzufügen
     */
    private boolean hideGrabber;

    private ArrayList<DownloadLink> linkData;

    /**
     * Download nach Beendigung starten
     */
    private boolean startDownload;

    private String orgData;

    private boolean filterNormalHTTP = false;

    /**
     * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll. Die
     * übergebenen Daten werden durch einen URLDecoder geschickt.
     * 
     * @param data
     *            Daten, die verteilt werden sollen
     */
    public DistributeData(String data) {
        super("JD-DistributeData");
        this.data = new String(data);
    }

    public DistributeData(String data, boolean disableDeepEmergencyScan) {
        this(data);
        this.disableDeepEmergencyScan = disableDeepEmergencyScan;
    }

    public DistributeData(String data, boolean hideGrabber, boolean startDownload) {
        this(data);
        this.hideGrabber = hideGrabber;
        this.startDownload = startDownload;
    }

    public void setFilterNormalHTTP(boolean b) {
        this.filterNormalHTTP = b;
    }

    static public boolean hasPluginFor(String tmp, boolean filterNormalHTTP) {
        String data = tmp;
        if (DecryptPluginWrapper.getDecryptWrapper() == null) return false;
        data = data.replaceAll("jd://", "http://");
        for (DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pDecrypt.usePlugin() && pDecrypt.canHandle(data)) return true;
        }
        for (HostPluginWrapper pHost : JDUtilities.getPluginsForHost()) {
            if (pHost.usePlugin() && pHost.canHandle(data)) return true;
        }
        data = Encoding.urlDecode(data, true);
        for (DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pDecrypt.usePlugin() && pDecrypt.canHandle(data)) return true;
        }
        for (HostPluginWrapper pHost : JDUtilities.getPluginsForHost()) {
            if (pHost.usePlugin() && pHost.canHandle(data)) return true;
        }
        if (!filterNormalHTTP) {
            data = data.replaceAll("http://", "httpviajd://");
            data = data.replaceAll("https://", "httpsviajd://");
        }
        for (DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pDecrypt.usePlugin() && pDecrypt.canHandle(data)) return true;
        }
        for (HostPluginWrapper pHost : JDUtilities.getPluginsForHost()) {
            if (pHost.usePlugin() && pHost.canHandle(data)) return true;
        }
        return false;
    }

    /**
     * Sucht in dem übergebenen vector nach weiteren decryptbaren Links und
     * decrypted diese
     * 
     * @param decryptedLinks
     * @return
     */
    private boolean deepDecrypt(ArrayList<DownloadLink> decryptedLinks) {
        if (decryptedLinks.isEmpty()) return false;
        final ArrayList<DownloadLink> newdecryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> notdecryptedLinks = new ArrayList<DownloadLink>();
        class DThread extends Thread implements JDRunnable {
            private DownloadLink link = null;

            public DThread(DownloadLink link) {
                this.link = link;
            }

            public void run() {
                String url = link.getDownloadURL();

                if (url != null) {
                    url = HTMLParser.getHttpLinkList(url);

                    try {
                        url = URLDecoder.decode(url, "UTF-8");
                    } catch (Exception e) {
                        logger.warning("text not url decodeable");
                    }
                }
                boolean coulddecrypt = false;
                for (DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
                    if (pDecrypt.usePlugin() && pDecrypt.canHandle(url)) {
                        try {
                            PluginForDecrypt plg = (PluginForDecrypt) pDecrypt.getNewPluginInstance();

                            CryptedLink[] decryptableLinks = plg.getDecryptableLinks(url);
                            url = plg.cutMatches(url);
                            // Reicht die Decrypter Passwörter weiter
                            for (CryptedLink cLink : decryptableLinks) {
                                cLink.setDecrypterPassword(link.getDecrypterPassword());
                            }

                            // Reiche Properties weiter
                            for (CryptedLink cLink : decryptableLinks) {
                                cLink.setProperties(link.getProperties());
                            }

                            ArrayList<DownloadLink> dLinks = plg.decryptLinks(decryptableLinks);
                            // Reicht die Passwörter weiter
                            for (DownloadLink dLink : dLinks) {
                                dLink.addSourcePluginPasswordList(link.getSourcePluginPasswordList());
                            }
                            /* Das Plugin konnte arbeiten */
                            coulddecrypt = true;
                            if (dLinks != null && dLinks.size() > 0) {
                                synchronized (newdecryptedLinks) {
                                    newdecryptedLinks.addAll(dLinks);
                                }
                            }
                            break;
                        } catch (Exception e) {
                            JDLogger.exception(e);
                        }
                    }
                }
                if (coulddecrypt == false) {
                    synchronized (notdecryptedLinks) {
                        notdecryptedLinks.add(link);
                    }
                }
            }

            public void go() throws Exception {
                run();

            }
        }

        Jobber decryptJobbers = new Jobber(4);
        for (int b = decryptedLinks.size() - 1; b >= 0; b--) {
            DThread dthread = new DThread(decryptedLinks.get(b));
            decryptJobbers.add(dthread);
        }
        int todo = decryptJobbers.getJobsAdded();
        decryptJobbers.start();
        while (decryptJobbers.getJobsFinished() != todo) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }
        decryptJobbers.stop();
        decryptedLinks.clear();
        decryptedLinks.addAll(newdecryptedLinks);
        decryptedLinks.addAll(notdecryptedLinks);
        return newdecryptedLinks.size() > 0;
    }

    /**
     * Ermittelt über die Plugins alle Passenden Links und gibt diese in einem
     * ArrayList zurück
     * 
     * @return Link-ArrayList
     */
    public ArrayList<DownloadLink> findLinks() {
        ArrayList<String> foundPasswords = HTMLParser.findPasswords(data);
        data = HTMLEntities.unhtmlentities(data);
        data = data.replaceAll("jd://", "http://");
        ArrayList<DownloadLink> ret = findLinksIntern();
        data = Encoding.urlDecode(data, true);
        ret.addAll(findLinksIntern());
        if (!filterNormalHTTP) {
            data = data.replaceAll("--CUT--", "\n");
            data = data.replaceAll("http://", "httpviajd://");
            data = data.replaceAll("https://", "httpsviajd://");
            ret.addAll(findLinksIntern());
            data = data.replaceAll("httpviajd://", "http://");
            data = data.replaceAll("httpsviajd://", "https://");
        }
        for (DownloadLink link : ret) {
            link.addSourcePluginPasswordList(foundPasswords);
        }
        return ret;
    }

    private ArrayList<DownloadLink> findLinksIntern() {

        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        if (JDUtilities.getPluginsForHost() == null) return new ArrayList<DownloadLink>();

        this.orgData = data;
        reformDataString();
        // es werden die entschlüsselten Links (soweit überhaupt
        // vorhanden) an die HostPlugins geschickt, damit diese einen
        // Downloadlink erstellen können

        // Edit Coa:
        // Hier werden auch die SourcePLugins in die Downloadlinks gesetzt
        ArrayList<DownloadLink> alldecrypted = handleDecryptPlugins();
        ArrayList<HostPluginWrapper> pHostAll = JDUtilities.getPluginsForHost();
        for (DownloadLink decrypted : alldecrypted) {
            if (!checkdecrypted(pHostAll, links, decrypted)) {
                if (decrypted.getDownloadURL() != null) {
                    if (!filterNormalHTTP) {
                        decrypted.setUrlDownload(decrypted.getDownloadURL().replaceAll("http://", "httpviajd://"));
                        decrypted.setUrlDownload(decrypted.getDownloadURL().replaceAll("https://", "httpsviajd://"));
                        checkdecrypted(pHostAll, links, decrypted);
                    }
                }
            }
        }
        // Danach wird der (noch verbleibende) Inhalt der Zwischenablage an die
        // Plugins der Hoster geschickt
        useHoster(links);
        return links;
    }

    private boolean checkdecrypted(ArrayList<HostPluginWrapper> pHostAll, ArrayList<DownloadLink> links, DownloadLink decrypted) {
        if (decrypted.getDownloadURL() == null) return true;
        if (LinkGrabberController.isFiltered(decrypted)) return true;
        boolean gothost = false;
        for (HostPluginWrapper pHost : pHostAll) {
            try {
                if (pHost.canHandle(decrypted.getDownloadURL())) {
                    ArrayList<DownloadLink> dLinks = pHost.getPlugin().getDownloadLinks(decrypted.getDownloadURL(), decrypted.getFilePackage() != FilePackage.getDefaultFilePackage() ? decrypted.getFilePackage() : null);
                    gothost = true;
                    if (!pHost.usePlugin()) break;
                    for (int c = 0; c < dLinks.size(); c++) {
                        dLinks.get(c).addSourcePluginPasswordList(decrypted.getSourcePluginPasswordList());
                        dLinks.get(c).setSourcePluginComment(decrypted.getSourcePluginComment());
                        dLinks.get(c).setName(decrypted.getName());
                        dLinks.get(c).setFinalFileName(decrypted.getFinalFileName());
                        dLinks.get(c).setBrowserUrl(decrypted.getBrowserUrl());
                        if (decrypted.isAvailabilityStatusChecked()) dLinks.get(c).setAvailable(decrypted.isAvailable());
                        dLinks.get(c).setProperties(decrypted.getProperties());
                        dLinks.get(c).getLinkStatus().setStatusText(decrypted.getLinkStatus().getStatusString());
                        dLinks.get(c).setDownloadSize(decrypted.getDownloadSize());
                        dLinks.get(c).setSubdirectory(decrypted);
                    }
                    links.addAll(dLinks);
                    break;
                }
            } catch (Exception e) {
                logger.severe("Decrypter/Search Fehler: " + e.getMessage());
                JDLogger.exception(e);
            }
        }
        return gothost;
    }

    private void useHoster(ArrayList<DownloadLink> links) {
        for (HostPluginWrapper pHost : JDUtilities.getPluginsForHost()) {
            if (pHost.canHandle(pHost.isAcceptOnlyURIs() ? data : orgData)) {
                ArrayList<DownloadLink> dl = pHost.getPlugin().getDownloadLinks(pHost.isAcceptOnlyURIs() ? data : orgData, null);
                if (pHost.isAcceptOnlyURIs()) {
                    data = pHost.getPlugin().cutMatches(data);
                } else {
                    orgData = pHost.getPlugin().cutMatches(orgData);
                }
                if (!pHost.usePlugin()) continue;
                for (DownloadLink dll : dl) {
                    if (LinkGrabberController.isFiltered(dll)) continue;
                    links.add(dll);
                }
            }
        }
    }

    public ArrayList<DownloadLink> getLinkData() {
        return linkData;
    }

    private ArrayList<DownloadLink> handleDecryptPlugins() {

        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (DecryptPluginWrapper.getDecryptWrapper() == null) return decryptedLinks;

        class DThread extends Thread implements JDRunnable {
            private CryptedLink[] decryptableLinks = null;
            private PluginForDecrypt plg = null;

            public DThread(PluginForDecrypt plg, CryptedLink[] decryptableLinks) {
                this.decryptableLinks = decryptableLinks;
                this.plg = plg;
            }

            public void run() {
                ArrayList<DownloadLink> tmp = plg.decryptLinks(decryptableLinks);
                synchronized (decryptedLinks) {
                    decryptedLinks.addAll(tmp);
                }
            }

            public void go() throws Exception {
                run();
            }
        }
        Jobber decryptJobbers = new Jobber(4);
        for (DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pDecrypt.usePlugin() && pDecrypt.canHandle(pDecrypt.isAcceptOnlyURIs() ? data : orgData)) {

                try {
                    PluginForDecrypt plg = (PluginForDecrypt) pDecrypt.getNewPluginInstance();

                    CryptedLink[] decryptableLinks = plg.getDecryptableLinks(plg.isAcceptOnlyURIs() ? data : orgData);
                    if (plg.isAcceptOnlyURIs()) {
                        data = plg.cutMatches(data);
                    } else {
                        orgData = plg.cutMatches(orgData);
                    }

                    DThread dthread = new DThread(plg, decryptableLinks);
                    decryptJobbers.add(dthread);
                } catch (Exception e) {
                    JDLogger.exception(e);
                }
            }
        }
        int todo = decryptJobbers.getJobsAdded();
        decryptJobbers.start();
        while (decryptJobbers.getJobsFinished() != todo) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }
        decryptJobbers.stop();
        int i = 1;
        while (deepDecrypt(decryptedLinks)) {
            i++;
            logger.info("Deepdecrypt depths: " + i);
        }
        return decryptedLinks;
    }

    /**
     * Bringt alle links in data in eine einheitliche Form
     */
    private void reformDataString() {
        if (data != null) {
            String tmp = HTMLParser.getHttpLinkList(data);
            if (!(tmp.length() == 0)) {
                data = tmp;
            }
        }
    }

    // @Override
    public void run() {

        ArrayList<DownloadLink> links = findLinks();

        if (links.size() == 0 && !disableDeepEmergencyScan) {
            String[] ls = HTMLParser.getHttpLinks(data, null);
            if (ls.length > 0) {
                String txt = "\r\n";
                for (String l : ls)
                    txt += l + "\r\n";
                logger.warning("No supported links found -> search for links in source code of all urls");

                String title = JDLocale.L("gui.dialog.deepdecrypt.title", "Deep decryption?");
                String message = JDLocale.LF("gui.dialog.deepdecrypt.message", "JDownloader has not found anything on %s\r\n-------------------------------\r\nJD now loads this page to look for further links.", txt + "");
                int res = UserIO.getInstance().requestConfirmDialog(0, title, message, JDTheme.II("gui.images.search", 32, 32), JDLocale.L("gui.btn_continue", "Continue"), null);
                if (JDFlags.hasAllFlags(res, UserIO.RETURN_OK)) {

                    data = getLoadLinkString(data);

                    links = findLinks();
                }
            }

        }

        Collections.sort(links);

        if (hideGrabber && startDownload) {
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED_HIDEGRABBER_START, links));
        } else if (hideGrabber) {
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED_HIDEGRABBER, links));
        } else if (startDownload) {
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED, links));
            if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
                JDUtilities.getController().toggleStartStop();
            }
        } else {
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED, links));
        }
    }

    public void setLinkData(ArrayList<DownloadLink> linkData) {
        this.linkData = linkData;
    }

    /**
     * searches for links in linkstring, loads them, and parses the source for
     * each link
     * 
     * @param linkstring
     */
    public static void loadAndParse(String linkstring) {

        JDController.getInstance().distributeLinks(getLoadLinkString(linkstring));

    }

    private static String getLoadLinkString(String linkstring) {
        StringBuffer sb = new StringBuffer();
        String[] links = HTMLParser.getHttpLinks(linkstring, null);
        ProgressController pc = new ProgressController(JDLocale.LF("gui.addurls.progress", "Parse %s URL(s)", links.length), links.length);
        int i = 0;

        for (String l : links) {
            Browser br = new Browser();

            try {
                new URL(l);
                pc.setStatusText(JDLocale.LF("gui.addurls.progress.get", "Parse %s URL(s). Get %s links", links.length, l));

                br.getPage(l);

                String[] found = HTMLParser.getHttpLinks(br + "", l);
                for (String f : found) {

                    i++;
                    sb.append("\r\n" + f);
                }

            } catch (Exception e1) {

            }
            pc.setStatusText(JDLocale.LF("gui.addurls.progress.found", "Parse %s URL(s). Found %s links", links.length, i));
            pc.increase(1);

        }
        JDLogger.getLogger().info("Found Links" + sb);
        pc.finalize(2000);
        return sb.toString();
    }
}