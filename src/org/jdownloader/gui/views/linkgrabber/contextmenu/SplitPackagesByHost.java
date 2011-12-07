package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkTreeUtils;

public class SplitPackagesByHost extends AppAction {

    /**
     * 
     */
    private static final long         serialVersionUID = 2636706677433058054L;
    private ArrayList<CrawledPackage> packages;

    public SplitPackagesByHost(AbstractNode contextObject, ArrayList<AbstractNode> selection) {

        this.packages = LinkTreeUtils.getPackages(contextObject, selection);
        setName(_GUI._.SplitPackagesByHost_SplitPackagesByHost_object_());
        setIconKey("split_packages");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
