package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.io.File;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinknameCleaner;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;

import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
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

    protected File dialog(File path) throws DialogClosedException, DialogCanceledException {
        return DownloadFolderChooserDialog.open(path, true, _GUI._.OpenDownloadFolderAction_actionPerformed_object_(si.getContextPackage().getName()));
    }

    @Override
    protected void set(CrawledPackage pkg, String absolutePath) {
        pkg.setDownloadFolder(absolutePath);
    }

    @Override
    protected CrawledPackage createNewByPrototype(SelectionInfo<CrawledPackage, CrawledLink> si, CrawledPackage entry) {
        final CrawledPackage pkg = new CrawledPackage();
        pkg.setExpanded(true);
        if (TYPE.NORMAL != entry.getType()) {
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
