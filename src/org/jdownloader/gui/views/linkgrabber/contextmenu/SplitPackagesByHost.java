package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class SplitPackagesByHost extends AppAction {

    private ArrayList<CrawledPackage> packages;

    public SplitPackagesByHost(ArrayList<CrawledPackage> packages) {
        this.packages = packages;
        setName(_GUI._.SplitPackagesByHost_SplitPackagesByHost_object_());
        setIconKey("split_packages");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
