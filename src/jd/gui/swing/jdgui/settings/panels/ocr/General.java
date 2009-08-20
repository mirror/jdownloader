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

package jd.gui.swing.jdgui.settings.panels.ocr;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.captcha.JACMethod;
import jd.config.Configuration;
import jd.config.ConfigEntry.PropertyType;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class General extends ConfigPanel {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.ocr.General.";

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "captcha.title", "OCR");
    }

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
            case 0:
                return Boolean.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            case 3:
                return String.class;
            }
            return String.class;
        }

        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDL.L("gui.config.jac.column.use", "Verwenden");
            case 1:
                return JDL.L("gui.config.jac.column.plugin", "Pluginname");
            case 2:
                return JDL.L("gui.config.jac.column.usedmethod", "Verwendete Methode");
            case 3:
                return JDL.L("gui.config.jac.column.author", "Author");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return methods.size();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {

            switch (columnIndex) {
            case 0:
                return configuration.getBooleanProperty(jacKeyForMethod(rowIndex), true);
            case 1:
                return methods.get(rowIndex).getServiceName();
            case 2:
                return methods.get(rowIndex).getFileName();
            case 3:
                return methods.get(rowIndex).getAuthor();
            }
            return null;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                configuration.setProperty(jacKeyForMethod(row), value);
            }
        }
    }

    private static final long serialVersionUID = 1592765387324291781L;

    private ArrayList<JACMethod> methods;

    private JTable table;

    private InternalTableModel tableModel;

    private Configuration configuration;

    public General(Configuration configuration) {
        super();
        this.configuration = configuration;
        methods = JACMethod.getMethods();
      for( Iterator<JACMethod> it = methods.iterator();it.hasNext();){
          JACMethod next = it.next();
          if(next.getServiceName().equalsIgnoreCase("underground cms")){
              it.remove();
              continue;
          }
          if(next.getServiceName().equalsIgnoreCase("uu.canna.to")){
              it.remove();
              continue;
          }
          if(next.getServiceName().equalsIgnoreCase("wii-reloaded.ath.cx")){
              it.remove();
              continue;
          }
          if(next.getServiceName().equalsIgnoreCase("raubkopierer.ws")){
              it.remove();
              continue;
          }
          if(next.getServiceName().equalsIgnoreCase("appscene.org")){
              it.remove();
              continue;
          }
          if(next.getServiceName().equalsIgnoreCase("canna.to")){
              it.remove();
              continue;
          }
          if(next.getServiceName().equalsIgnoreCase("serienfreaks.to")){
              it.remove();
              continue;
          }
          if(next.getServiceName().equalsIgnoreCase("sealed.in")){
              it.remove();
              continue;
          }
      }
        initPanel();
        load();
    }

    @Override
    public void initPanel() {

        tableModel = new InternalTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        // table.addHighlighter(new
        // PainterHighlighter(HighlightPredicate.ROLLOVER_ROW, new
        // MattePainter(Colors.getColor(getBackground().brighter(), 50))));

        TableColumn column = null;
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setMaxWidth(80);
                column.setPreferredWidth(60);
                break;
            case 1:
                column.setPreferredWidth(150);
                break;
            case 2:
                column.setPreferredWidth(600);
                break;
            case 3:
                column.setPreferredWidth(150);
                break;
            }
        }

        // tabbed = new JTabbedPane();
        // tabbed.addTab(JDL.L("gui.config.panels.captcha.methodstab",
        // "OCR methods"), new JScrollPane(table));
        // tabbed.setIconAt(0, JDTheme.II("gui.images.captcha.methods", 16,
        // 16));
        // tabbed.addTab(JDL.L("gui.config.panels.captcha.advancedtab",
        // "Premium settings"), cep = new ConfigEntriesPanel(container));
        // tabbed.setIconAt(1, JDTheme.II("gui.images.config.ocr", 16, 16));

        setLayout(new MigLayout("ins 0,wrap 1", "[fill,grow 10]", "[fill,grow]"));

        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), new JScrollPane(table));

        this.add(tabbed);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {

        saveConfigEntries();
    }

    @Override
    public PropertyType hasChanges() {
        return super.hasChanges();
    }

    private String jacKeyForMethod(int index) {
        return Configuration.PARAM_JAC_METHODS + methods.get(index).getServiceName();
    }

}
