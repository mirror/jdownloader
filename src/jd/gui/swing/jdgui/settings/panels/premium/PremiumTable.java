package jd.gui.swing.jdgui.settings.panels.premium;

import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.JDTable.JDTable;
import jd.plugins.Account;

public class PremiumTable extends JDTable {

    /**
     * 
     */
    private static final long serialVersionUID = 9049514723238421532L;
    private Premium panel;

    public PremiumTable(Premium panel) {
        super(new PremiumJTableModel("premiumview"));
        this.panel = panel;
    }

    public ArrayList<Account> getSelectedAccounts() {
        int[] rows = getSelectedRows();
        ArrayList<Account> ret = new ArrayList<Account>();
        for (int row : rows) {
            Object element = getValueAt(row, 0);
            if (element != null && element instanceof Account) {
                ret.add((Account) element);
            }
        }
        return ret;
    }

    public void fireTableChanged() {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                final Rectangle viewRect = panel.getScrollPane().getViewport().getViewRect();
                int[] rows = getSelectedRows();
                final ArrayList<Object> selected = new ArrayList<Object>();
                for (int row : rows) {
                    Object elem = getJDTableModel().getValueAt(row, 0);
                    if (elem != null) selected.add(elem);
                }
                getJDTableModel().refreshModel();
                getJDTableModel().fireTableStructureChanged();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (Object obj : selected) {
                            int row = getJDTableModel().getRowforObject(obj);
                            if (row != -1) addRowSelectionInterval(row, row);
                        }
                        scrollRectToVisible(viewRect);
                    }
                });
                return null;
            }
        }.start();
    }

}
