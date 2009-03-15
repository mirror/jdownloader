package jd.gui.skins.simple;

import org.jdesktop.swingx.JXTaskPane;

public class TaskPanel extends JXTaskPane {

    private TabbedPane tabbedPane;

    public TabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public void setTabbedPane(TabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;

    }

}
