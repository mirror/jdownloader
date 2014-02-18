package jd.gui.swing.jdgui;

public abstract class DirectFeedback {

    private boolean positive;

    public DirectFeedback(boolean positive) {
        this.positive = positive;
    }

    public boolean isPositive() {
        return positive;
    }

    public void setPositive(boolean positive) {
        this.positive = positive;
    }

}
