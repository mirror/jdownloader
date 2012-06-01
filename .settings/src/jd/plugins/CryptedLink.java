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

import jd.config.Property;
import jd.controlling.ProgressController;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Crypted Link
 * festgehalten.
 * 
 * @author jiaz
 */
public class CryptedLink extends Property {
    private static final long  serialVersionUID = 6493927031856751251L;

    /**
     * enthält die Url, welche an das Decrypter-Plugin übergeben wird
     */
    private String             cryptedUrl;

    private ProgressController progress;

    // Password welches dem Decrypter-Plugin übergeben wird (zb FolderPassword)
    private String             decrypterPassword;

    private PluginForDecrypt   plugin           = null;

    private DownloadLink       link             = null;

    public CryptedLink(String cryptedUrl) {
        // this.cryptedUrl = cryptedUrl;
        // this.decrypterPassword = null;
        // this.progress = null;
        this(cryptedUrl, null);
    }

    public CryptedLink(String cryptedUrl, String pw) {
        if (cryptedUrl != null) {
            this.cryptedUrl = new String(cryptedUrl);
        } else {
            this.cryptedUrl = null;
        }
        this.decrypterPassword = pw;
        this.progress = null;
    }

    public CryptedLink(DownloadLink link) {
        this.link = link;
    }

    public void setProgressController(final ProgressController progress) {
        this.progress = progress;
    }

    public ProgressController getProgressController() {
        return this.progress;
    }

    /**
     * Gibt die CryptedUrl zurück, welche vom Decrypter-Plugin verarbeitet wird
     */
    public String getCryptedUrl() {
        return this.cryptedUrl;
    }

    public DownloadLink getDecryptedLink() {
        return this.link;
    }

    /**
     * Setzt die CryptedUrl zurück, welche vom Decrypter-Plugin verarbeitet wird
     */
    public void setCryptedUrl(final String url) {
        this.cryptedUrl = url;
    }

    /**
     * Gibt das Password zurück, welches vom Decrypter-Plugin genutzt werden
     * kann (zb. FolderPassword)
     */
    public String getDecrypterPassword() {
        return this.decrypterPassword;
    }

    /**
     * Setzt das Password, welches vom Decrypter-Plugin genutzt werden kann (zb.
     * FolderPassword)
     */
    public void setDecrypterPassword(final String pw) {
        this.decrypterPassword = pw;
    }

    /**
     * Gibt die CryptedUrl zurück, welche vom Decrypter-Plugin verarbeitet wird
     */
    // @Override
    public String toString() {
        return this.cryptedUrl;
    }

    /**
     * @param plugin
     *            the plugin to set
     */
    public void setPlugin(PluginForDecrypt plugin) {
        this.plugin = plugin;
    }

    /**
     * @return the plugin
     */
    public PluginForDecrypt getPlugin() {
        return plugin;
    }
}
