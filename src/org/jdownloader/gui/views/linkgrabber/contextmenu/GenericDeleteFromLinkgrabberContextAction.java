package org.jdownloader.gui.views.linkgrabber.contextmenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.controlling.contextmenu.TableContext;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.bottombar.GenericDeleteFromLinkgrabberAction;
import org.jdownloader.gui.views.linkgrabber.bottombar.IncludedSelectionSetup;

public class GenericDeleteFromLinkgrabberContextAction extends GenericDeleteFromLinkgrabberAction {
    private TableContext tableContext;

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
        if (tableContext != null) {
            SelectionInfo<CrawledPackage, CrawledLink> actualSelection = LinkGrabberTable.getInstance().getSelectionInfo();
            boolean has = actualSelection != null && !actualSelection.isEmpty();
            if (tableContext != null) {
                if (has) {
                    if (tableContext.isItemVisibleForSelections()) {
                        setVisible(true);
                    } else {
                        setVisible(false);
                        setEnabled(false);
                    }
                } else {
                    switch (includedSelection.getSelectionType()) {
                    case SELECTED:
                        if (tableContext.isItemVisibleForEmptySelection()) {
                            setVisible(true);
                            setEnabled(true);
                        } else {
                            setVisible(false);
                            setEnabled(false);
                        }
                        break;
                    case ALL:
                        if (tableContext.isItemVisibleForEmptySelection()) {
                            setVisible(true);
                            setEnabled(true);
                        } else {
                            setVisible(false);
                            setEnabled(false);
                        }
                    case UNSELECTED:
                        setVisible(false);
                        setEnabled(false);
                        break;
                    default:
                        setVisible(false);
                        setEnabled(false);
                    }
                }
            } else if (!has) {
                setVisible(false);
                setEnabled(false);
            } else if (has) {
                setVisible(true);
            }
        }
    }

    @Override
    public void initContextDefaults() {
        includedSelection.setIncludeSelectedLinks(true);
        includedSelection.setIncludeUnselectedLinks(false);
    }
}
