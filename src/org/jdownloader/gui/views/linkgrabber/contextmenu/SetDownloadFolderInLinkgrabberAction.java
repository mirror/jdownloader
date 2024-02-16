package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.io.File;
import java.util.List;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.BadContextException;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.context.SetDownloadFolderAction;
import org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinknameCleaner;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;

public class SetDownloadFolderInLinkgrabberAction extends SetDownloadFolderAction<CrawledPackage, CrawledLink> {
    /**
     *
     */
    private static final long                                serialVersionUID = -6632019767606316873L;
    private final SelectionInfo<CrawledPackage, CrawledLink> selection;

    public SetDownloadFolderInLinkgrabberAction() {
        this(null);
    }

    public SetDownloadFolderInLinkgrabberAction(SelectionInfo<CrawledPackage, CrawledLink> selectionInfo) {
        selection = selectionInfo;
    }

    @Override
    protected SelectionInfo<CrawledPackage, CrawledLink> getSelection() {
        if (selection == null) {
            return super.getSelection();
        }
        return selection;
    }

    protected File dialog(File path) throws DialogClosedException, DialogCanceledException {
        CrawledPackage cp = getSelection().getFirstPackage();
        try {
            cp = getSelection().getFirstPackage();
        } catch (BadContextException e) {
            // happens if we open the contextmenu in the linkgrabber sidebar.
        }
        return DownloadFolderChooserDialog.open(path, true, _GUI.T.OpenDownloadFolderAction_actionPerformed_object_(cp.getName()));
    }

    @Override
    protected void set(CrawledPackage pkg, String absolutePath) {
        pkg.setDownloadFolder(absolutePath);
    }

    @Override
    /** New package gets created when the download-path of one or multiple items of an existing package gets changed. */
    protected CrawledPackage createNewByPrototype(SelectionInfo<CrawledPackage, CrawledLink> si, CrawledPackage entry) {
        final CrawledPackage pkg = new CrawledPackage();
        /* Set package expanded status based on global setting. */
        pkg.setExpanded(CFG_LINKCOLLECTOR.CFG.isPackageAutoExpanded());
        if (TYPE.NORMAL == entry.getType()) {
            /* Source package is a "normal" package -> Keep package-name */
            pkg.setName(entry.getName());
        } else {
            /*
             * Source package is a "special" package like "permanently offline" or "various" -> Use the filename of the first item of that
             * package and create a new package name by that.
             */
            pkg.setName(LinknameCleaner.derivePackagenameFromFilename(getSelection().getPackageView(entry).getChildren().get(0).getName()));
        }
        /* Adopt comment of source-package. */
        pkg.setComment(entry.getComment());
        return pkg;
    }

    @Override
    protected void move(CrawledPackage pkg, List<CrawledLink> selectedLinksByPackage) {
        LinkCollector.getInstance().moveOrAddAt(pkg, selectedLinksByPackage, -1);
    }

    @Override
    protected Queue getQueue() {
        return LinkCollector.getInstance().getQueue();
    }
}
