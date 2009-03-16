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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.logging.Logger;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.event.ControlEvent;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTMLEntities;
import jd.nutils.jobber.JDRunnable;
import jd.nutils.jobber.Jobber;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

/**
 * Diese Klasse läuft in einem Thread und verteilt den Inhalt der Zwischenablage
 * an (unter Umständen auch mehrere) Plugins Die gefundenen Treffer werden
 * ausgeschnitten.
 * 
 * @author astaldo
 */
public class DistributeData extends ControlBroadcaster {
    /**
     * Der Logger
     */
    private static Logger logger = JDUtilities.getLogger();

    /**
     * Aufruf von Clipboard Überwachung
     */
    private boolean clipboard = false;

    /**
     * Die zu verteilenden Daten
     */
    private String data;

    /**
     * keinen Linkgrabber öffnen sondern direkt hinzufügen
     */
    private boolean hideGrabber;

    private Vector<DownloadLink> linkData;

    /**
     * Download nach Beendigung starten
     */
    private boolean startDownload;

    private String orgData;

    /**
     * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll. Die
     * übergebenen Daten werden durch einen URLDecoder geschickt.
     * 
     * @param data
     *            Daten, die verteilt werden sollen
     */
    public DistributeData(String data) {
        super("JD-DistributeData");
        this.data = data;
    }

    public DistributeData(String data, boolean clipboard) {
        this(data);
        this.clipboard = clipboard;
    }

    public DistributeData(String data, boolean hideGrabber, boolean startDownload) {
        this(data);
        this.hideGrabber = hideGrabber;
        this.startDownload = startDownload;
    }

