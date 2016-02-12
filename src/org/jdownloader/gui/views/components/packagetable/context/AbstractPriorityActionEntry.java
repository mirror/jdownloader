package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.KeyObserver;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.translate._JDT;

import jd.controlling.TaskQueue;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.interfaces.View;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public abstract class AbstractPriorityActionEntry<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends CustomizableTableContextAppAction<PackageType, ChildrenType> implements GUIListener, ActionContext {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final Priority    priority;
    private volatile boolean  metaCtrl         = false;

    public AbstractPriorityActionEntry(Priority priority) {
        super();
        this.priority = priority;
        GUIEventSender.getInstance().addListener(this, true);
        metaCtrl = KeyObserver.getInstance().isMetaDown(true) || KeyObserver.getInstance().isControlDown(true);
        updateStateAndLabelAndIcon();
        setSmallIcon(priority.loadIcon(18));
    }

    @Override
    public void onKeyModifier(int parameter) {
        final boolean before = metaCtrl;
        if (KeyObserver.getInstance().isControlDown(false) || KeyObserver.getInstance().isMetaDown(false)) {
            metaCtrl = true;
        } else {
            metaCtrl = false;
        }
        if (before != metaCtrl) {
            updateStateAndLabelAndIcon();
        }
    }

    private void updateStateAndLabelAndIcon() {
        if (isForceMode() && !metaCtrl || metaCtrl) {
            setName(priority.T() + " " + _GUI.T.system_download_triggerfileexists_overwrite());
        } else {
            setName(priority.T());
        }
    }

    private boolean forceMode = false;

    public static String getTranslationForForceMode() {
        return _JDT.T.PriorityAction_getTranslationForForceMode();
    }

    @Customizer(link = "#getTranslationForForceMode")
    public boolean isForceMode() {
        return forceMode;
    }

    public void setForceMode(boolean forceMode) {
        this.forceMode = forceMode;
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
    }

    public void actionPerformed(ActionEvent e) {
        final SelectionInfo<PackageType, ChildrenType> selection = getSelection();
        if (selection.isEmpty()) {
            return;
        }
        final boolean finalMetaCtrl = forceMode ? !metaCtrl : metaCtrl;
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                boolean linkGrabber = false;
                boolean downloadList = false;
                final ArrayList<AbstractPackageNode> forcePriority = new ArrayList<AbstractPackageNode>();
                for (final AbstractNode node : selection.getRawSelection()) {
                    if (node instanceof CrawledLink) {
                        linkGrabber = true;
                        ((CrawledLink) node).setPriority(priority);
                    } else if (node instanceof DownloadLink) {
                        downloadList = true;
                        ((DownloadLink) node).setPriorityEnum(priority);
                    } else if (node instanceof CrawledPackage) {
                        linkGrabber = true;
                        if (finalMetaCtrl) {
                            forcePriority.add((AbstractPackageNode) node);
                        }
                        ((CrawledPackage) node).setPriorityEnum(priority);
                    } else if (node instanceof FilePackage) {
                        downloadList = true;
                        if (finalMetaCtrl) {
                            forcePriority.add((AbstractPackageNode) node);
                        }
                        ((FilePackage) node).setPriorityEnum(priority);
                    }
                }
                for (final AbstractPackageNode node : forcePriority) {
                    final boolean readL = node.getModifyLock().readLock();
                    try {
                        for (final Object child : node.getChildren()) {
                            if (child instanceof CrawledLink) {
                                ((CrawledLink) child).setPriority(priority);
                            } else if (child instanceof DownloadLink) {
                                ((DownloadLink) child).setPriorityEnum(priority);
                            }
                        }
                    } finally {
                        node.getModifyLock().readUnlock(readL);
                    }
                }
                if (linkGrabber) {
                    LinkGrabberTableModel.getInstance().setPriorityColumnVisible(true);
                }
                if (downloadList) {
                    DownloadsTableModel.getInstance().setPriorityColumnVisible(true);
                }
                return null;
            }
        });
    }

}
