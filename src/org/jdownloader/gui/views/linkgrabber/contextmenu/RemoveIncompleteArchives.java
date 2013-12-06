package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.swing.exttable.ExtTableModelEventWrapper;
import org.appwork.swing.exttable.ExtTableModelListener;
import org.appwork.uio.UIOManager;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.ExtractIconVariant;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.downloads.action.ByPassDialogSetup;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.bottombar.IncludedSelectionSetup;

public class RemoveIncompleteArchives extends CustomizableAppAction implements ExtTableListener, ActionContext, ExtTableModelListener {

    /**
     * 
     */
    private static final long      serialVersionUID = 2816227528827363428L;
    private ByPassDialogSetup      byPassDialog;
    private IncludedSelectionSetup includedSelection;

    public RemoveIncompleteArchives() {

        setName(_GUI._.RemoveIncompleteArchives_RemoveIncompleteArchives_object_());
        setSmallIcon(new ExtractIconVariant("error", 18));
        addContextSetup(byPassDialog = new ByPassDialogSetup());
        initIncludeSelectionSupport();
    }

    @Override
    protected void initContextDefaults() {
        super.initContextDefaults();
    }

    protected void initIncludeSelectionSupport() {
        addContextSetup(includedSelection = new IncludedSelectionSetup(LinkGrabberTable.getInstance(), this, this));
    }

    public void actionPerformed(ActionEvent e) {

        final List<CrawledLink> nodesToDelete = new ArrayList<CrawledLink>();
        final AtomicBoolean containsOnline = new AtomicBoolean(false);

        final SelectionInfo<CrawledPackage, CrawledLink> selection = LinkGrabberTable.getInstance().getSelectionInfo();
        switch (includedSelection.getSelectionType()) {
        case NONE:
            return;

        case SELECTED:
            nodesToDelete.addAll(selection.getChildren());

            break;
        case UNSELECTED:
            final List<PackageControllerTableModelFilter<CrawledPackage, CrawledLink>> filters = LinkGrabberTableModel.getInstance().getEnabledTableFilters();
            LinkCollector.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {

                @Override
                public int returnMaxResults() {
                    return 0;
                }

                @Override
                public boolean acceptNode(CrawledLink node) {
                    if (!selection.contains(node)) {

                        if (true) {
                            for (PackageControllerTableModelFilter<CrawledPackage, CrawledLink> filter : filters) {
                                if (filter.isFiltered(node)) { return false; }
                            }
                        }
                        if (node.getDownloadLink().getAvailableStatus() != AvailableStatus.FALSE) {
                            containsOnline.set(true);
                        }
                        nodesToDelete.add(node);
                    }
                    return false;
                }
            });
            break;
        case ALL:

            nodesToDelete.addAll(LinkGrabberTable.getInstance().getSelectionInfo(false, true).getChildren());
        }

        if (!isEnabled()) return;
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    List<CrawledLink> l = new ArrayList<CrawledLink>();

                    for (Archive a : ArchiveValidator.validate(new SelectionInfo<CrawledPackage, CrawledLink>(null, nodesToDelete, true))) {
                        final DummyArchive da = ExtractionExtension.getInstance().createDummyArchive(a);
                        if (!da.isComplete()) {
                            try {
                                if (JDGui.bugme(WarnLevel.LOW) && !byPassDialog.isBypassDialog()) {
                                    Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.literally_are_you_sure(), _GUI._.RemoveIncompleteArchives_run_(da.getName()), null, _GUI._.literally_yes(), _GUI._.literall_no());
                                }

                                for (ArchiveFile af : a.getArchiveFiles()) {
                                    if (af instanceof CrawledLinkArchiveFile) {
                                        l.addAll(((CrawledLinkArchiveFile) af).getLinks());
                                    }
                                }
                                LinkCollector.getInstance().removeChildren(l);
                            } catch (DialogCanceledException e) {
                                // next archive
                            }
                        }

                    }

                } catch (DialogNoAnswerException e) {
                    return;
                } catch (Throwable e) {
                    Log.exception(e);
                }

            }
        };
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setName(getClass().getName());
        thread.start();
    }

    @Override
    public void onExtTableModelEvent(ExtTableModelEventWrapper event) {
    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {
    }

}
