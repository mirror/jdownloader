package org.jdownloader.plugins.components.youtube;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;
import jd.plugins.decrypter.YoutubeHelper;

public enum VideoFrameRate {
    FPS_60(60, 5, 100),
    FPS_30(30, 3, 100),

    FPS_6(6, -5, 100),
    FPS_15(15, -4, 100),
    FPS_24(24, -1, 100);

    private double rating = -1;
    private double fps;

    public double getFps() {
        return fps;
    }

    private VideoFrameRate(double fps, double rating, double modifier) {
        this.rating = rating / modifier;
        this.fps = fps;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getLabel(Object caller, YoutubeVariant callingVariant) {
        if (caller != null && callingVariant != null) {
            // return the correct resolution of possible
            // the youtube plugin onlinecheck writes the currently selected stream as property. So we can show the actuall height for this
            // variant
            DownloadLink link = null;
            if (caller instanceof CrawledLink) {
                link = ((CrawledLink) caller).getDownloadLink();
            } else if (caller instanceof DownloadLink) {
                link = (DownloadLink) caller;
            }
            if (link != null) {
                YoutubeFinalLinkResource r = link.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, YoutubeFinalLinkResource.TYPE_REF);
                try {
                    double rFps = Double.parseDouble(r.getFps());
                    if (r != null && rFps > 3) {

                        if (callingVariant.getiTagVideo() == r.getItag() && rFps != fps) {

                            return (int) Math.ceil(rFps) + "fps";

                        }

                    }
                } catch (Throwable e) {

                }

            }
        }

        return (int) Math.ceil(getFps()) + "fps";
    }
}
