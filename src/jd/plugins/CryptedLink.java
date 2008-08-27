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

import jd.config.Property;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Crypted Link
 * festgehalten.
 * 
 * @author jiaz
 */
public class CryptedLink extends Property {
    private static final long serialVersionUID = 6493927031856751251L;

    /**
     * enthält die Url welche an das Decrypter-Plugin übergeben wird
     */
    private String cryptedUrl;

    /**
     * Password welches dem Decrypter-Plugin übergeben wird (zb FolderPassword)
     */
    private String decrypterPassword;

    public CryptedLink(String cryptedUrl) {
        this.cryptedUrl = cryptedUrl;
        this.decrypterPassword = null;
    }

    public CryptedLink(String cryptedUrl, String pw) {
        this.cryptedUrl = cryptedUrl;
        this.decrypterPassword = pw;
    }

    /**
     * gibt die cryptedUrl zurück, welche vom Decrypter-Plugin verarbeitet wird
     */
    public String getCryptedUrl() {
        return this.cryptedUrl;
    }

    /**
     * setzt die cryptedUrl zurück, welche vom Decrypter-Plugin verarbeitet wird
     */
    public void setCryptedUrl(String url) {
        this.cryptedUrl = url;
    }

    /**
     * gibt das Password zurück, welches vom Decrypter-Plugin genutzt werden
     * kann (zb. FolderPassword)
     */
    public String getDecrypterPassword() {
        return this.decrypterPassword;
    }

    /**
     * setzt das Password, welches vom Decrypter-Plugin genutzt werden kann (zb.
     * FolderPassword)
     */
    public void setDecrypterPassword(String pw) {
        this.decrypterPassword = pw;
    }

    /**
     * gibt die cryptedUrl zurück, welche vom Decrypter-Plugin verarbeitet wird
     */
    @Override
    public String toString() {
        return this.cryptedUrl;
    }
}
