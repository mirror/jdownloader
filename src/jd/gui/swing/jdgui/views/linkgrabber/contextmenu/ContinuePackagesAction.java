package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.plugins.LinkGrabberFilePackage;

import org.jdownloader.gui.translate._GUI;

public class ContinuePackagesAction extends ContextMenuAction {

    private static final long                       serialVersionUID = -6472892343168808922L;

    private final ArrayList<LinkGrabberFilePackage> packages;

    public ContinuePackagesAction(ArrayList<LinkGrabberFilePackage> packages) {
        this.packages = packages;

        init();
    }

    @Override
    protected String getIcon() {
        return "linkgrabber";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_linkgrabberv2_lg_continueselected() + " (" + packages.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        LinkGrabberPanel.getLinkGrabber().confirmPackages(packages);
    }

}