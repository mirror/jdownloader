package jd.controlling;

import java.net.URLDecoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.event.ControlEvent;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginForSearch;
import jd.utils.JDUtilities;

/**
 * Diese Klasse läuft in einem Thread und verteilt den Inhalt der Zwischenablage
 * an (unter Umständen auch mehrere) Plugins Die gefundenen Treffer werden
 * ausgeschnitten.
 * 
 * @author astaldo
 */
public class DistributeData extends ControlMulticaster {
    /**
     * Der Logger
     */
    private static Logger            logger = JDUtilities.getLogger();

    /**
     * Die zu verteilenden Daten
     */
    private String                   data;

    private Vector<DownloadLink>     linkData;

    /**
     * Plugins der Anbieter
     */
    private Vector<PluginForHost>    pluginsForHost;

    /**
     * Plugins zum Entschlüsseln
     */
    private Vector<PluginForDecrypt> pluginsForDecrypt;

    private Vector<PluginForSearch>  pluginsForSearch;

    /**
     * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll. Die
     * übergebenen Daten werden durch einen URLDecoder geschickt.
     * 
     * @param data Daten, die verteilt werden sollen
     */
    public DistributeData(String data) {
        super("JD-DistributeData");
        this.data = data;
        this.pluginsForHost = JDUtilities.getPluginsForHost();
        this.pluginsForDecrypt = JDUtilities.getPluginsForDecrypt();
        this.pluginsForSearch = JDUtilities.getPluginsForSearch();
        try {
            // this.data = URLDecoder.decode(this.data, "UTF-8");
        }
        catch (Exception e) {
            logger.warning("text not url decodeable");
        }
    }

