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

package jd.gui.skins.simple.config.panels;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import jd.captcha.JAntiCaptcha;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.ConfigEntry.PropertyType;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class ConfigPanelCaptcha extends ConfigPanel implements MouseListener {

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
            case 0:
                return Boolean.class;
            case 1:
                return String.class;
            }
            return String.class;
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDLocale.L("gui.config.jac.column.use", "Verwenden");
            case 1:
                return JDLocale.L("gui.config.jac.column.method", "Methode");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return methods.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {

            switch (columnIndex) {
            case 0:
                return configuration.getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[rowIndex].getName(), true);
            case 1:
                return methods[rowIndex].getName() + " : " + (configuration.getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[rowIndex].getName(), true) ? JDLocale.L("gui.config.jac.status.auto", "Automatische Erkennung") : JDLocale.L("gui.config.jac.status.noauto", "Manuelle Eingabe"));
            }
            return null;
        }
    }

    private static final long serialVersionUID = 1592765387324291781L;

    private ConfigEntriesPanel cep;

    private Configuration configuration;

    private ConfigContainer container;

    private File[] methods;

    private JTable table;

    private InternalTableModel tableModel;

    public ConfigPanelCaptcha(Configuration configuration) {
        super();
        this.configuration = configuration;
        methods = JAntiCaptcha.getMethods("jd/captcha/methods/");
        initPanel();
        load();
    }

    public void initPanel() {
        setupContainer();
        panel.setLayout(new MigLayout("ins 0,wrap 2", "[fill,grow 10]10[fill,grow]","[][][fill,grow]"));
        panel.add(cep = new ConfigEntriesPanel(container),"spanx");
     
        tableModel = new InternalTableModel();
        table = new JTable(tableModel);
        table.getTableHeader().setPreferredSize(new Dimension(-1, 25));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setEditingRow(0);
        table.addMouseListener(this);

        TableColumn column = null;
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setMaxWidth(80);
                column.setPreferredWidth(50);

                break;
            case 1:
                column.setPreferredWidth(600);
                break;
            }
        }
        JScrollPane sp = new JScrollPane(table);
        panel.add(getHeader(new ConfigGroup(JDLocale.L("gui.config.captcha.list", "Captcha Methods"), JDTheme.II("gui.images.captcha.methods", 32, 32))), "spanx,gaptop 15,gapleft 20, gapright 20");

        panel.add(sp, "spanx,gapleft 55, gapright 40");
        add(panel);
    }

    public void load() {
        loadConfigEntries();
    }

    public void mouseClicked(MouseEvent e) {
        configuration.setProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[table.getSelectedRow()].getName(), !configuration.getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[table.getSelectedRow()].getName(), true));
        tableModel.fireTableRowsUpdated(table.getSelectedRow(), table.getSelectedRow());
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void save() {
        cep.save();
        saveConfigEntries();
    }

    public PropertyType hasChanges() {

        return PropertyType.getMax(super.hasChanges(), cep.hasChanges());
    }

    public void setupContainer() {
        ConfigEntry ce1;
        ConfigEntry ce2;

        container = new ConfigContainer(this);
        container.setGroup(new ConfigGroup(JDLocale.L("gui.config.captcha.settings", "Captcha settings"), JDTheme.II("gui.images.config.ocr", 32, 32)));
        container.addEntry(ce1 = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_CAPTCHA_JAC_DISABLE, JDLocale.L("gui.config.captcha.jac_disable", "Automatische Bilderkennung abschalten")).setDefaultValue(false));
        container.addEntry(ce2 = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getSubConfig("JAC"), Configuration.JAC_SHOW_TIMEOUT, JDLocale.L("gui.config.captcha.train.show_timeout", "Anzeigedauer des Eingabefensters"), 0, 600).setDefaultValue(20));
        ce2.setEnabledCondidtion(ce1, "==", false);
        container.addEntry(ce2 = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getSubConfig("JAC"), Configuration.AUTOTRAIN_ERROR_LEVEL, JDLocale.L("gui.config.captcha.train.level", "Anzeigeschwelle"), 0, 100).setDefaultValue(95));

        ce2.setEnabledCondidtion(ce1, "==", false);
    }

}
