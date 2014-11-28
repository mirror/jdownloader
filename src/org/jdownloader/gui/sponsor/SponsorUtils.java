package org.jdownloader.gui.sponsor;

import java.util.Date;

public class SponsorUtils {
    private static Sponsor SPONSOR;
    static {
        if (System.currentTimeMillis() > new Date(2014 - 1900, 11, 1, 0, 0).getTime()) {
            SPONSOR = UploadedController.getInstance();
            ((UploadedController) SPONSOR).start();
        } else {
            SPONSOR = OboomController.getInstance();
            ((OboomController) SPONSOR).start();
        }
    }

    public static Sponsor getSponsor() {

        return SPONSOR;
    }
}
