package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinknameCleaner;
import jd.controlling.linkcollector.VariousCrawledPackage;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.context.SetDownloadFolderAction;

public class SetDownloadFolderInLinkgrabberAction extends SetDownloadFolderAction<CrawledPackage, CrawledLink> {

    /**
     * 
     */
    private static final long serialVersionUID = -6632019767606316873L;

    public SetDownloadFolderInLinkgrabberAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        super(si);

    }

    public void actionPerformed(ActionEvent e) {

    }

    @Override
    protected void set(CrawledPackage pkg, String absolutePath) {

        pkg.setDownloadFolder(absolutePath);
    }

    @Override
    protected CrawledPackage createNewByPrototype(SelectionInfo<CrawledPackage, CrawledLink> si, CrawledPackage entry) {
        final CrawledPackage pkg = new CrawledPackage();
        pkg.setExpanded(true);
        pkg.setCreated(System.currentTimeMillis());
        if (entry instanceof VariousCrawledPackage) {
            pkg.setName(LinknameCleaner.cleanFileName(si.getSelectedLinksByPackage(entry).get(0).getName()));
        } else {
            pkg.setName(entry.getName());
        }
        pkg.setComment(entry.getComment());

        return pkg;
    }

    @Override
    protected void move(CrawledPackage pkg, List<CrawledLink> selectedLinksByPackage) {
        LinkCollector.getInstance().moveOrAddAt(pkg, selectedLinksByPackage, -1);
    }

}
