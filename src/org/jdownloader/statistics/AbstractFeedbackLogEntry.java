package org.jdownloader.statistics;

import org.appwork.storage.Storable;

public class AbstractFeedbackLogEntry extends AbstractLogEntry implements Storable {
    public static enum Rating {
        _10,
        _9,
        _8,
        _7,
        _6,
        _5,
        _4,
        _3,
        _2,
        _1,
        _0;
    }

    private Rating rating = Rating._0;

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    protected AbstractFeedbackLogEntry(/* storable */) {
    }

    protected AbstractFeedbackLogEntry(Rating rating) {
        this.rating = rating;
    }

    protected AbstractFeedbackLogEntry(boolean positive) {
        this.rating = positive ? Rating._10 : Rating._0;
    }
}
