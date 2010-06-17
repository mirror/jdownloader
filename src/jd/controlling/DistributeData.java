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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.CPluginWrapper;
import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.JDFlags;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.nutils.jobber.JDRunnable;
import jd.nutils.jobber.Jobber;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

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
    private static final Logger LOG = JDLogger.getLogger();

    private static final int MAX_DECRYPTER_COUNT = 5;

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

    private boolean filterNormalHTTP = false;
    private final ArrayList<String> foundPasswords = new ArrayList<String>();

    private boolean autostart = false;

    /**
     * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll. Die
     * übergebenen Daten werden durch einen URLDecoder geschickt.
     * 
     * @param data
     *            Daten, die verteilt werden sollen
     */
    public DistributeData(final String data) {
        super("JD-DistributeData");
        this.data = data;
        this.disableDeepEmergencyScan = true;
    }

    public DistributeData(final String data, final boolean hideGrabber) {
        this(data);
        this.hideGrabber = hideGrabber;
    }

    public DistributeData setDisableDeepEmergencyScan(final boolean b) {
        this.disableDeepEmergencyScan = b;
        return this;
    }

    public DistributeData setHideGrabber(final boolean hideGrabber) {
        this.hideGrabber = hideGrabber;
        return this;
    }

    public DistributeData setAutostart(final boolean autostart) {
        this.autostart = autostart;
        return this;
    }

    public DistributeData setFilterNormalHTTP(final boolean b) {
        this.filterNormalHTTP = b;
        return this;
    }

    public static boolean hasContainerPluginFor(final String tmp) {
        for (CPluginWrapper cDecrypt : CPluginWrapper.getCWrapper()) {
            if (cDecrypt.isEnabled() && cDecrypt.canHandle(tmp)) return true;
        }
        return false;
    }

    public static boolean hasPluginFor(final String tmp, final boolean filterNormalHTTP) {
        if (DecryptPluginWrapper.getDecryptWrapper() != null) {
            String data = tmp;
            data = data.replaceAll("jd://", "http://");
            for (final DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
                if (pDecrypt.isEnabled() && pDecrypt.canHandle(data)) return true;
            }
            for (final HostPluginWrapper pHost : HostPluginWrapper.getHostWrapper()) {
                if (pHost.isEnabled() && pHost.canHandle(data)) return true;
            }
            data = Encoding.urlDecode(data, true);
            for (final DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
                if (pDecrypt.isEnabled() && pDecrypt.canHandle(data)) return true;
            }
            for (final HostPluginWrapper pHost : HostPluginWrapper.getHostWrapper()) {
                if (pHost.isEnabled() && pHost.canHandle(data)) return true;
            }
            if (!filterNormalHTTP) {
                data = data.replaceAll("http://", "httpviajd://");
                data = data.replaceAll("https://", "httpsviajd://");
                for (final DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
                    if (pDecrypt.isEnabled() && pDecrypt.canHandle(data)) return true;
                }
                for (final HostPluginWrapper pHost : HostPluginWrapper.getHostWrapper()) {
                    if (pHost.isEnabled() && pHost.canHandle(data)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Sucht in dem übergebenen vector nach weiteren decryptbaren Links und
     * decrypted diese
     */
    private boolean deepDecrypt(final ArrayList<DownloadLink> decryptedLinks) {
        if (decryptedLinks.isEmpty()) return false;
        final ArrayList<DownloadLink> newdecryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> notdecryptedLinks = new ArrayList<DownloadLink>();
        final PluginForHost directhttp = JDUtilities.getPluginForHost("DirectHTTP");
        class DThread implements JDRunnable {
            private final DownloadLink link;

            public DThread(final DownloadLink link) {
                this.link = link;
            }

            public void go() {
                String url = link.getDownloadURL();

                if (url != null) {
                    url = HTMLParser.getHttpLinkList(url);
                    url = Encoding.urlDecode(url, true);
                }
                boolean coulddecrypt = false;
                if (directhttp != null && directhttp.getWrapper().isEnabled() && directhttp.getWrapper().canHandle(url)) {
                    /* prevent endless loops for directhttp links */
                    coulddecrypt = false;
                } else {
                    for (final DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
                        if (pDecrypt.isEnabled() && pDecrypt.canHandle(url)) {
                            try {
                                final PluginForDecrypt plg = pDecrypt.getNewPluginInstance();

                                final CryptedLink[] decryptableLinks = plg.getDecryptableLinks(url);
                                url = plg.cutMatches(url);
                                // Reicht die Decrypter Passwörter weiter
                                for (CryptedLink cLink : decryptableLinks) {
                                    cLink.setDecrypterPassword(link.getDecrypterPassword());
                                }

                                // Reiche Properties weiter
                                for (CryptedLink cLink : decryptableLinks) {
                                    cLink.setProperties(link.getProperties());
                                }

                                final ArrayList<DownloadLink> dLinks = plg.decryptLinks(decryptableLinks);

                                /* Das Plugin konnte arbeiten */
                                coulddecrypt = true;
                                if (dLinks != null && !dLinks.isEmpty()) {
                                    // Reicht die Passwörter weiter
                                    for (final DownloadLink dLink : dLinks) {
                                        dLink.addSourcePluginPasswordList(link.getSourcePluginPasswordList());
                                    }
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
                }
                if (coulddecrypt == false) {
                    synchronized (notdecryptedLinks) {
                        notdecryptedLinks.add(link);
                    }
                }
            }
        }

        final Jobber decryptJobbers = new Jobber(MAX_DECRYPTER_COUNT);
        for (int b = decryptedLinks.size() - 1; b >= 0; b--) {
            decryptJobbers.add(new DThread(decryptedLinks.get(b)));
        }
        final int todo = decryptJobbers.getJobsAdded();
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
        return !newdecryptedLinks.isEmpty();
    }

    /**
     * Ermittelt über die Plugins alle Passenden Links und gibt diese in einem
     * ArrayList zurück
     * 
     * @return Link-ArrayList
     */
    public ArrayList<DownloadLink> findLinks() {
        /* normal quickcheck */
        ArrayList<DownloadLink> ret = quickHosterCheck(data);
        foundPasswords.addAll(HTMLParser.findPasswords(data));
        if (ret != null && ret.size() == 1 && ret.get(0).getPlugin() != null) {
            /* also check for disabled hosterplugin and filtering here */
            if (!ret.get(0).getPlugin().getWrapper().isEnabled() || LinkGrabberController.isFiltered(ret.get(0))) {
                ret.clear();
            } else {
                ret.get(0).addSourcePluginPasswordList(foundPasswords);
                return ret;
            }
        }
        data = HTMLEntities.unhtmlentities(data);
        data = data.replaceAll("jd://", "http://");
        ret = findLinksIntern();
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
        for (final DownloadLink link : ret) {
            link.addSourcePluginPasswordList(foundPasswords);
        }
        return ret;
    }

    /**
     * Checks if data is only a singlehoster link
     */
    private ArrayList<DownloadLink> quickHosterCheck(final String data) {
        final String lowercasedata = data.toLowerCase(Locale.getDefault());
        /*
         * multiple links without new line
         */
        if (new Regex(data, " http").count() > 1) return null;
        String[] res = Regex.getLines(data);
        if (res != null && res.length > 1 && res[0].contains("http") && res[1].contains("http")) return null;
        for (final HostPluginWrapper pw : HostPluginWrapper.getHostWrapper()) {
            final Pattern pattern = pw.getPattern();

            if (lowercasedata.contains(pw.getHost().toLowerCase())) {
                final String match = new Regex(data, pattern).getMatch(-1);
                if (match != null && (match.equals(data) || (match.length() > 10 + pw.getHost().length() && data.startsWith(match) && (match.length() * 2) > data.length()))) {
                    final DownloadLink dl = new DownloadLink(pw.getNewPluginInstance(), null, pw.getHost(), Encoding.urlDecode(match, true), true);
                    final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                    ret.add(dl);
                    return ret;
                }
            }
        }
        return null;
    }

    private ArrayList<DownloadLink> findLinksIntern() {
        final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        final ArrayList<HostPluginWrapper> pHostAll = HostPluginWrapper.getHostWrapper();
        if (pHostAll.size() == 0) return new ArrayList<DownloadLink>();
        reformDataString();
        /*
         * es werden die entschlüsselten Links (soweit überhaupt vorhanden) an
         * die HostPlugins geschickt, damit diese einen Downloadlink erstellen
         * können
         */
        /*
         * Edit Coa: Hier werden auch die SourcePLugins in die Downloadlinks
         * gesetzt
         */
        final ArrayList<DownloadLink> alldecrypted = handleDecryptPlugins();

        for (final DownloadLink decrypted : alldecrypted) {
            if (!checkdecrypted(pHostAll, links, decrypted)) {
                if (decrypted.getDownloadURL() != null && !filterNormalHTTP) {
                    decrypted.setUrlDownload(decrypted.getDownloadURL().replaceAll("http://", "httpviajd://"));
                    decrypted.setUrlDownload(decrypted.getDownloadURL().replaceAll("https://", "httpsviajd://"));
                    checkdecrypted(pHostAll, links, decrypted);
                }
            }
        }
        /*
         * Danach wird der (noch verbleibende) Inhalt der Zwischenablage an die
         * Plugins der Hoster geschickt
         */
        useHoster(links);
        return links;
    }

    private boolean checkdecrypted(final ArrayList<HostPluginWrapper> pHostAll, final ArrayList<DownloadLink> links, final DownloadLink decrypted) {
        if (decrypted.getDownloadURL() == null || LinkGrabberController.isFiltered(decrypted)) return true;
        boolean gothost = false;
        for (final HostPluginWrapper pHost : pHostAll) {
            try {
                if (pHost.canHandle(decrypted.getDownloadURL())) {
                    final ArrayList<DownloadLink> dLinks = pHost.getPlugin().getDownloadLinks(decrypted.getDownloadURL(), decrypted.getFilePackage() != FilePackage.getDefaultFilePackage() ? decrypted.getFilePackage() : null);
                    gothost = true;
                    if (!pHost.isEnabled()) {
                        break;
                    }
                    final int dLinksSize = dLinks.size();
                    DownloadLink dl = null;
                    for (int c = 0; c < dLinksSize; c++) {
                        dl = dLinks.get(c);
                        dl.addSourcePluginPasswordList(decrypted.getSourcePluginPasswordList());
                        dl.setSourcePluginComment(decrypted.getSourcePluginComment());
                        dl.setName(decrypted.getName());
                        dl.setFinalFileName(decrypted.getFinalFileName());
                        dl.setBrowserUrl(decrypted.getBrowserUrl());
                        if (decrypted.isAvailabilityStatusChecked()) {
                            dl.setAvailable(decrypted.isAvailable());
                        }
                        dl.setProperties(decrypted.getProperties());
                        dl.getLinkStatus().setStatusText(decrypted.getLinkStatus().getStatusString());
                        dl.setDownloadSize(decrypted.getDownloadSize());
                        dl.setSubdirectory(decrypted);
                    }
                    links.addAll(dLinks);
                    break;
                }
            } catch (Exception e) {
                LOG.severe("Decrypter/Search Fehler: " + e.getMessage());
                JDLogger.exception(e);
            }
        }
        return gothost;
    }

    private void useHoster(final ArrayList<DownloadLink> links) {
        for (final HostPluginWrapper pHost : HostPluginWrapper.getHostWrapper()) {
            if (pHost.canHandle(data)) {
                final ArrayList<DownloadLink> dl = pHost.getPlugin().getDownloadLinks(data, null);
                data = pHost.getPlugin().cutMatches(data);
                if (!pHost.isEnabled()) continue;
                for (final DownloadLink dll : dl) {
                    if (LinkGrabberController.isFiltered(dll)) continue;
                    links.add(dll);
                }
            }
        }
    }

    private ArrayList<DownloadLink> handleDecryptPlugins() {

        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (DecryptPluginWrapper.getDecryptWrapper() == null) return decryptedLinks;

        class DThread implements JDRunnable {
            private final CryptedLink[] decryptableLinks;
            private final PluginForDecrypt plg;

            public DThread(final PluginForDecrypt plg, final CryptedLink[] decryptableLinks) {
                this.decryptableLinks = decryptableLinks;
                this.plg = plg;
            }

            public void go() {
                final ArrayList<DownloadLink> tmp = plg.decryptLinks(decryptableLinks);
                synchronized (decryptedLinks) {
                    decryptedLinks.addAll(tmp);
                }
            }
        }
        final Jobber decryptJobbers = new Jobber(4);
        for (final DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pDecrypt.isEnabled() && pDecrypt.canHandle(data)) {
                try {
                    final PluginForDecrypt plg = pDecrypt.getNewPluginInstance();
                    final CryptedLink[] decryptableLinks = plg.getDecryptableLinks(data);
                    data = plg.cutMatches(data);
                    decryptJobbers.add(new DThread(plg, decryptableLinks));
                } catch (Exception e) {
                    JDLogger.exception(e);
                }
            }
        }
        final int todo = decryptJobbers.getJobsAdded();
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
            LOG.info("Deepdecrypt depths: " + i);
        }
        return decryptedLinks;
    }

    /**
     * Bringt alle links in data in eine einheitliche Form
     */
    private void reformDataString() {
        if (data != null) {
            final String tmp = HTMLParser.getHttpLinkList(data);
            if (!(tmp.length() == 0)) {
                data = tmp;
            }
        }
    }

    @Override
    public void run() {
        /*
         * check if there are any links (we need at least a domain and
         * protocoll)
         */
        if (data == null || data.length() == 0 || !new Regex(data, "//.*?\\.").matches()) return;
        ArrayList<DownloadLink> links = findLinks();

        if (links.isEmpty() && !disableDeepEmergencyScan) {
            final String[] ls = HTMLParser.getHttpLinks(data, null);
            if (ls.length > 0) {
                String txt = "\r\n";
                for (final String l : ls) {
                    txt += l + "\r\n";
                }
                LOG.warning("No supported links found -> search for links in source code of all urls");

                final String title = JDL.L("gui.dialog.deepdecrypt.title", "Deep decryption?");
                final String message = JDL.LF("gui.dialog.deepdecrypt.message", "JDownloader has not found anything on %s\r\n-------------------------------\r\nJD now loads this page to look for further links.", txt);
                final int res = UserIO.getInstance().requestConfirmDialog(0, title, message, JDTheme.II("gui.images.search", 32, 32), JDL.L("gui.btn_continue", "Continue"), null);

                if (JDFlags.hasAllFlags(res, UserIO.RETURN_OK)) {
                    data = getLoadLinkString(data);
                    links = findLinks();
                }
            }

        }
        Collections.sort(links);
        LinkGrabberController.getInstance().addLinks(links, hideGrabber, autostart);
    }

    public String getRestData() {
        return data;
    }

    private static String getLoadLinkString(final String linkstring) {
        final StringBuffer sb = new StringBuffer();
        final String[] links = HTMLParser.getHttpLinks(linkstring, null);
        final ProgressController pc = new ProgressController(JDL.LF("gui.addurls.progress", "Parse %s URL(s)", links.length), links.length, null);
        int count = 0;

        for (final String l : links) {
            final Browser br = new Browser();

            try {
                new URL(l);
                pc.setStatusText(JDL.LF("gui.addurls.progress.get", "Parse %s URL(s). Get %s links", links.length, l));

                br.getPage(l);

                final String[] found = HTMLParser.getHttpLinks(br.toString(), l);
                for (final String f : found) {
                    count++;
                    sb.append("\r\n" + f);
                }
            } catch (Exception e1) {
            }
            pc.setStatusText(JDL.LF("gui.addurls.progress.found", "Parse %s URL(s). Found %s links", links.length, count));
            pc.increase(1);

        }
        JDLogger.getLogger().info("Found Links " + sb);
        pc.doFinalize(2000);
        return sb.toString();
    }

}