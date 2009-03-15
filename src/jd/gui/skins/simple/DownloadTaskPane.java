package jd.gui.skins.simple;

import javax.swing.ImageIcon;

import jd.gui.skins.simple.config.ConfigPanelGUI;
import jd.utils.JDTheme;

public class DownloadTaskPane extends TreeTaskPane {

    private DownloadLinksTreeTablePanel linkListPane;

    public DownloadTaskPane(String string, ImageIcon ii, DownloadLinksTreeTablePanel linkListPane) {
        super(string, ii);
        this.linkListPane = linkListPane;
        initGUI();

    }

    private void initGUI() {
        getRoot().insertNodeInto(new TreeTabbedNode(linkListPane, "Show queue", JDTheme.II("gui.images.package_opened")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelGUI.class, null, "Start", JDTheme.II("gui.images.next")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelGUI.class, null, "Stop", JDTheme.II("gui.images.stop")));
        getRoot().insertNodeInto(new TreeTabbedNode(ConfigPanelGUI.class, null, "Pause", JDTheme.II("gui.images.stop_after")));
    }

}
