//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
                    Object elem = getValueAt(row, 0);
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
