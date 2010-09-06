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

package jd;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.laf.LookAndFeelController;
import jd.update.JDUpdateUtils;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class Config {

    private class ConfigTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -5434313385327397539L;

        private final String[]    columnNames      = { "Key", "Value" };

        @Override
        public Class<?> getColumnClass(final int c) {
            return String.class;
        }

        public int getColumnCount() {
            return this.columnNames.length;
        }

        @Override
        public String getColumnName(final int col) {
            return this.columnNames[col];
        }

        public int getRowCount() {
            return Config.this.values.size();
        }

        public Object getValueAt(final int row, final int col) {
            try {
                switch (col) {
                case 0:
                    return Config.this.keys.get(row);
                case 1:
                    try {
                        new ObjectConverter().toString(Config.this.values.get(row));
                        return Config.this.values.get(row);
                    } catch (final Exception e) {
                        return Config.this.values.get(row);
                    }
                }
            } catch (final Exception e) {
            }
            return "";
        }

        @Override
        public boolean isCellEditable(final int row, final int col) {
            return false;
        }

    }

    private final Configuration               mainConfig;
    private final ArrayList<SubConfiguration> configs;
    private JComboBox                         configSelection;
    private SubConfiguration                  currentConfig;
    private JTable                            table;

    private ConfigTableModel                  tableModel;
    private ArrayList<Object>                 values;
    private ArrayList<String>                 keys;
    private JButton                           add;
    private JButton                           edit;
    private JButton                           remove;

    private JFrame                            frame;

    public Config() {

        new JDInit().init();
        final JDController controller = JDController.getInstance();
        JDUpdateUtils.backupDataBase();

        System.out.println("Backuped Database");
        this.mainConfig = JDUtilities.getConfiguration();
        LookAndFeelController.setUIManager();
        final SubConfiguration laf = SubConfiguration.getConfig(LookAndFeelController.DEFAULT_PREFIX + "." + LookAndFeelController.getPlaf().getClassName());
        final SubConfiguration tmplaf = SubConfiguration.getConfig("CURRENT_LOOK_AND_FEEL");
        tmplaf.setProperties(new HashMap<String, Object>());

        tmplaf.getProperties().putAll(laf.getProperties());
        final UIDefaults defaults = UIManager.getDefaults();

        final Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            final Object key = keys.nextElement();
            if (key instanceof String) {
                final String strKey = (String) key;
                if (!tmplaf.hasProperty(strKey)) {
                    System.out.println("ORG UI Defauls: " + strKey + " : " + defaults.get(strKey));
                    // if(next.getKey().toString().equals("PopupMenuUI")){
                    // System.out.println("ORG UI Defauls: " + next);
                    // }
                    tmplaf.setProperty(strKey, defaults.get(strKey));
                }
            }
        }

        controller.addControlListener(new ControlListener() {

            public void controlEvent(final ControlEvent event) {
                if (event.getCaller() == tmplaf && event.getEventID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
                    final String strParam = event.getParameter().toString();
                    Object value = tmplaf.getProperty(strParam);
                    if (value == null) {
                        value = Property.NULL;
                    }
                    laf.setProperty(strParam, value);
                    if (value != null && value != Property.NULL) {
                        UIManager.put(strParam, value);
                    }
                    laf.save();
                    SwingUtilities.updateComponentTreeUI(Config.this.frame);

                    Config.this.frame.pack();
                }
            }

        });
        this.configs = JDUtilities.getDatabaseConnector().getSubConfigurationKeys();
        this.configs.add(0, this.mainConfig);

        this.sort();
        this.setCurrentConfig(this.configs.get(0));
        this.initGUI();
    }

    private void createMap(final HashMap<?, ?> hashMap, final ArrayList<String> keys, final ArrayList<Object> values, String pre) {
        pre = pre.length() > 0 ? pre + "/" : "";
        for (final Entry<?, ?> next : hashMap.entrySet()) {
            final String key = pre + next.getKey();
            final Object value = next.getValue();
            keys.add(key);
            values.add(value);
            if (value instanceof HashMap<?, ?>) {
                this.createMap((HashMap<?, ?>) value, keys, values, key);
            }
        }
    }

    private int[] getSelectedRows() {
        final int[] rows = this.table.getSelectedRows();
        final int length = rows.length;
        final int[] ret = new int[length];

        for (int i = 0; i < length; ++i) {
            ret[i] = this.table.convertRowIndexToModel(rows[i]);
        }
        Arrays.sort(ret);
        return ret;
    }

    private void initGUI() {
        this.frame = new JFrame("JDownloader Config - leave any warranty behind you!");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLayout(new MigLayout("ins 10,wrap 1", "[grow,fill]", "[][grow,fill]"));
        this.frame.setMinimumSize(new Dimension(800, 600));

        this.configSelection = new JComboBox(this.configs.toArray(new SubConfiguration[] {}));
        this.configSelection.setEditable(true);
        this.configSelection.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (Config.this.configSelection.getSelectedItem() instanceof String) {
                    final SubConfiguration conf = SubConfiguration.getConfig(Config.this.configSelection.getSelectedItem().toString());
                    Config.this.configs.add(conf);
                    Config.this.sort();
                    Config.this.configSelection.setModel(new DefaultComboBoxModel(Config.this.configs.toArray(new SubConfiguration[] {})));
                    Config.this.configSelection.setSelectedItem(conf);
                    Config.this.setCurrentConfig(conf);
                    Config.this.tableModel.fireTableDataChanged();
                } else {
                    Config.this.setCurrentConfig((SubConfiguration) Config.this.configSelection.getSelectedItem());
                    Config.this.tableModel.fireTableDataChanged();
                }
            }

        });
        this.table = new JTable(this.tableModel = new ConfigTableModel());

        this.table.getTableHeader().setReorderingAllowed(false);

        this.table.getColumnModel().getColumn(0).setPreferredWidth(100);

        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                final int[] rows = Config.this.getSelectedRows();
                if (rows.length == 0) { return; }
                final Object value = Config.this.tableModel.getValueAt(rows[0], 1);
                try {
                    new ObjectConverter().toString(value);
                    Config.this.edit.setEnabled(true);
                    Config.this.remove.setEnabled(true);
                } catch (final Exception e1) {
                    e1.printStackTrace();
                    Config.this.edit.setEnabled(false);
                    Config.this.remove.setEnabled(true);
                }
            }

        });
        this.add = new JButton(JDTheme.II("gui.images.add", 24, 24));
        this.add.addActionListener(new ActionListener() {

            @SuppressWarnings("unchecked")
            public void actionPerformed(final ActionEvent e) {
                final String key = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, "Enter Key", "Enter your key use / deliminator to create new sub-maps", "NEW_KEY", null, "Create Entry", "Cancel");
                if (key == null) { return; }
                if (Config.this.keys.contains(key)) {
                    UserIO.getInstance().requestMessageDialog("Key " + key + " is already available. Try Edit feature");
                    return;
                }
                final String result = UserIO.getInstance().requestInputDialog(UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, "Edit value for " + key, "Please take care to keep xml structure", "<classtype>VALUE</classtype>\r\n e.g.: <boolean>true</boolean>", null, "Save", "Cancel");
                if (result == null) { return; }
                try {
                    if (result != null) {
                        final ObjectConverter oc = new ObjectConverter();
                        oc.toString(new Object());
                        final Object object = oc.toObject(result);
                        final String[] configKeys = key.toString().split("/");

                        HashMap<String, Object> props = Config.this.currentConfig.getProperties();

                        System.out.println("Save Object " + key);

                        final int length = configKeys.length;
                        for (int i = 0; i < length; i++) {
                            final String k = configKeys[i];
                            if (i < length) {
                                final Object next = props.get(k);

                                if (next instanceof HashMap<?, ?>) {
                                    System.out.println("sub Hashmap " + k);
                                    props = (HashMap<String, Object>) next;
                                } else {
                                    System.out.println("create sub Hashmap " + k);
                                    props.put(k, props = new HashMap<String, Object>());
                                }
                            }
                        }
                        Config.this.currentConfig.setProperty(configKeys[length - 1], object);
                        Config.this.currentConfig.save();
                        Config.this.setCurrentConfig(Config.this.currentConfig);
                    }
                } catch (final Exception e1) {
                    UserIO.getInstance().requestMessageDialog("Could not save object. Failures in XML structure!");
                }
            }

        });
        this.edit = new JButton(JDTheme.II("gui.images.findandreplace", 24, 24));
        this.edit.setEnabled(false);
        this.edit.addActionListener(new ActionListener() {

            @SuppressWarnings("unchecked")
            public void actionPerformed(final ActionEvent e) {
                final int[] rows = Config.this.getSelectedRows();
                if (rows.length == 0) { return; }
                final int row = rows[0];
                final Object key = Config.this.tableModel.getValueAt(row, 0);
                final Object value = Config.this.tableModel.getValueAt(row, 1);

                try {

                    final ObjectConverter oc = new ObjectConverter();
                    final String valuess = oc.toString(value);

                    final String result = UserIO.getInstance().requestInputDialog(UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, "Edit value for " + key, "Please take care to keep xml structure", valuess, null, "Save", "Cancel");
                    try {
                        if (result != null) {
                            final Object object = oc.toObject(result);
                            final String[] configKeys = key.toString().split("/");

                            HashMap<String, Object> props = Config.this.currentConfig.getProperties();
                            String myKey = null;
                            System.out.println("Save Object " + key);

                            for (final String k : configKeys) {
                                final Object next = props.get(k);
                                if (next instanceof HashMap<?, ?>) {
                                    System.out.println("sub Hashmap " + k);
                                    props = (HashMap<String, Object>) next;
                                } else {
                                    myKey = k;
                                    System.out.println("Save Object to key " + k);
                                    break;
                                }
                            }
                            Config.this.currentConfig.setProperty(myKey, object);
                            Config.this.currentConfig.save();
                            Config.this.setCurrentConfig(Config.this.currentConfig);
                        }
                    } catch (final Exception e1) {
                        e1.printStackTrace();
                        UserIO.getInstance().requestMessageDialog("Could not save object. Failures in XML structure!");
                    }
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        this.remove = new JButton(JDTheme.II("gui.images.delete", 24, 24));
        this.remove.setEnabled(false);
        this.remove.addActionListener(new ActionListener() {

            @SuppressWarnings("unchecked")
            public void actionPerformed(final ActionEvent e) {
                final int[] rows = Config.this.getSelectedRows();
                if (rows.length == 0) { return; }

                final Object key = Config.this.tableModel.getValueAt(rows[0], 0);
                final String[] keys = key.toString().split("/");
                final int keysLength = keys.length;
                if (keys[keysLength - 1].equals("null")) {
                    keys[keysLength - 1] = null;
                }
                if (keysLength == 1) {
                    Config.this.currentConfig.setProperty(keys[0], Property.NULL);
                    Config.this.currentConfig.save();
                    Config.this.setCurrentConfig(Config.this.currentConfig);
                } else {
                    HashMap<String, Object> props = Config.this.currentConfig.getProperties();
                    for (final String k : keys) {
                        final Object next = props.get(k);
                        if (next instanceof HashMap<?, ?>) {
                            System.out.println("sub Hashmap " + k);
                            props = (HashMap<String, Object>) next;
                        } else if (k != keys[keysLength - 1]) {
                            System.out.println("error key " + k);
                            return;
                        }
                    }
                    Config.this.currentConfig.setProperty(keys[keysLength - 1], Property.NULL);
                    Config.this.currentConfig.save();
                    Config.this.setCurrentConfig(Config.this.currentConfig);
                }
            }
        });
        this.add.setOpaque(false);
        this.add.setBorderPainted(false);
        this.add.setContentAreaFilled(false);
        this.edit.setOpaque(false);
        this.edit.setBorderPainted(false);
        this.edit.setContentAreaFilled(false);
        this.remove.setOpaque(false);
        this.remove.setBorderPainted(false);
        this.remove.setContentAreaFilled(false);
        this.frame.add(this.configSelection, "split 4,pushx,growx");
        this.frame.add(this.add, "alignx right");
        this.frame.add(this.remove, "alignx right");
        this.frame.add(this.edit, "alignx right");
        this.frame.add(new JScrollPane(this.table));
        this.frame.setVisible(true);
        this.frame.pack();
    }

    private void setCurrentConfig(final SubConfiguration cfg) {
        this.currentConfig = cfg;

        this.keys = new ArrayList<String>();
        this.values = new ArrayList<Object>();

        this.createMap(cfg.getProperties(), this.keys, this.values, "");
        if (this.tableModel != null) {
            this.tableModel.fireTableDataChanged();
        }
    }

    private void sort() {
        Collections.sort(this.configs, new Comparator<SubConfiguration>() {

            public int compare(final SubConfiguration o1, final SubConfiguration o2) {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }

        });
    }
}