    public void run() {
        Vector<DownloadLink> links = findLinks();

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED, links));
    }

    /**
     * Ermittelt über die Plugins alle Passenden Links und gibt diese in einem
     * Vector zurück
     * 
     * @return link-Vector
     */
    public Vector<DownloadLink> findLinks() {

        Vector<DownloadLink> links = new Vector<DownloadLink>();

        Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
        if (pluginsForHost == null) return decryptedLinks;
        String foundpassword = Plugin.findPassword(data);

        PluginForHost pHost;

        // Zuerst wird data durch die Such Plugins geschickt.
        decryptedLinks.addAll(handleSearchPlugins());

        reformDataString();

        decryptedLinks.addAll(handleDecryptPlugins());

        // decrypted Links werden solange an einen neuen Distributor geschickt
        // bis keine links mehr decrypted werden.

        // Die entschlüsselten Links werden nochmal durch alle DecryptPlugins
        // geschickt.
        // Könnte sein, daß einige zweifach oder mehr verschlüsselt sind
        // cryptedLinks = new Vector<String>();
        // boolean moreToDo;
        // do {
        // moreToDo = false;
        // logger.info("Size: " + pluginsForDecrypt.size());
        // for (int i = 0; i < pluginsForDecrypt.size(); i++) {
        // pDecrypt = pluginsForDecrypt.get(i);
        // Iterator<String[]> iterator = decryptedLinks.iterator();
        // while (iterator.hasNext()) {
        // String[] data = iterator.next();
        // String localData = Plugin.getHttpLinkList(data[0]);
        //
        // try {
        // localData = URLDecoder.decode(localData, "UTF-8");
        // }
        // catch (Exception e) {
        // logger.warning("text not url decodeable");
        // }
        //
        // if (pDecrypt.canHandle(localData)) {
        // logger.info("dec2 link"+i+" : "+data[0]+" - "+localData);
        // moreToDo = true;
        //
        // fireControlEvent(new ControlEvent(this,
        // ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE, pDecrypt));
        // // logger.info("decryptedLink removed
        // // "+data+">>"+pDecrypt.getHost());
        // // Schleift die Passwörter und COmments durch den
        // // Nächsten Decrypter. (bypass)
        // iterator.remove();
        //
        // cryptedLinks.addAll(pDecrypt.getDecryptableLinks(localData));
        // localData = pDecrypt.cutMatches(localData);
        // logger.info("send 2:"+cryptedLinks);
        // Vector<String[]> tmpLinks = pDecrypt.decryptLinks(cryptedLinks);
        // String password = data[1] == null ? "" : data[1];
        // String comment = data[2] == null ? "" : data[2];
        // logger.info("p " + password+" links: "+tmpLinks);
        // for (int ii = 0; ii < tmpLinks.size(); ii++) {
        //
        // if (tmpLinks.get(ii)[1] != null) {
        // tmpLinks.get(ii)[1] = password + "|" + tmpLinks.get(ii)[1];
        // while (tmpLinks.get(ii)[1].startsWith("|"))
        // tmpLinks.get(ii)[1] = tmpLinks.get(ii)[1].substring(1);
        // }
        // else {
        // tmpLinks.get(ii)[1] = password;
        // }
        // if (tmpLinks.get(ii)[2] != null) {
        // tmpLinks.get(ii)[2] = comment + "|" + tmpLinks.get(ii)[2];
        // while (tmpLinks.get(ii)[2].startsWith("|"))
        // tmpLinks.get(ii)[2] = tmpLinks.get(ii)[2].substring(1);
        // }
        // else {
        // tmpLinks.get(ii)[2] = comment;
        // }
        // }
        //
        // decryptedLinks.addAll(tmpLinks);
        //
        // iterator = decryptedLinks.iterator();
        // fireControlEvent(new ControlEvent(this,
        // ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE, pDecrypt));
        // }
        // }
        // }
        // }
        // while (moreToDo);

        // Danach wird der (noch verbleibende) Inhalt der Zwischenablage an die
        // Plugins der Hoster geschickt.
        for (int i = 0; i < pluginsForHost.size(); i++) {
            pHost = pluginsForHost.get(i);
            if (pHost.canHandle(data)) {
                Vector<DownloadLink> dl = pHost.getDownloadLinks(data);
                if (!foundpassword.matches("[\\s]*")) {
                    for (int j = 0; j < dl.size(); j++) {
                        DownloadLink da = dl.get(j);
                        da.addSourcePluginPassword(foundpassword);
                        // TODO:Das erneute setzen sollte unnötig sein
                        dl.set(j, da);
                    }
                }
                links.addAll(dl);
                data = pHost.cutMatches(data);
            }
        }

        // Als letztes werden die entschlüsselten Links (soweit überhaupt
        // vorhanden)
        // an die HostPlugins geschickt, damit diese einen Downloadlink
        // erstellen können

        // Edit Coa:
        // Hier werden auch die SourcePLugins in die Downloadlinks gesetzt

        Iterator<DownloadLink> iterator = decryptedLinks.iterator();

        while (iterator.hasNext()) {
            DownloadLink decrypted = iterator.next();

            // logger.info("link: " + decrypted);
            for (int i = 0; i < pluginsForHost.size(); i++) {
                try {
                    pHost = pluginsForHost.get(i);
                    if (pHost.canHandle(decrypted.getUrlDownloadDecrypted())) {
                        Vector<DownloadLink> dLinks = pHost.getDownloadLinks(decrypted.getUrlDownloadDecrypted());
                        for (int c = 0; c < dLinks.size(); c++) {

                            dLinks.get(c).addSourcePluginPassword(foundpassword);
                            dLinks.get(c).addSourcePluginPasswords(decrypted.getSourcePluginPasswords());
                            dLinks.get(c).setSourcePluginComment(decrypted.getSourcePluginComment());

                        }

                        links.addAll(dLinks);
                        iterator.remove();
                    }
                }
                catch (Exception e) {
                    logger.severe("Decrypter/Search Fehler: " + e.getMessage());
                }
            }
        }
        // logger.info("--> " + links);

        return links;
    }

    private Vector<DownloadLink> handleDecryptPlugins() {
        PluginForDecrypt pDecrypt;
        Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
        if (pluginsForDecrypt == null) return decryptedLinks;
        for (int i = 0; i < pluginsForDecrypt.size(); i++) {
            pDecrypt = pluginsForDecrypt.get(i);
            if (pDecrypt.canHandle(data)) {

                try {
                    pDecrypt = pDecrypt.getClass().newInstance();

                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE, pDecrypt));
                    Vector<String> decryptableLinks = pDecrypt.getDecryptableLinks(data);
                    data = pDecrypt.cutMatches(data);

                    decryptedLinks.addAll(pDecrypt.decryptLinks(decryptableLinks));

                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE, pDecrypt));
                }
                catch (Exception e) {

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
     * Sucht in dem übergebenen vector nach weiteren decryptbaren Links und
     * decrypted diese
     * 
     * @param decryptedLinks
     * @return
     */
    private boolean deepDecrypt(Vector<DownloadLink> decryptedLinks) {
        if (decryptedLinks.size() == 0) return false;
        boolean hasDecryptedLinks = false;
        for (int i = decryptedLinks.size() - 1; i >= 0; i--) {
            DownloadLink link = decryptedLinks.get(i);
            String url = link.getUrlDownloadDecrypted();

            if (url != null) {
                url = Plugin.getHttpLinkList(url);

                try {
                    url = URLDecoder.decode(url, "UTF-8");
                }
                catch (Exception e) {
                    logger.warning("text not url decodeable");
                }
            }
            PluginForDecrypt pDecrypt;

            boolean canDecrypt = false;

            for (int d = 0; d < pluginsForDecrypt.size(); d++) {
                pDecrypt = pluginsForDecrypt.get(d);
                if (pDecrypt.canHandle(url)) {
                    try {
                        pDecrypt = pDecrypt.getClass().newInstance();

                        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE, pDecrypt));

                        Vector<String> decryptableLinks = pDecrypt.getDecryptableLinks(url);
                        url = pDecrypt.cutMatches(url);
                        Vector<DownloadLink> links = pDecrypt.decryptLinks(decryptableLinks);
                        logger.info("Got links: " + links);

                        // Reicht die passwörter weiter
                        for (int t = 0; t < links.size(); t++) {
                            links.get(t).addSourcePluginPasswords(link.getSourcePluginPasswords());
                        }
                        decryptedLinks.addAll(links);

                        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE, pDecrypt));
                        canDecrypt = true;
                    }
                    catch (Exception e) {

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
     * Bringt alle links in data in eine einheitliche Form
     */
    private void reformDataString() {
        if (data != null) {
            data = Plugin.getHttpLinkList(data);

            try {
                this.data = URLDecoder.decode(this.data, "UTF-8");
            }
            catch (Exception e) {
                logger.warning("text not url decodeable");
            }
        }
    }

    /**
     * Suceht im datastring nach Suchpatterns und schreibt gibt die treffer
     * zurück
     * 
     * @param decryptedLinks
     */
    private Vector<DownloadLink> handleSearchPlugins() {
        if (pluginsForSearch == null) return new Vector<DownloadLink>();
        Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
        if (data != null) {
            PluginForSearch pSearch;
            for (int i = 0; i < pluginsForSearch.size(); i++) {
                pSearch = pluginsForSearch.get(i);
                logger.info("engine:" + pSearch.getPluginName());
                if (pSearch.canHandle(data)) {
                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_SEARCH_ACTIVE, pSearch));

                    decryptedLinks.addAll(pSearch.findLinks(data));

                    // data = pSearch.cutMatches(data);
                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_SEARCH_INACTIVE, pSearch));
                }
            }
        }
        return decryptedLinks;
    }

    public Vector<DownloadLink> getLinkData() {
        return linkData;
    }

    public void setLinkData(Vector<DownloadLink> linkData) {
        this.linkData = linkData;
    }
}