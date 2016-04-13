package org.jdownloader.plugins.components.youtube.itag;

import org.jdownloader.gui.translate._GUI;

public enum ImageQuality {
    HIGH(3, 10) {
        @Override
        public String getLocaleName() {
            return _GUI.T.YoutubeVariant_name_IMAGE_HQ();
        }

        public String getLocaleTag() {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_HQ();
        }
    },
    LOW(1, 10) {
        @Override
        public String getLocaleName() {
            return _GUI.T.YoutubeVariant_name_IMAGE_LQ();
        }

        public String getLocaleTag() {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_LQ();
        }
    },
    HIGHEST(4, 10) {
        @Override
        public String getLocaleName() {
            return _GUI.T.YoutubeVariant_name_IMAGE_MAX();
        }

        public String getLocaleTag() {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_MAX();
        }
    },
    NORMAL(2, 10) {
        @Override
        public String getLocaleName() {
            return _GUI.T.YoutubeVariant_name_IMAGE_MQ();
        }

        public String getLocaleTag() {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_MQ();
        }
    };
    private double rating = -1;

    private ImageQuality(double rating, double modifier) {
        this.rating = rating / modifier;
    }

    public abstract String getLocaleName();

    public abstract String getLocaleTag();

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

}
