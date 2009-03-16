package jd.gui.skins.simple.tasks;

import javax.swing.ImageIcon;

import jd.gui.skins.simple.LinkGrabber;
import jd.gui.skins.simple.TreeTabbedNode;
import jd.utils.JDTheme;

public class GrabberTaskPane extends TaskPanel {

    private LinkGrabber grabber;
    private TreeTabbedNode linkGrabberNode;

    public GrabberTaskPane(String string, ImageIcon ii, LinkGrabber grabber) {
super(string,ii);
        this.grabber = grabber;
        initGUI();

    }

    private void initGUI() {

        getRoot().insertNodeInto(linkGrabberNode = new TreeTabbedNode(grabber, "Show", JDTheme.II("gui.images.package_opened")));
        getRoot().insertNodeInto(linkGrabberNode = new TreeTabbedNode(grabber, "Insert all", JDTheme.II("gui.images.add")));
        getRoot().insertNodeInto(linkGrabberNode = new TreeTabbedNode(grabber, "close", JDTheme.II("gui.images.exit")));

    }

}
