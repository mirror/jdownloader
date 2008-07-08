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

package jd.plugins;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import jd.parser.Regex;

import jd.config.MenuItem;
import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die Links entschlüsseln können
 * 
 * @author astaldo
 */
@SuppressWarnings("unchecked")
public abstract class PluginForDecrypt extends Plugin implements Comparable {
    protected ProgressController progress;

    /**
     * Diese Methode entschlüsselt Links.
     * 
     * @param cryptedLinks
     *            Ein Vector, mit jeweils einem verschlüsseltem Link. Die
     *            einzelnen verschlüsselten Links werden aufgrund des Patterns
     *            {@link jd.plugins.Plugin#getSupportedLinks() getSupportedLinks()}
     *            herausgefiltert
     * @return Ein Vector mit Klartext-links
     */

    public PluginForDecrypt() {

    }

    public Vector<DownloadLink> decryptLinks(String[] cryptedLinks) {
        this.fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, cryptedLinks);
        Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
        	for (int i = 0; i < cryptedLinks.length; i++) {
                decryptedLinks.addAll(decryptLink(cryptedLinks[i]));
			}

        this.fireControlEvent(ControlEvent.CONTROL_PLUGIN_INACTIVE, decryptedLinks);

        return decryptedLinks;
    }

    private String cryptedLink = null;

    // private String decrypterDefaultPassword = null;

    // private String decrypterDefaultComment = null;

    protected Vector<String> default_password = new Vector<String>();;

    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    /**
     * Die Methode entschlüsselt einen einzelnen Link. Alle steps werden
     * durchlaufen. Der letzte step muss als parameter einen Vector<String> mit
     * den decoded Links setzen
     * 
     * @param cryptedLink
     *            Ein einzelner verschlüsselter Link
     * 
     * @return Ein Vector mit Klartext-links
     */
    public Vector<DownloadLink> decryptLink(String cryptedLink) {
        this.cryptedLink = cryptedLink;
        if (progress != null && !progress.isFinished()) {
            progress.finalize();
            logger.warning(" Progress ist besetzt von " + progress);
        }

        progress = new ProgressController("Decrypter: " + this.getLinkName());
        progress.setStatusText("decrypt-" + getPluginName() + ": " + this.getLinkName());
        PluginStep step = null;

        while ((step = nextStep(step)) != null) {
            doStep(step, cryptedLink);

            if (nextStep(step) == null) {
                logger.info("decrypting finished");
                Object tmpLinks = step.getParameter();

                if (tmpLinks == null || !(tmpLinks instanceof Vector)) {
                    logger.severe("ACHTUNG1 Decrypt Plugins müssen im letzten schritt einen  Vector<DownloadLink>");

                    progress.finalize();
                    return new Vector<DownloadLink>();
                }
                Vector links = (Vector) tmpLinks;

                if (links.size() == 0) {

                    progress.finalize();
                    return new Vector<DownloadLink>();
                }
                // Vector<DownloadLink> decryptedLinks = new
                // Vector<DownloadLink>();
                String link;
                try {
                    if (links.get(0) instanceof DownloadLink) {

                        for (int i = links.size() - 1; i >= 0; i--) {
                            DownloadLink dl = (DownloadLink) links.get(i);
                            link = JDUtilities.htmlDecode(dl.getDownloadURL());
                            dl.setUrlDownload(link);

                            if (dl.getSourcePluginPasswords() == null || dl.getSourcePluginPasswords().size() == 0) {
                                dl.setSourcePluginPasswords(this.getDefaultPassswords());
                            }
                            if (link == null || link.trim().equalsIgnoreCase(cryptedLink.trim())) {
                                links.remove(i);
                            } else {

                            }

                        }

                        progress.finalize();

                        return (Vector<DownloadLink>) links;
                    } else {
                        logger.severe("ACHTUNG2 Decrypt Plugins müssen im letzten schritt einen  Vector<DownloadLink>");

                        progress.finalize();
                        return new Vector<DownloadLink>();
                    }

                } catch (Exception e) {

                    e.printStackTrace();

                }

                progress.finalize();

            }
        }

        progress.finalize();
        return new Vector<DownloadLink>();

    }

    protected DownloadLink createDownloadlink(String link) {
        DownloadLink dl = new DownloadLink(this, null, this.getHost(), JDUtilities.htmlDecode(link), true);
        return dl;
    }

    /**
     * Sucht in data nach allen passenden links und gibt diese als vektor zurück
     * 
     * @param data
     * @return
     */
    public String[] getDecryptableLinks(String data) {
//        Vector<String> hits = SimpleMatches.getMatches(data, getSupportedLinks());
        String[] hits = new Regex(data, getSupportedLinks()).getMatches(0);
        if (hits != null && hits.length > 0) {

            for (int i = hits.length - 1; i >= 0; i--) {
                String file = hits[i];
                while (file.charAt(0) == '"')
                    file = file.substring(1);
                while (file.charAt(file.length() - 1) == '"')
                    file = file.substring(0, file.length() - 1);
                hits[i]=file;

            }
        }
        return hits;
    }

    /**
     * Diese Methode arbeitet die unterschiedlichen schritte ab. und gibt den
     * gerade abgearbeiteten Schritt jeweisl zurück.
     * 
     * @param step
     * @param parameter
     * @return gerade abgeschlossener Schritt
     */
    public abstract PluginStep doStep(PluginStep step, String parameter);

    public Vector<String> getDefaultPassswords() {
        return default_password;

    }

    /**
     * Deligiert den doStep Call weiter und ändert dabei nur den parametertyp.
     */
    public PluginStep doStep(PluginStep step, Object parameter) {
        return doStep(step, (String) parameter);
    }

    /**
     * Gibt den namen des internen CryptedLinks zurück
     * 
     * @return encryptedLink
     */

    public String getLinkName() {
        if (cryptedLink == null) return "";
        try {
            return new URL(cryptedLink).getFile();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * vergleicht Decryptplugins anhand des Hostnamens wird zur Sortierung
     * benötigt
     */
    public int compareTo(Object o) {
        return getHost().toLowerCase().compareTo(((PluginForDecrypt) o).getHost().toLowerCase());
    }

}
