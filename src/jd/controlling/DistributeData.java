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
import jd.parser.HTMLParser;
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
        try {
            this.data = URLDecoder.decode(this.data, "UTF-8");
        } catch (Exception e) {
            logger.warning("text not url decodeable");
        }
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

        data = data.replaceAll("http://", "httpviajd://");

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
        boolean hasDecryptedLinks = false;

        for (int i = decryptedLinks.size() - 1; i >= 0; i--) {
            DownloadLink link = decryptedLinks.get(i);
            String url = link.getDownloadURL();

            if (url != null) {
                url = HTMLParser.getHttpLinkList(url);

                try {
                    url = URLDecoder.decode(url, "UTF-8");
                } catch (Exception e) {
                    logger.warning("text not url decodeable");
                }
            }

            boolean canDecrypt = false;
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

                        decryptedLinks.addAll(dLinks);
                        canDecrypt = true;
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (canDecrypt) {
                decryptedLinks.remove(i);
                hasDecryptedLinks = true;
            }

        }
        return hasDecryptedLinks;
    }

    /**
     * Ermittelt über die Plugins alle Passenden Links und gibt diese in einem
     * Vector zurück
     * 
     * @return Link-Vector
     */
    public Vector<DownloadLink> findLinks() {
        return findLinks(true);
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

        for (DownloadLink decrypted : handleDecryptPlugins()) {
            for (HostPluginWrapper pHost : JDUtilities.getPluginsForHost()) {
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
                            dLinks.get(c).setProperties(decrypted.getProperties());
                            dLinks.get(c).getLinkStatus().setStatusText(decrypted.getLinkStatus().getStatusString());
                            dLinks.get(c).setDownloadSize(decrypted.getDownloadSize());
                            dLinks.get(c).setSubdirectory(decrypted);

                        }
                        links.addAll(dLinks);
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
        data = data.replaceAll("http://", "httpviajd://");
        useHoster(foundPasswords, links);
        data = data.replaceAll("httpviajd://", "http://");

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

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (DecryptPluginWrapper.getDecryptWrapper() == null) return decryptedLinks;

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

                    decryptedLinks.addAll(plg.decryptLinks(decryptableLinks));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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
            try {
                if (tmp == "") {
                    data = URLDecoder.decode(data, "UTF-8");
                } else {
                    data = URLDecoder.decode(tmp, "UTF-8");
                }
            } catch (Exception e) {
                logger.warning("text not url decodeable");
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

            Browser br = new Browser();
            for (String url : urls) {

                try {
                    data += br.getPage(url) + " ";
                } catch (Exception e) {
                }

            }

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
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_STARTSTOP_DOWNLOAD, this));
            }
        } else {
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED, links));
        }
    }

    public void setLinkData(Vector<DownloadLink> linkData) {
        this.linkData = linkData;
    }
}