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

import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Crypted Link festgehalten.
 *
 * @author jiaz
 */
public class CryptedLink {
    private String cryptedURL        = null;
    // Password welches dem Decrypter-Plugin übergeben wird (zb FolderPassword)
    private String decrypterPassword = null;
    private Object source            = null;

    public CryptedLink(String url, Object source) {
        cryptedURL = url;
        this.source = source;
    }

    public CryptedLink(Object source) {
        this.source = source;
    }

    public void setSourceLink(Object source) {
        this.source = source;
    }

    private LazyCrawlerPlugin lazyC = null;

    public LazyCrawlerPlugin getLazyC() {
        return lazyC;
    }

    public void setLazyC(LazyCrawlerPlugin lazyC) {
        this.lazyC = lazyC;
    }

    public DownloadLink getDownloadLink() {
        if (source instanceof DownloadLink) {
            return (DownloadLink) source;
        } else if (source instanceof CrawledLink) {
            return ((CrawledLink) source).getDownloadLink();
        } else {
            return null;
        }
    }

    public Object getSource() {
        return source;
    }

    /**
     * Gibt die CryptedUrl zurück, welche vom Decrypter-Plugin verarbeitet wird
     */
    public String getCryptedUrl() {
        if (cryptedURL != null) {
            return cryptedURL;
        } else if (source instanceof CrawledLink) {
            return ((CrawledLink) source).getURL();
        } else if (source instanceof DownloadLink) {
            return ((DownloadLink) source).getPluginPatternMatcher();
        } else if (source instanceof String) {
            return (String) source;
        } else {
            return null;
        }
    }

    /**
     * Setzt die CryptedUrl zurück, welche vom Decrypter-Plugin verarbeitet wird
     */
    public void setCryptedUrl(final String url) {
        this.cryptedURL = url;
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
        return "Plugin:" + getLazyC() + "|URL:" + getCryptedUrl();
    }
}
