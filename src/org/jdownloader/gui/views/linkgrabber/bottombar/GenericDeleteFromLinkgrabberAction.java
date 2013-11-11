package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.swing.exttable.ExtTableModelListener;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public class GenericDeleteFromLinkgrabberAction extends CustomizableAppAction implements ExtTableListener, ActionContext, ExtTableModelListener {

    public static final String                           ONLY_SELECTED_ITEMS = "onlySelectedItems";
    public static final String                           DELETE_ALL          = "deleteAll";
    public static final String                           DELETE_DISABLED     = "deleteDisabled";
    public static final String                           DELETE_OFFLINE      = "deleteOffline";
    /**
     * 
     */
    private static final long                            serialVersionUID    = 1L;

    private DelayedRunnable                              delayer;
    private boolean                                      deleteAll           = false;

    private boolean                                      deleteDisabled      = false;

    private boolean                                      deleteOffline       = false;

    private boolean                                      ignoreFiltered      = true;

    private CrawledLink                                  lastLink;

    private boolean                                      onlySelectedItems   = false;

    protected SelectionInfo<CrawledPackage, CrawledLink> selection;

    public GenericDeleteFromLinkgrabberAction() {
        super();

        setIconKey(IconKey.ICON_DELETE);
        delayer = new DelayedRunnable(500, 1500) {

            @Override
            public void delayedrun() {
                update();
            }
        };

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final List<CrawledLink> nodesToDelete = new ArrayList<CrawledLink>();
        boolean containsOnline = false;
        for (final CrawledLink dl : selection.getChildren()) {
            if (checkLink(dl)) {
                nodesToDelete.add(dl);

                if (TYPE.OFFLINE == dl.getParentNode().getType()) continue;
                if (TYPE.POFFLINE == dl.getParentNode().getType()) continue;
                if (dl.getDownloadLink().getAvailableStatus() != AvailableStatus.FALSE) {
                    containsOnline = true;

                }
            }
        }
        LinkCollector.requestDeleteLinks(nodesToDelete, containsOnline, createName());

    }

    public boolean checkLink(CrawledLink cl) {
        if (isDeleteAll()) return true;
        if (isDeleteDisabled() && !cl.isEnabled()) {

        return true; }

        if (isDeleteOffline() && cl.getDownloadLink().isAvailabilityStatusChecked() && cl.getDownloadLink().getAvailableStatus() == AvailableStatus.FALSE) {

        return true; }
        return false;
    }

    private String createName() {
        final StringBuilder sb = new StringBuilder();

        if (this.isDeleteAll()) {
            if (this.isOnlySelectedItems()) {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected_all());
            } else {
                sb.append(_GUI._.GenericDeleteFromLinkgrabberAction_createName_updateName_object_all());
            }
        } else {
            if (this.isOnlySelectedItems()) {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected());
            } else {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object());
            }
            boolean first = true;

            if (this.isDeleteDisabled()) {
                if (!first) {
                    sb.append(" & ");
                }
                sb.append(_GUI._.lit_disabled());
                first = false;
            }

            if (this.isDeleteOffline()) {
                if (!first) {
                    sb.append(" & ");
                }
                first = false;
                sb.append(_GUI._.lit_offline());
            }
        }
        return sb.toString();
    }

    @Customizer(name = "Include All Links")
    public boolean isDeleteAll() {
        return deleteAll;
    }

    @Customizer(name = "Include disabled Links")
    public boolean isDeleteDisabled() {
        return deleteDisabled;
    }

    @Customizer(name = "Include Offline Links")
    public boolean isDeleteOffline() {
        return deleteOffline;
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }

    @Customizer(name = "Exclude filtered Links")
    public boolean isIgnoreFiltered() {
        return ignoreFiltered;
    }

    @Customizer(name = "Only Selected Links")
    public boolean isOnlySelectedItems() {
        return onlySelectedItems;
    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {
        if (event.getType() == ExtTableEvent.Types.SELECTION_CHANGED) {
            update();
        }
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        updateListeners();
        update();
    }

    public void setDeleteAll(final boolean deleteIdle) {
        GenericDeleteFromLinkgrabberAction.this.deleteAll = deleteIdle;
        updateName();

    }

    public void setDeleteDisabled(final boolean deleteDisabled) {
        GenericDeleteFromLinkgrabberAction.this.deleteDisabled = deleteDisabled;
        updateName();

    }

    public void setDeleteOffline(final boolean deleteOffline) {
        GenericDeleteFromLinkgrabberAction.this.deleteOffline = deleteOffline;
        updateName();
    }

    public void setIgnoreFiltered(final boolean ignoreFiltered) {
        GenericDeleteFromLinkgrabberAction.this.ignoreFiltered = ignoreFiltered;
        updateName();

    }

    public void setOnlySelectedItems(final boolean onlySelectedItems) {
        GenericDeleteFromLinkgrabberAction.this.onlySelectedItems = onlySelectedItems;
        updateListeners();
        updateName();
    }

    protected void updateListeners() {
        if (isOnlySelectedItems()) {
            LinkGrabberTable.getInstance().getEventSender().addListener(GenericDeleteFromLinkgrabberAction.this, true);

            LinkGrabberTableModel.getInstance().getEventSender().removeListener(GenericDeleteFromLinkgrabberAction.this);
        } else {
            LinkGrabberTable.getInstance().getEventSender().removeListener(GenericDeleteFromLinkgrabberAction.this);
            LinkGrabberTableModel.getInstance().getEventSender().addListener(GenericDeleteFromLinkgrabberAction.this, true);
        }
    }

    protected void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                if (isOnlySelectedItems()) {
                    selection = LinkGrabberTable.getInstance().getSelectionInfo();

                } else {

                    if (isIgnoreFiltered()) {

                        selection = LinkGrabberTable.getInstance().getSelectionInfo(false, false);
                    } else {
                        selection = LinkGrabberTable.getInstance().getSelectionInfo(false, true);

                    }

                }
                setVisible(true);
                // if (!tableContext.isItemVisibleForEmptySelection() && !hasSelection()) {
                // setVisible(false);
                // setEnabled(false);
                // return;
                // }
                // if (!tableContext.isItemVisibleForSelections() && hasSelection()) {
                // setVisible(false);
                // setEnabled(false);
                // return;
                // }
                // we remember the last link and try it first
                if (lastLink != null && selection.contains(lastLink)) {
                    if (checkLink(lastLink)) {

                        setEnabled(true);
                        return;
                    }
                }

                for (CrawledLink link : selection.getChildren()) {
                    if (checkLink(link)) {
                        setEnabled(true);
                        lastLink = link;
                        return;
                    }
                }
                setEnabled(false);
            }

            /**
             * @param link
             * @return
             */

        };

    }

    private void updateName() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setName(createName());
            }
        };

    }

    @Override
    public void onExtTableModelEvent(ExtTableModelListener listener) {
        delayer.resetAndStart();
    }

}
