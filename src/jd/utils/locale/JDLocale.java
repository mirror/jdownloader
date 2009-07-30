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

import javax.swing.ImageIcon;

import jd.config.container.JDLabelContainer;
import jd.nutils.JDImage;
import jd.utils.JDGeoCode;

public class JDLocale implements Serializable, JDLabelContainer {
    /**
     * 
     */
    private static final long serialVersionUID = 1116656232817008992L;

    private String[] codes;

    private String lngGeoCode;

    public JDLocale(String lngGeoCode) {
        this.lngGeoCode = lngGeoCode;
        codes = JDGeoCode.parseLanguageCode(lngGeoCode);
    }

    public String getCountryCode() {

        return codes[1];
    }

    public String getExtensionCode() {

        return codes[1];
    }

    public String getLanguageCode() {
        return codes[0];
    }

    public String getLngGeoCode() {
        return lngGeoCode;
    }

    @Override
    public String toString() {
        return JDGeoCode.toLongerNative(lngGeoCode);
    }

    public boolean equals(Object l) {
        if (l == null || !(l instanceof JDLocale)) return false;
        return this.getLngGeoCode().equals(((JDLocale) l).getLngGeoCode());
    }

    public ImageIcon getIcon() {
        try {
            ImageIcon img;
            if (lngGeoCode.length() > 2) {
                img = JDImage.getImageIcon("default/flags/" + lngGeoCode.substring(0, 2));
            } else {
                img = JDImage.getImageIcon("default/flags/" + lngGeoCode);
            }
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getLabel() {
        return toString();
    }
}
