//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.settings.panels.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.config.Configuration;
import jd.config.ConfigEntry.PropertyType;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.components.toolbar.MainToolBar;
import jd.gui.swing.jdgui.components.toolbar.ToolBar;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;

public class ToolbarController extends ConfigPanel {
    private static final ArrayList<String> WHITELIST = new ArrayList<String>();
    static {
        // controlls
        WHITELIST.add("toolbar.control.start");
        WHITELIST.add("toolbar.control.pause");
        WHITELIST.add("toolbar.control.stop");

        // move
        WHITELIST.add("separator");
        WHITELIST.add("action.downloadview.movetotop");
        WHITELIST.add("action.downloadview.moveup");
        WHITELIST.add("action.downloadview.movedown");
        WHITELIST.add("action.downloadview.movetobottom");

        // config
        WHITELIST.add("separator");
        WHITELIST.add("toolbar.quickconfig.clipboardoberserver");
        WHITELIST.add("toolbar.quickconfig.reconnecttoggle");
        WHITELIST.add("toolbar.control.stopmark");

        WHITELIST.add("separator");
        WHITELIST.add("scheduler");
        WHITELIST.add("langfileditor");
        WHITELIST.add("chat");
        WHITELIST.add("livescripter");

        // removes
        WHITELIST.add("separator");
        WHITELIST.add("action.remove.links");
        WHITELIST.add("action.remove.packages");
        WHITELIST.add("action.remove_dupes");
        WHITELIST.add("action.remove_disabled");
        WHITELIST.add("action.remove_offline");
        WHITELIST.add("action.remove_failed");

        // actions
        WHITELIST.add("separator");
        WHITELIST.add("action.addurl");
        WHITELIST.add("action.load");

        WHITELIST.add("toolbar.interaction.reconnect");
        WHITELIST.add("toolbar.interaction.update");

        WHITELIST.add("action.opendlfolder");
        WHITELIST.add("action.restore");
        WHITELIST.add("action.premiumview.addacc");
        WHITELIST.add("action.premium.buy");

        WHITELIST.add("action.about");
        WHITELIST.add("action.help");
        WHITELIST.add("action.changes");
        WHITELIST.add("action.restart");
        WHITELIST.add("action.exit");

        WHITELIST.add("gui.jdshutdown.toggle");

    }
    private static final long serialVersionUID = -7024581410075950497L;

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + ".toolbarController.title", "Toolbar Manager");
    }

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.gui.ToolbarController.";

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Boolean.class;
            return ToolBarAction.class;
        }

        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDL.L(JDL_PREFIX + ".column.use", "Use");
            case 1:
                return JDL.L(JDL_PREFIX + ".column.name", "Name");
            case 2:
                return JDL.L(JDL_PREFIX + ".column.desc", "Description");
            case 3:
                return JDL.L(JDL_PREFIX + ".column.icon", "Icon");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {

            return actions.size();
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (columnIndex == 0) return list.contains(actions.get(rowIndex).getID());

            return actions.get(rowIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                if ((Boolean) value) {
                    list.add(actions.get(row).getID());
                } else {
                    // if there are accidently more entries
                    while (list.remove(actions.get(row).getID())) {
                    }
                }
                GUIUtils.getConfig().setProperty("TOOLBAR", list);
                GUIUtils.getConfig().save();
                Collections.sort(list, new Comparator<String>() {

                    public int compare(String o1, String o2) {
                        int ia = WHITELIST.indexOf(o1);

                        int ib = WHITELIST.indexOf(o2);

                        return ia < ib ? -1 : 1;

                    }
                });
                while (list.remove("toolbar.separator")) {
                }
                // adds separatores based on WHITELIST order
                for (int i = 1; i < list.size(); i++) {
                    int b = WHITELIST.indexOf(list.get(i));
                    int a = WHITELIST.indexOf(list.get(i - 1));
                    if (a > 0 && b > 0) {
                        for (int ii = a; ii < b; ii++) {
                            if (WHITELIST.get(ii).equals("separator")) {
                                list.add(i, "toolbar.separator");
                                i++;
                                break;
                            }
                        }
                    }
                }
                MainToolBar.getInstance().setList(list.toArray(new String[] {}));
            }
        }
    }

    private JXTable table;

    private InternalTableModel tableModel;

    private ArrayList<ToolBarAction> actions;

    private ArrayList<String> list;

    public ToolbarController(Configuration configuration) {
        super();
        actions = new ArrayList<ToolBarAction>();
        initPanel();
        load();
    }

    public void onShow() {
        super.onShow();
        setActions(ActionController.getActions());

    }

    /**
     * filters the available actions
     * 
     * @param actions2
     */
    private void setActions(ArrayList<ToolBarAction> actions2) {

        Collections.sort(actions2, new Comparator<ToolBarAction>() {

            public int compare(ToolBarAction o1, ToolBarAction o2) {
                int ia = WHITELIST.indexOf(o1.getID());

                int ib = WHITELIST.indexOf(o2.getID());

                return ia < ib ? -1 : 1;

            }
        });

        this.list = new ArrayList<String>();
        list.addAll(GUIUtils.getConfig().getGenericProperty("TOOLBAR", ToolBar.DEFAULT_LIST));
        for (Iterator<ToolBarAction> it = actions2.iterator(); it.hasNext();) {
            ToolBarAction a = it.next();
            if (a.getValue(ToolBarAction.IMAGE_KEY) == null) {
                it.remove();
                list.remove(a.getID());
                continue;
            }
            if (!WHITELIST.contains(a.getID())) {
                it.remove();
                list.remove(a.getID());

                System.out.println("WHITELIST.add(\"" + a.getID() + "\");");
                continue;
            }

        }
        this.actions = actions2;
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                tableModel.fireTableDataChanged();

                // table.getSelectionModel().setSelectionInterval(0, 0);
                return null;
            }

        }.start();

    }

    @Override
    public void initPanel() {
        tableModel = new InternalTableModel();
        table = new JXTable(tableModel) {
            private static final long serialVersionUID = -7914266013067863393L;

            @Override
            public TableCellRenderer getCellRenderer(int row, int col) {
                if (col == 0) return super.getCellRenderer(row, col);
                return new TableRenderer();
            }
        };
        table.setSortable(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getTableHeader().setReorderingAllowed(false);

        TableColumn column = null;

        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(40);
                column.setMaxWidth(40);
                break;

            case 3:
                column.setPreferredWidth(40);
                column.setMaxWidth(40);
                break;

            }
        }

        panel.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]", "[fill,grow][]"));
        panel.add(new JScrollPane(table));

        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);

        this.add(tabbed);
    }

    @Override
    public PropertyType hasChanges() {
        return PropertyType.NONE;
    }

    class TableRenderer extends DefaultTableRenderer {

        private static final long serialVersionUID = 1L;

        private Component co;
        private JCheckBox checkbox;
        private JLabel label;

        public TableRenderer() {
            checkbox = new JCheckBox();

            label = new JLabel();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ToolBarAction action;
            switch (column) {
            case 0:
                checkbox.setSelected((Boolean) value);
                return checkbox;
            case 1:
                action = (ToolBarAction) value;
                label.setIcon(null);
                label.setText(action.getTitle());
                return label;
            case 2:
                action = (ToolBarAction) value;
                label.setIcon(null);
                label.setText(action.getTooltipText());
                return label;
            case 3:
                action = (ToolBarAction) value;
                label.setIcon(JDTheme.II(action.getValue(ToolBarAction.IMAGE_KEY) + "", 16, 16));
                label.setText("");
                return label;

            }
            return co;
        }

    }
}
