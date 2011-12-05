package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class PropertiesAction extends AppAction {

    private CrawledLink    link;
    private CrawledPackage pkg;

    /**
     * 
     */

    public PropertiesAction(CrawledLink link, CrawledPackage pkg) {
        setName(_GUI._.MergeToPackageAction_MergeToPackageAction_());
        if (link != null) {
            setSmallIcon(link.getIcon());
        } else {
            setSmallIcon(NewTheme.I().getIcon("package_open", 18));
        }
        this.link = link;
        this.pkg = pkg;

    }

    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public boolean isEnabled() {
        return link != null || pkg != null;
    }

}
