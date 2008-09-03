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

import java.awt.Color;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import jd.config.MenuItem;
import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;

/**
 * Dies ist die Oberklasse für alle Plugins, die Links entschlüsseln können
 * 
 * @author astaldo
 */
public abstract class PluginForDecrypt extends Plugin {
    private CryptedLink cryptedLink = null;

    protected ProgressController progress;

    protected Browser br;

    /**
     * Diese Methode entschlüsselt Links.
     * 
     * @param cryptedLinks
     *            Ein Vector, mit jeweils einem verschlüsseltem Link. Die
     *            einzelnen verschlüsselten Links werden aufgrund des Patterns
     *            {@link jd.plugins.Plugin#getSupportedLinks()
     *            getSupportedLinks()} herausgefiltert
     * @return Ein Vector mit Klartext-links
     */

    public PluginForDecrypt() {
        br = new Browser();
    }

    protected DownloadLink createDownloadlink(String link) {
        DownloadLink dl = new DownloadLink(null, null, getHost(), Encoding.htmlDecode(link), true);
        return dl;
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    /**
     * Die Methode entschlüsselt einen einzelnen Link.
     */
    public abstract ArrayList<DownloadLink> decryptIt(CryptedLink parameter) throws Exception;

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
    public ArrayList<DownloadLink> decryptLink(CryptedLink cryptedLink) {
        this.cryptedLink = cryptedLink;
        if (progress != null && !progress.isFinished()) {
            progress.finalize();
            logger.warning(" Progress ist besetzt von " + progress);
        }

        progress = new ProgressController("Decrypter: " + getLinkName());
        progress.setStatusText("decrypt-" + getPluginName() + ": " + getLinkName());
        ArrayList<DownloadLink> tmpLinks = null;
        try {
            tmpLinks = decryptIt(cryptedLink);
        } catch (DecrypterException e) {
            tmpLinks = new ArrayList<DownloadLink>();
            progress.setStatusText(this.getHost() + ": " + e.getErrorMessage());
            progress.setColor(Color.RED);
            progress.finalize(15000l);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (tmpLinks == null) {
            logger.severe("Decrypter out of date: " + this);
            progress.setStatusText("Decrypter out of date: " + this.getHost());
            progress.setColor(Color.RED);
            progress.finalize(15000l);
            return new ArrayList<DownloadLink>();
        }

        if (tmpLinks.size() == 0) {
            progress.finalize();
            return new ArrayList<DownloadLink>();
        }

        progress.finalize();
        return tmpLinks;
    }

    public ArrayList<DownloadLink> decryptLinks(CryptedLink[] cryptedLinks) {
        fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, cryptedLinks);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        for (CryptedLink element : cryptedLinks) {
            decryptedLinks.addAll(decryptLink(element));
        }

        fireControlEvent(ControlEvent.CONTROL_PLUGIN_INACTIVE, decryptedLinks);

        return decryptedLinks;
    }

    /**
     * Sucht in data nach allen passenden links und gibt diese als vektor zurück
     * 
     * @param data
     * @return
     */
    public CryptedLink[] getDecryptableLinks(String data) {
        String[] hits = new Regex(data, getSupportedLinks()).getColumn(-1);
        ArrayList<CryptedLink> chits = new ArrayList<CryptedLink>();
        if (hits != null && hits.length > 0) {

            for (int i = hits.length - 1; i >= 0; i--) {
                String file = hits[i];
                while (file.charAt(0) == '"') {
                    file = file.substring(1);
                }
                while (file.charAt(file.length() - 1) == '"') {
                    file = file.substring(0, file.length() - 1);
                }
                hits[i] = file;

            }

            for (String hit : hits) {
                chits.add(new CryptedLink(hit));
            }
        }
        return chits.toArray((new CryptedLink[chits.size()]));
    }

    /**
     * Gibt den namen des internen CryptedLinks zurück
     * 
     * @return encryptedLink
     */

    @Override
    public String getLinkName() {
        if (cryptedLink == null) return "";
        try {
            return new URL(cryptedLink.toString()).getFile();
        } catch (MalformedURLException e) {
            return "";
        }
    }

}
