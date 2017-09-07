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

import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Crypted Link festgehalten.
 *
 * @author jiaz
 */
public class CryptedLink {
    /**
     * enthält die Url, welche an das Decrypter-Plugin übergeben wird
     */
    private String cryptedUrl;
    // Password welches dem Decrypter-Plugin übergeben wird (zb FolderPassword)
    private String decrypterPassword;

    public CryptedLink(String cryptedUrl) {
        this(cryptedUrl, null);
    }

    private LazyCrawlerPlugin lazyC = null;

    public LazyCrawlerPlugin getLazyC() {
        return lazyC;
    }

    public void setLazyC(LazyCrawlerPlugin lazyC) {
        this.lazyC = lazyC;
    }

    public CryptedLink(String cryptedUrl, String pw) {
        this.cryptedUrl = cryptedUrl;
        this.decrypterPassword = pw;
    }

    /**
     * Gibt die CryptedUrl zurück, welche vom Decrypter-Plugin verarbeitet wird
     */
    public String getCryptedUrl() {
        return this.cryptedUrl;
    }

    /**
     * Setzt die CryptedUrl zurück, welche vom Decrypter-Plugin verarbeitet wird
     */
    public void setCryptedUrl(final String url) {
        this.cryptedUrl = url;
    }

    /**
     * Gibt das Password zurück, welches vom Decrypter-Plugin genutzt werden kann (zb. FolderPassword)
     */
    public String getDecrypterPassword() {
        return this.decrypterPassword;
    }

    /**
     * Setzt das Password, welches vom Decrypter-Plugin genutzt werden kann (zb. FolderPassword)
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
}
