package jd.controlling;

import java.net.URLDecoder;
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
        if (pluginsForSearch == null) return new Vector<DownloadLink>();
        Vector<DownloadLink> links = new Vector<DownloadLink>();
        Vector<String> cryptedLinks = new Vector<String>();
        // Array weil an pos 1 und 2 passwort und comment stehen können
        Vector<String[]> decryptedLinks = new Vector<String[]>();
        String foundpassword = Plugin.findPassword(data);
        PluginForDecrypt pDecrypt;
        PluginForHost pHost;
        PluginForSearch pSearch;
        // Zuerst wird data durch die Such Plugins geschickt.

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
        // Sucht alle Links und gibt ein Formatierte Liste zurück. das macht es
        // den Plugin entwicklern einfacher

        data = Plugin.getHttpLinkList(data);

        try {
            this.data = URLDecoder.decode(this.data, "UTF-8");
        }
        catch (Exception e) {
            logger.warning("text not url decodeable");
        }

        // logger.info("Eingefügt: " + data);
        // Zuerst wird überprüft, ob ein Decrypt-Plugin einen Teil aus der
        // Zwischenablage entschlüsseln kann. Ist das der Fall, wird die
        // entsprechende Stelle
        // verarbeitet und gelöscht, damit sie keinesfalls nochmal verarbeitet
        // wird.
        for (int i = 0; i < pluginsForDecrypt.size(); i++) {
            pDecrypt = pluginsForDecrypt.get(i);
            if (pDecrypt.canHandle(data)) {
                //Es wird eine neue instanz erstellt. Jeder decryptvorgang brauchts eine eigene instanz da decryptvorgänge auch paralell laufen können.
                try {
                    pDecrypt = pDecrypt.getClass().newInstance();

                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE, pDecrypt));
                    cryptedLinks.addAll(pDecrypt.getDecryptableLinks(data));
                    data = pDecrypt.cutMatches(data);

                    decryptedLinks.addAll(pDecrypt.decryptLinks(cryptedLinks));

                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE, pDecrypt));
                }
                catch (InstantiationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        // Die entschlüsselten Links werden nochmal durch alle DecryptPlugins
        // geschickt.
        // Könnte sein, daß einige zweifach oder mehr verschlüsselt sind
        boolean moreToDo;
        do {
            moreToDo = false;
            logger.info("Size: " + pluginsForDecrypt.size());
            for (int i = 0; i < pluginsForDecrypt.size(); i++) {
                pDecrypt = pluginsForDecrypt.get(i);
                Iterator<String[]> iterator = decryptedLinks.iterator();
                while (iterator.hasNext()) {
                    String[] data = iterator.next();
                    String localData = Plugin.getHttpLinkList(data[0]);

                    try {
                        localData = URLDecoder.decode(localData, "UTF-8");
                    }
                    catch (Exception e) {
                        logger.warning("text not url decodeable");
                    }

                    if (pDecrypt.canHandle(localData)) {
                        moreToDo = true;

                        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE, pDecrypt));
                        // logger.info("decryptedLink removed
                        // "+data+">>"+pDecrypt.getHost());
                        // Schleift die Passwörter und COmments durch den
                        // Nächsten Decrypter. (bypass)
                        iterator.remove();

                        cryptedLinks.addAll(pDecrypt.getDecryptableLinks(localData));
                        localData = pDecrypt.cutMatches(localData);

                        Vector<String[]> tmpLinks = pDecrypt.decryptLinks(cryptedLinks);
                        String password = data[1] == null ? "" : data[1];
                        String comment = data[2] == null ? "" : data[2];
                        logger.info("p " + password);
                        for (int ii = 0; ii < tmpLinks.size(); ii++) {

                            if (tmpLinks.get(ii)[1] != null) {
                                tmpLinks.get(ii)[1] = password + "|" + tmpLinks.get(ii)[1];
                                while (tmpLinks.get(ii)[1].startsWith("|"))
                                    tmpLinks.get(ii)[1] = tmpLinks.get(ii)[1].substring(1);
                            }
                            else {
                                tmpLinks.get(ii)[1] = password;
                            }
                            if (tmpLinks.get(ii)[2] != null) {
                                tmpLinks.get(ii)[2] = comment + "|" + tmpLinks.get(ii)[2];
                                while (tmpLinks.get(ii)[2].startsWith("|"))
                                    tmpLinks.get(ii)[2] = tmpLinks.get(ii)[2].substring(1);
                            }
                            else {
                                tmpLinks.get(ii)[2] = comment;
                            }
                        }

                        decryptedLinks.addAll(tmpLinks);

                        iterator = decryptedLinks.iterator();
                        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE, pDecrypt));
                    }
                }
            }
        }
        while (moreToDo);

        // Danach wird der (noch verbleibende) Inhalt der Zwischenablage an die
        // Plugins der Hoster geschickt.
        for (int i = 0; i < pluginsForHost.size(); i++) {
            pHost = pluginsForHost.get(i);
            if (pHost.canHandle(data)) {
                Vector<DownloadLink> dl = pHost.getDownloadLinks(data);
                if (!foundpassword.matches("[\\s]*")) {
                    for (int j = 0; j < dl.size(); j++) {
                        DownloadLink da = dl.get(j);
                        da.setSourcePluginPassword(foundpassword);
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

        Iterator<String[]> iterator = decryptedLinks.iterator();

        while (iterator.hasNext()) {
            String[] decrypted = iterator.next();

            // logger.info("link: " + decrypted);
            for (int i = 0; i < pluginsForHost.size(); i++) {
                try {
                    pHost = pluginsForHost.get(i);
                    if (pHost.canHandle(decrypted[0])) {
                        Vector<DownloadLink> dLinks = pHost.getDownloadLinks(decrypted[0]);
                        for (int c = 0; c < dLinks.size(); c++) {

                            dLinks.get(c).setSourcePluginPassword((decrypted[1] == null) ? foundpassword : decrypted[1]);
                            dLinks.get(c).setSourcePluginComment(decrypted[2]);

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
}