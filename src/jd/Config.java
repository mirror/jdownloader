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
import jd.gui.swing.dialog.AbstractDialog;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.gui.swing.laf.LookAndFeelController;
import jd.update.JDUpdateUtils;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class Config {

    private final Configuration mainConfig;
    private final ArrayList<SubConfiguration> configs;
    private JComboBox configSelection;
    private SubConfiguration currentConfig;
    private JTable table;
    private ConfigTableModel tableModel;

    private ArrayList<Object> values;
    private ArrayList<String> keys;
    private JButton add;
    private JButton edit;
    private JButton remove;
    private JFrame frame;

    public Config() {

        (new JDInit()).init();
        final JDController controller = JDController.getInstance();
        JDUpdateUtils.backupDataBase();

        System.out.println("Backuped Database");
        mainConfig = JDUtilities.getConfiguration();
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
                if (event.getSource() == tmplaf && event.getID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
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
                    SwingUtilities.updateComponentTreeUI(frame);

                    frame.pack();
                }
            }

        });
        configs = JDUtilities.getDatabaseConnector().getSubConfigurationKeys();
        configs.add(0, mainConfig);

        sort();
        setCurrentConfig(configs.get(0));
        initGUI();
    }

    private void sort() {
        Collections.sort(configs, new Comparator<SubConfiguration>() {

            public int compare(final SubConfiguration o1, final SubConfiguration o2) {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }

        });
    }

    private int[] getSelectedRows() {
        final int[] rows = table.getSelectedRows();
        final int length = rows.length;
        final int[] ret = new int[length];

        for (int i = 0; i < length; ++i) {
            ret[i] = table.convertRowIndexToModel(rows[i]);
        }
        Arrays.sort(ret);
        return ret;
    }

    private void setCurrentConfig(final SubConfiguration cfg) {
        currentConfig = cfg;

        keys = new ArrayList<String>();
        values = new ArrayList<Object>();

        createMap(cfg.getProperties(), keys, values, "");
        if (tableModel != null) {
            this.tableModel.fireTableDataChanged();
        }
    }

    @SuppressWarnings("unchecked")
    private void createMap(final HashMap<?, ?> hashMap, final ArrayList<String> keys, final ArrayList<Object> values, String pre) {
        pre = (pre.length() > 0) ? pre + "/" : "";
        for (final Entry<?, ?> next : hashMap.entrySet()) {
            final String key = pre + next.getKey();
            final Object value = next.getValue();
            keys.add(key);
            values.add(value);
            if (value instanceof HashMap<?, ?>) {
                createMap((HashMap) value, keys, values, key);
            }
        }
    }

    private void initGUI() {
        frame = new JFrame("JDownloader Config - leave any warranty behind you!");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new MigLayout("ins 10,wrap 1", "[grow,fill]", "[][grow,fill]"));
        frame.setMinimumSize(new Dimension(800, 600));

        configSelection = new JComboBox(configs.toArray(new SubConfiguration[] {}));
        configSelection.setEditable(true);
        configSelection.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (configSelection.getSelectedItem() instanceof String) {
                    final SubConfiguration conf = SubConfiguration.getConfig(configSelection.getSelectedItem().toString());
                    configs.add(conf);
                    sort();
                    configSelection.setModel(new DefaultComboBoxModel(configs.toArray(new SubConfiguration[] {})));
                    configSelection.setSelectedItem(conf);
                    setCurrentConfig(conf);
                    tableModel.fireTableDataChanged();
                } else {
                    setCurrentConfig((SubConfiguration) configSelection.getSelectedItem());
                    tableModel.fireTableDataChanged();
                }
            }

        });
        table = new JTable(tableModel = new ConfigTableModel());

        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(100);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                final int[] rows = getSelectedRows();
                if (rows.length == 0) return;
                final Object value = tableModel.getValueAt(rows[0], 1);
                try {
                    new ObjectConverter().toString(value);
                    edit.setEnabled(true);
                    remove.setEnabled(true);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    edit.setEnabled(false);
                    remove.setEnabled(true);
                }
            }

        });
        add = new JButton(JDTheme.II("gui.images.add", 24, 24));
        add.addActionListener(new ActionListener() {

            @SuppressWarnings("unchecked")
            public void actionPerformed(final ActionEvent e) {
                final String key = UserIOGui.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, "Enter Key", "Enter your key use / deliminator to create new sub-maps", "NEW_KEY", null, "Create Entry", "Cancel");
                if (key == null) { return; }
                if (keys.contains(key)) {
                    UserIOGui.getInstance().requestMessageDialog("Key " + key + " is already available. Try Edit feature");
                    return;
                }
                final String result = UserIOGui.getInstance().requestInputDialog(UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, "Edit value for " + key, "Please take care to keep xml structure", "<classtype>VALUE</classtype>\r\n e.g.: <boolean>true</boolean>", null, "Save", "Cancel");
                if (result == null) { return; }
                try {
                    if (result != null) {
                        final ObjectConverter oc = new ObjectConverter();
                        oc.toString(new Object());
                        final Object object = oc.toObject(result);
                        final String[] configKeys = key.toString().split("/");

                        HashMap<String, Object> props = currentConfig.getProperties();

                        System.out.println("Save Object " + key);

                        final int length = configKeys.length;
                        for (int i = 0; i < length; i++) {
                            final String k = configKeys[i];
                            if (i < length) {
                                final Object next = props.get(k);

                                if (next instanceof HashMap<?, ?>) {
                                    System.out.println("sub Hashmap " + k);
                                    props = (HashMap) next;
                                } else {
                                    System.out.println("create sub Hashmap " + k);
                                    props.put(k, props = new HashMap<String, Object>());
                                }
                            }
                        }
                        currentConfig.setProperty(configKeys[length - 1], object);
                        currentConfig.save();
                        setCurrentConfig(currentConfig);
                    }
                } catch (Exception e1) {
                    UserIOGui.getInstance().requestMessageDialog("Could not save object. Failures in XML structure!");
                }
            }

        });
        edit = new JButton(JDTheme.II("gui.images.findandreplace", 24, 24));
        edit.setEnabled(false);
        edit.addActionListener(new ActionListener() {

            @SuppressWarnings("unchecked")
            public void actionPerformed(ActionEvent e) {
                final int[] rows = getSelectedRows();
                if (rows.length == 0) { return; }
                final int row = rows[0];
                final Object key = tableModel.getValueAt(row, 0);
                final Object value = tableModel.getValueAt(row, 1);

                try {
                    AbstractDialog.setDefaultDimension(new Dimension(550, 400));
                    final ObjectConverter oc = new ObjectConverter();
                    final String valuess = oc.toString(value);

                    final String result = UserIOGui.getInstance().requestInputDialog(UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, "Edit value for " + key, "Please take care to keep xml structure", valuess, null, "Save", "Cancel");
                    try {
                        if (result != null) {
                            final Object object = oc.toObject(result);
                            final String[] configKeys = key.toString().split("/");

                            HashMap<String, Object> props = currentConfig.getProperties();
                            String myKey = null;
                            System.out.println("Save Object " + key);

                            for (final String k : configKeys) {
                                final Object next = props.get(k);
                                if (next instanceof HashMap<?, ?>) {
                                    System.out.println("sub Hashmap " + k);
                                    props = (HashMap) next;
                                } else {
                                    myKey = k;
                                    System.out.println("Save Object to key " + k);
                                    break;
                                }
                            }
                            currentConfig.setProperty(myKey, object);
                            currentConfig.save();
                            setCurrentConfig(currentConfig);
                        }
                    } catch (final Exception e1) {
                        e1.printStackTrace();
                        UserIOGui.getInstance().requestMessageDialog("Could not save object. Failures in XML structure!");
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        remove = new JButton(JDTheme.II("gui.images.delete", 24, 24));
        remove.setEnabled(false);
        remove.addActionListener(new ActionListener() {

            @SuppressWarnings("unchecked")
            public void actionPerformed(ActionEvent e) {
                final int[] rows = getSelectedRows();
                if (rows.length == 0) { return; }

                final Object key = tableModel.getValueAt(rows[0], 0);
                final String[] keys = key.toString().split("/");
                final int keysLength = keys.length;
                if (keys[keysLength - 1].equals("null")) {
                    keys[keysLength - 1] = null;
                }
                if (keysLength == 1) {
                    currentConfig.setProperty(keys[0], Property.NULL);
                    currentConfig.save();
                    setCurrentConfig(currentConfig);
                } else {
                    HashMap<String, Object> props = currentConfig.getProperties();
                    for (final String k : keys) {
                        final Object next = props.get(k);
                        if (next instanceof HashMap<?, ?>) {
                            System.out.println("sub Hashmap " + k);
                            props = (HashMap) next;
                        } else if (k != keys[keysLength - 1]) {
                            System.out.println("error key " + k);
                            return;
                        }
                    }
                    currentConfig.setProperty(keys[keysLength - 1], Property.NULL);
                    currentConfig.save();
                    setCurrentConfig(currentConfig);
                }
            }
        });
        add.setOpaque(false);
        add.setBorderPainted(false);
        add.setContentAreaFilled(false);
        edit.setOpaque(false);
        edit.setBorderPainted(false);
        edit.setContentAreaFilled(false);
        remove.setOpaque(false);
        remove.setBorderPainted(false);
        remove.setContentAreaFilled(false);
        frame.add(configSelection, "split 4,pushx,growx");
        frame.add(add, "alignx right");
        frame.add(remove, "alignx right");
        frame.add(edit, "alignx right");
        frame.add(new JScrollPane(table));
        frame.setVisible(true);
        frame.pack();
    }

    private class ConfigTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -5434313385327397539L;

        private final String[] columnNames = { "Key", "Value" };

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return values.size();
        }

        @Override
        public String getColumnName(final int col) {
            return columnNames[col];
        }

        public Object getValueAt(final int row, final int col) {
            try {
                switch (col) {
                case 0:
                    return keys.get(row);
                case 1:
                    try {
                        new ObjectConverter().toString(values.get(row));
                        return values.get(row);
                    } catch (Exception e) {
                        return values.get(row);
                    }
                }
            } catch (Exception e) {
            }
            return "";
        }

        @Override
        public Class<?> getColumnClass(final int c) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(final int row, final int col) {
            return false;
        }

    }
}
