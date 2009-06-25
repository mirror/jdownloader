package jd.utils.locale;

import java.io.Serializable;

import jd.utils.JDGeoCode;

public class JDLocale implements Serializable {
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
        return this.getLngGeoCode().equals(((JDLocale)l).getLngGeoCode());
    }
}
