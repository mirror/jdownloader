package org.jdownloader.gui.views.linkgrabber.bottombar;

import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.swing.exttable.ExtTableModelListener;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.linkgrabber.bottombar.GenericDeleteFromLinkgrabberAction.SelectionType;
import org.jdownloader.translate._JDT;

public class IncludedSelectionSetup implements ActionContext {
    private boolean                           includeUnselectedLinks   = false;
    private boolean                           includeSelectedLinks     = true;
    private final PackageControllerTable      table;
    private final PackageControllerTableModel model;
    private ExtTableModelListener             modelListener;
    private ExtTableListener                  tableListener;
    public static final String                INCLUDE_SELECTED_LINKS   = "includeSelectedLinks";
    public static final String                INCLUDE_UNSELECTED_LINKS = "includeUnselectedLinks"; ;

    public IncludedSelectionSetup(PackageControllerTable table, ExtTableModelListener modelListener, ExtTableListener tableListener) {
        this.table = table;
        this.model = table.getModel();
        this.modelListener = modelListener;
        this.tableListener = tableListener;
    }

    public SelectionType getSelectionType() {
        if (isIncludeSelectedLinks() && isIncludeUnselectedLinks()) {
            return SelectionType.ALL;
        } else if (isIncludeSelectedLinks()) {
            return SelectionType.SELECTED;
        } else if (isIncludeUnselectedLinks()) {
            return SelectionType.UNSELECTED;
        } else {
            return SelectionType.NONE;
        }
    }

    public void updateListeners() {
        switch (getSelectionType()) {
        case ALL:
            table.getEventSender().removeListener(tableListener);
            model.getEventSender().addListener(modelListener, true);
            break;
        case SELECTED:
        case UNSELECTED:
            table.getEventSender().addListener(tableListener, true);
            model.getEventSender().removeListener(modelListener);
            break;
        case NONE:
            table.getEventSender().removeListener(tableListener);
            model.getEventSender().removeListener(modelListener);
        }
    }

    public static String getTranslationForIncludeSelectedLinks() {
        return _JDT.T.IncludedSelectionSetup_getTranslationForIncludeSelectedLinks();
    }

    @Customizer(link = "#getTranslationForIncludeSelectedLinks")
    public boolean isIncludeSelectedLinks() {
        return includeSelectedLinks;
    }

    public void setIncludeSelectedLinks(boolean includeSelectedLinks) {
        this.includeSelectedLinks = includeSelectedLinks;
    }

    public static String getTranslationForIncludeUnselectedLinks() {
        return _JDT.T.IncludedSelectionSetup_getTranslationForIncludeUnselectedLinks();
    }

    @Customizer(link = "#getTranslationForIncludeUnselectedLinks")
    public boolean isIncludeUnselectedLinks() {
        return includeUnselectedLinks;
    }

    public void setIncludeUnselectedLinks(boolean includeUnselectedLinks) {
        this.includeUnselectedLinks = includeUnselectedLinks;
    }
}
