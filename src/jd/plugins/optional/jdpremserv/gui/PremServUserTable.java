package jd.plugins.optional.jdpremserv.gui;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;

import jd.plugins.optional.jdpremserv.gui.actions.AddUserAction;
import jd.plugins.optional.jdpremserv.gui.actions.EditHosterUserAction;
import jd.plugins.optional.jdpremserv.gui.actions.EditTrafficUserAction;
import jd.plugins.optional.jdpremserv.gui.actions.EnableDisableUserAction;
import jd.plugins.optional.jdpremserv.gui.actions.RemoveUserAction;
import jd.plugins.optional.jdpremserv.model.PremServUser;

import org.appwork.utils.swing.table.AlternateHighlighter;
import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.SelectionHighlighter;

public class PremServUserTable extends ExtTable<PremServUser> {

    /**
     * 
     */
    private static final long serialVersionUID = -8085452897702054596L;
    private static final PremServUserTable INSTANCE = new PremServUserTable();

    public static PremServUserTable getInstance() {
        return INSTANCE;
    }

    private PremServUserTable() {

        super(PremServUserTableModel.getInstance(), "PremServUserTable");
        // this.setFocusable(false);
        // setShowHorizontalLines(false);
        setShowVerticalLines(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setRowHeight(22);

        this.addRowHighlighter(new SelectionHighlighter(null, new Color(0, 255, 0, 30)));
        this.addRowHighlighter(new AlternateHighlighter(null, new Color(100, 100, 100, 30)));
        this.setSearchEnabled(true);

    }

    protected void onSelectionChanged(ArrayList<PremServUser> selected) {

        scrollToSelection();

    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, PremServUser contextObject, ArrayList<PremServUser> selection) {

        popup.add(new AddUserAction());
        if (contextObject != null) {
            popup.add(new RemoveUserAction(contextObject));
            popup.add(new EnableDisableUserAction(contextObject));
            popup.add(new EditHosterUserAction(contextObject));
            popup.add(new EditTrafficUserAction(contextObject));
        }
        return popup;

    }

    protected boolean onShortcutPaste(ArrayList<PremServUser> selectedObjects, KeyEvent evt) {

        return false;
    }

    protected boolean onShortcutDelete(ArrayList<PremServUser> selectedObjects, KeyEvent evt, boolean diorect) {
        if (selectedObjects == null || selectedObjects.size() == 0) return false;

        return true;
    }

}