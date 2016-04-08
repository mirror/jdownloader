package org.jdownloader.plugins.components.youtube;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;
import jd.plugins.decrypter.YoutubeHelper;

public enum VideoResolution {
    P_1080(1080, 1080, 1),
    P_144(144, 144, 1),
    P_1440(1440, 1440, 1),
    P_2160(2160, 2160, 1),
    P_2160_ESTIMATED(2160, 2160 - 1, 1),
    P_240(240, 240, 1),
    P_270(270, 270, 1),
    P_360(360, 360, 1),
    P_480(480, 480, 1),
    P_720(720, 720, 1),
    P_4320(4320, 4320, 1),
    P_72(72, 72, 1);
    private double rating = -1;
    private int    height;

    private VideoResolution(int height, double rating, double modifier) {
        this.rating = rating / modifier;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getLabel(Object caller) {
        if (caller != null) {
            DownloadLink link = null;
            if (caller instanceof CrawledLink) {
                link = ((CrawledLink) caller).getDownloadLink();
            } else if (caller instanceof DownloadLink) {
                link = (DownloadLink) caller;
            }
            if (link != null) {
                YoutubeFinalLinkResource r = link.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, YoutubeFinalLinkResource.TYPE_REF);
                if (r != null && r.getHeight() > 0) {
                    if (r.getItag().getVideoResolution(caller) == this && r.getHeight() != height) {
                        System.out.println(this + " - " + r.getItag());
                        return r.getHeight() + "p";
                    }
                }
            }
        }
        return height + "p";
    }

}
