package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.swing.exttable.ExtTableModelEventWrapper;
import org.appwork.swing.exttable.ExtTableModelListener;
import org.appwork.uio.UIOManager;
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
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.action.ByPassDialogSetup;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.bottombar.IncludedSelectionSetup;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;

public class RemoveIncompleteArchives extends CustomizableAppAction implements ExtTableListener, ActionContext, ExtTableModelListener {

    /**
     *
     */
    private static final long      serialVersionUID = 2816227528827363428L;
    private ByPassDialogSetup      byPassDialog;
    private IncludedSelectionSetup includedSelection;

    public RemoveIncompleteArchives() {
        setName(_GUI.T.RemoveIncompleteArchives_RemoveIncompleteArchives_object_());
        setIconKey(IconKey.ICON_EXTRACT_ERROR);
        addContextSetup(byPassDialog = new ByPassDialogSetup());
        initIncludeSelectionSupport();
    }

    @Override
    public void initContextDefaults() {
        super.initContextDefaults();
    }

    protected void initIncludeSelectionSupport() {
        addContextSetup(includedSelection = new IncludedSelectionSetup(LinkGrabberTable.getInstance(), this, this));
    }

    public void actionPerformed(ActionEvent e) {
        if (isEnabled()) {
            final SelectionInfo<CrawledPackage, CrawledLink> selectionInfo;
            switch (includedSelection.getSelectionType()) {
            case NONE:
                selectionInfo = null;
                return;
            case SELECTED:
                selectionInfo = LinkGrabberTable.getInstance().getSelectionInfo();
                break;
            case UNSELECTED:
                selectionInfo = LinkGrabberTable.getInstance().getSelectionInfo();
                if (selectionInfo.getUnselectedChildren() == null) {
                    return;
                }
                break;
            case ALL:
                selectionInfo = LinkGrabberTable.getInstance().getSelectionInfo(false, true);
                break;
            default:
                selectionInfo = null;
                return;
            }
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        final List<Archive> archives;
                        switch (includedSelection.getSelectionType()) {
                        case NONE:
                            archives = new ArrayList<Archive>();
                            return;
                        case SELECTED:
                            archives = ArchiveValidator.getArchivesFromPackageChildren(selectionInfo.getChildren());
                            break;
                        case UNSELECTED:
                            if (selectionInfo.getUnselectedChildren() == null) {
                                return;
                            }
                            archives = ArchiveValidator.getArchivesFromPackageChildren(selectionInfo.getUnselectedChildren());
                            break;
                        case ALL:
                            archives = ArchiveValidator.getArchivesFromPackageChildren(selectionInfo.getChildren());
                            break;
                        default:
                            archives = new ArrayList<Archive>();
                            return;
                        }

                        for (final Archive archive : archives) {
                            final DummyArchive da = ExtractionExtension.getInstance().createDummyArchive(archive);
                            if (!da.isComplete()) {
                                try {
                                    if (JDGui.bugme(WarnLevel.LOW) && !byPassDialog.isBypassDialog()) {
                                        Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI.T.literally_are_you_sure(), _GUI.T.RemoveIncompleteArchives_run_(da.getName()), null, _GUI.T.literally_yes(), _GUI.T.literall_no());
                                    }
                                    final List<CrawledLink> links = new ArrayList<CrawledLink>();
                                    for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                                        if (archiveFile instanceof CrawledLinkArchiveFile) {
                                            links.addAll(((CrawledLinkArchiveFile) archiveFile).getLinks());
                                        }
                                    }
                                    LinkCollector.getInstance().removeChildren(links);
                                } catch (DialogCanceledException e) {
                                    // next archive
                                }
                            }

                        }

                    } catch (DialogNoAnswerException e) {
                        return;
                    } catch (Throwable e) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                    }

                }
            };
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setName(getClass().getName());
            thread.start();
        }
    }

    @Override
    public void onExtTableModelEvent(ExtTableModelEventWrapper event) {
    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {
    }

}
