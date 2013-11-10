package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.controlling.contextmenu.TableContext;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public class GenericDeleteFromLinkgrabberContextAction extends CustomizableSelectionAppAction<CrawledPackage, CrawledLink> implements ActionContext {

    private TableContext tableContext;

    public GenericDeleteFromLinkgrabberContextAction() {

        this.setIconKey(IconKey.ICON_DELETE);

        addContextSetup(tableContext = new TableContext(false, true));

    }

    @Override
    protected void initContextDefaults() {
        setOnlySelectedItems(true);
    }

    public static final String ONLY_SELECTED_ITEMS = "onlySelectedItems";
    /**
     * 
     */
    private static final long  serialVersionUID    = 1L;
    public static final String DELETE_ALL          = "deleteAll";
    public static final String DELETE_DISABLED     = "deleteDisabled";
    public static final String DELETE_OFFLINE      = "deleteOffline";

    private boolean            deleteDisabled      = false;
    private boolean            onlySelectedItems   = false;

    private boolean            deleteAll           = false;

    private boolean            ignoreFiltered      = true;

    private boolean            deleteOffline       = false;

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        update();

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
                    break;
                }
            }
        }
        LinkCollector.requestDeleteLinks(nodesToDelete, containsOnline, createName());
    }

    private LinkGrabberTable getTable() {
        return (LinkGrabberTable) LinkGrabberTableModel.getInstance().getTable();
    }

    @Customizer(name = "Include All Links")
    public boolean isDeleteAll() {
        return this.deleteAll;
    }

    @Customizer(name = "Include disabled Links")
    public boolean isDeleteDisabled() {
        return this.deleteDisabled;
    }

    @Customizer(name = "Include Offline Links")
    public boolean isDeleteOffline() {
        return this.deleteOffline;
    }

    @Customizer(name = "Exclude filtered Links")
    public boolean isIgnoreFiltered() {
        return this.ignoreFiltered;
    }

    @Customizer(name = "Only Selected Links")
    public boolean isOnlySelectedItems() {

        return this.onlySelectedItems;
    }

    public void setDeleteAll(final boolean deleteIdle) {
        this.deleteAll = deleteIdle;

    }

    public void setDeleteDisabled(final boolean deleteDisabled) {
        this.deleteDisabled = deleteDisabled;

    }

    public void setDeleteOffline(final boolean deleteOffline) {
        this.deleteOffline = deleteOffline;

    }

    public void setIgnoreFiltered(final boolean ignoreFiltered) {
        this.ignoreFiltered = ignoreFiltered;

    }

    public void setOnlySelectedItems(final boolean onlySelectedItems) {
        this.onlySelectedItems = onlySelectedItems;

    }

    private CrawledLink lastLink;

    public boolean checkLink(CrawledLink cl) {
        if (isDeleteAll()) return true;
        if (isDeleteDisabled() && !cl.isEnabled()) {

        return true; }

        if (isDeleteOffline() && cl.getDownloadLink().isAvailabilityStatusChecked() && cl.getDownloadLink().getAvailableStatus() == AvailableStatus.FALSE) {

        return true; }
        return false;
    }

    private void update() {
        this.updateName();
        if (this.isOnlySelectedItems()) {
            if (hasSelection()) {
                selection = getSelection();
            }
        } else {

            if (this.isIgnoreFiltered()) {

                selection = getTable().getSelectionInfo(false, false);
            } else {
                selection = getTable().getSelectionInfo(false, true);

            }

        }
        setVisible(true);
        if (!tableContext.isItemVisibleForEmptySelection() && !hasSelection()) {
            setVisible(false);
            setEnabled(false);
            return;
        }
        if (!tableContext.isItemVisibleForSelections() && hasSelection()) {
            setVisible(false);
            setEnabled(false);
            return;
        }
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

    private void updateName() {

        // if (isDeleteFailed() && isDeleteDisabled() && isDeleteFinished() && isDeleteOffline()) {
        // setName(_GUI._.ContextMenuFactory_createPopup_cleanup_only());
        // } else {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setName(createName());
            }
        };

        // }

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
}