    static public boolean hasPluginFor(String tmp) {
        String data = tmp;
        if (DecryptPluginWrapper.getDecryptWrapper() == null) return false;

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
        data = data.replaceAll("http://", "httpviajd://");
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
        final Vector<DownloadLink> newdecryptedLinks = new Vector<DownloadLink>();
        final Vector<DownloadLink> notdecryptedLinks = new Vector<DownloadLink>();
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

                            ArrayList<DownloadLink> dLinks = plg.decryptLinks(decryptableLinks);
                            // Reicht die Passwörter weiter
                            for (DownloadLink dLink : dLinks) {
                                dLink.addSourcePluginPasswords(link.getSourcePluginPasswords());
                            }
                            /* Das Plugin konnte arbeiten */
                            coulddecrypt = true;
                            if (dLinks != null && dLinks.size() > 0) {
                                newdecryptedLinks.addAll(dLinks);
                            }
                            break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (coulddecrypt == false) {
                    notdecryptedLinks.add(link);
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
     * Vector zurück
     * 
     * @return Link-Vector
     */
    public Vector<DownloadLink> findLinks() {
        data = HTMLEntities.unhtmlentities(data);
        data = data.replaceAll("jd://", "http://");
        Vector<DownloadLink> ret = findLinks(true);
        data = Encoding.urlDecode(data, true);
        ret.addAll(findLinks(true));
        data = data.replaceAll("--CUT--", "\n");
        data = data.replaceAll("http://", "httpviajd://");
        ret.addAll(findLinks(true));
        data = data.replaceAll("httpviajd://", "http://");
        return ret;
    }

    public Vector<DownloadLink> findLinks(boolean searchpw) {

        Vector<DownloadLink> links = new Vector<DownloadLink>();
        if (JDUtilities.getPluginsForHost() == null) return new Vector<DownloadLink>();

        Vector<String> foundPasswords = new Vector<String>();
        if (searchpw == true) {
            foundPasswords = HTMLParser.findPasswords(data);
        }
        this.orgData = data;
        reformDataString();
        // es werden die entschlüsselten Links (soweit überhaupt
        // vorhanden) an die HostPlugins geschickt, damit diese einen
        // Downloadlink erstellen können

        // Edit Coa:
        // Hier werden auch die SourcePLugins in die Downloadlinks gesetzt
        ArrayList<DownloadLink> alldecrypted = handleDecryptPlugins();

        for (DownloadLink decrypted : alldecrypted) {
            ArrayList<HostPluginWrapper> pHostAll = JDUtilities.getPluginsForHost();
            for (HostPluginWrapper pHost : pHostAll) {
                try {
                    if (pHost.usePlugin() && pHost.canHandle(decrypted.getDownloadURL())) {
                        Vector<DownloadLink> dLinks = pHost.getPlugin().getDownloadLinks(decrypted.getDownloadURL(), decrypted.getFilePackage() != FilePackage.getDefaultFilePackage() ? decrypted.getFilePackage() : null);

                        for (int c = 0; c < dLinks.size(); c++) {
                            dLinks.get(c).addSourcePluginPasswords(foundPasswords);
                            dLinks.get(c).addSourcePluginPasswords(decrypted.getSourcePluginPasswords());
                            dLinks.get(c).setSourcePluginComment(decrypted.getSourcePluginComment());
                            dLinks.get(c).setName(decrypted.getName());
                            dLinks.get(c).setFinalFileName(decrypted.getFinalFileName());
                            dLinks.get(c).setBrowserUrl(decrypted.getBrowserUrl());
                            if (decrypted.isAvailabilityChecked()) dLinks.get(c).setAvailable(decrypted.isAvailable());
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
                    e.printStackTrace();
                }
            }
        }
        // Danach wird der (noch verbleibende) Inhalt der Zwischenablage an die
        // Plugins der Hoster geschickt
        useHoster(foundPasswords, links);
        return links;
    }

    private void useHoster(Vector<String> passwords, Vector<DownloadLink> links) {
        for (HostPluginWrapper pHost : JDUtilities.getPluginsForHost()) {
            if (pHost.usePlugin() && pHost.canHandle(pHost.isAcceptOnlyURIs() ? data : orgData)) {
                Vector<DownloadLink> dl = pHost.getPlugin().getDownloadLinks(pHost.isAcceptOnlyURIs() ? data : orgData, null);
                if (passwords.size() > 0) {
                    for (DownloadLink dLink : dl) {
                        dLink.addSourcePluginPasswords(passwords);
                    }
                }
                links.addAll(dl);
                if (pHost.isAcceptOnlyURIs()) {
                    data = pHost.getPlugin().cutMatches(data);
                } else {
                    orgData = pHost.getPlugin().cutMatches(orgData);
                }
            }
        }
    }

    public Vector<DownloadLink> getLinkData() {
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
                decryptedLinks.addAll(plg.decryptLinks(decryptableLinks));
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
                    e.printStackTrace();
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

    @Override
    public void run() {

        Vector<DownloadLink> links = findLinks();

        if (links.size() == 0 && !clipboard) {

            logger.info("No supported links found -> search for links in source code of all urls");
            String[] urls = HTMLParser.getHttpLinks(data, null);

            if (urls.length > 0) {
                data = "";
            }
            StringBuilder buff = new StringBuilder(data);
            Browser br = new Browser();
            br.setFollowRedirects(false);
            for (String url : urls) {

                try {
                    buff.append(br.getPage(url));
                    if (br.getRedirectLocation() != null) {
                        buff.append(' ');
                        buff.append(br.getRedirectLocation());
                    }
                    buff.append(' ');
                } catch (Exception e) {
                }

            }
            data = buff.toString();
            links = findLinks();

        }

        Collections.sort(links);

        if (hideGrabber && startDownload) {
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED_HIDEGRABBER_START, links));
        } else if (hideGrabber) {
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED_HIDEGRABBER, links));
        } else if (startDownload) {
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED, links));
            if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
                JDUtilities.getController().toggleStartStop();
            }
        } else {
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED, links));
        }
    }

    public void setLinkData(Vector<DownloadLink> linkData) {
        this.linkData = linkData;
    }
}