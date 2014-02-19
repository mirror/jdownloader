package jd.gui.swing.jdgui;


public class BasicIDFeedback extends DirectFeedback {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BasicIDFeedback(boolean positive, String string) {
        super(positive);
        this.id = string;
    }

}
