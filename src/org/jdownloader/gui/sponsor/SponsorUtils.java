package org.jdownloader.gui.sponsor;


public class SponsorUtils {
    private static Sponsor SPONSOR;
    static {

        SPONSOR = UploadedController.getInstance();
        ((UploadedController) SPONSOR).start();

    }

    public static Sponsor getSponsor() {

        return SPONSOR;
    }
}
