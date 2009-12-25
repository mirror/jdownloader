//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.utils.locale;

import java.io.Serializable;

import jd.utils.JDGeoCode;

/**
 * This class represents a Language and is used by {@link jd.utils.locale.JDL}
 * Locale System
 * 
 * @author unkown
 * 
 */
public class JDLocale implements Serializable {

    private static final long serialVersionUID = 1116656232817008992L;
    /**
     * stores the parsed codes
     * 
     * @see jd.utils.JDGeoCode.parseLanguageCode(String)
     * 
     */
    private String[] codes;

    private String lngGeoCode;

    /**
     * Creates a new JDLocale INstance
     * 
     * @param lngGeoCode
     *            a languagecode like de-AT-custom (language-COUNTRY-extension
     * @see jd.utils.JDGeoCode
     */
    public JDLocale(final String lngGeoCode) {
        this.lngGeoCode = lngGeoCode;

        codes = JDGeoCode.parseLanguageCode(lngGeoCode);
    }

    /**
     * returns the languageCode (e.g. de for german)
     * 
     * @return
     */
    public String getLanguageCode() {
        return codes[0];
    }

    /**
     * Returns the country code or null. (e.g. "AT" for Austria)
     * 
     * @return
     */
    public String getCountryCode() {
        return codes[1];
    }

    @Override
    public String toString() {
        return JDGeoCode.toLonger(lngGeoCode) + " (" + JDGeoCode.toLongerNative(lngGeoCode) + ")";
    }

    @Override
    public boolean equals(Object l) {
        if (l == null || !(l instanceof JDLocale)) return false;
        return this.getLngGeoCode().equals(((JDLocale) l).getLngGeoCode());
    }

    /**
     * @return the {@link JDLocale#lngGeoCode}
     * @see JDLocale#lngGeoCode
     */
    public String getLngGeoCode() {
        return lngGeoCode;
    }

}
