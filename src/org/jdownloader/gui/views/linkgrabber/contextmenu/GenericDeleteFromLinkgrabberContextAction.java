package org.jdownloader.gui.views.linkgrabber.contextmenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.TableContext;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.bottombar.GenericDeleteFromLinkgrabberAction;
import org.jdownloader.gui.views.linkgrabber.bottombar.IncludedSelectionSetup;

public class GenericDeleteFromLinkgrabberContextAction extends GenericDeleteFromLinkgrabberAction {
    private final TableContext tableContext;

    public GenericDeleteFromLinkgrabberContextAction() {
        super();
        addContextSetup(tableContext = new TableContext(false, true));
    }

    protected void initIncludeSelectionSupport() {
        addContextSetup(includedSelection = new IncludedSelectionSetup(LinkGrabberTable.getInstance(), this, this) {
            @Override
            public void updateListeners() {
            }
        });
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                SelectionInfo<CrawledPackage, CrawledLink> selection = GenericDeleteFromLinkgrabberContextAction.this.selection.get();
                final boolean hasSelection = selection != null && !selection.isEmpty();
                if (hasSelection) {
                    if (tableContext.isItemVisibleForSelections()) {
                        setVisible(true);
                    } else {
                        setVisible(false);
                        setEnabled(false);
                    }
                } else {
                    if (tableContext.isItemVisibleForEmptySelection()) {
                        setVisible(true);
                    } else {
                        setVisible(false);
                        setEnabled(false);
                    }
                }
            }
        };
    }

    @Override
    public void initContextDefaults() {
        includedSelection.setIncludeSelectedLinks(true);
        includedSelection.setIncludeUnselectedLinks(false);
    }
}
