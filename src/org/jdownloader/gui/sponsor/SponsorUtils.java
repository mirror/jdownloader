package org.jdownloader.gui.sponsor;

public class SponsorUtils {
    private final static Sponsor SPONSOR = new BannerRotation();

    public static Sponsor getSponsor() {
        return SPONSOR;
    }
}
