package jd;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.skins.simple.JDMouseAdapter;
import jd.gui.userio.SimpleUserIO;
import jd.gui.userio.dialog.AbstractDialog;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;

public class Config {

    private Configuration mainConfig;
    private ArrayList<SubConfiguration> configs;
    private JComboBox configSelection;
    private SubConfiguration currentConfig;
    private JXTable table;
    private ConfigTableModel tableModel;

    private ArrayList<Object> values;
    private ArrayList<String> keys;
    private JButton add;
    private JButton edit;
    private JButton remove;

    public Config() {
        JDInit jdi = new JDInit();
        jdi.init();

        mainConfig = JDUtilities.getConfiguration();
        configs = JDUtilities.getDatabaseConnector().getSubConfigurationKeys();

        configs.add(0, mainConfig);

        Collections.sort(configs, new Comparator<SubConfiguration>() {

            @Override
            public int compare(SubConfiguration o1, SubConfiguration o2) {
                // TODO Auto-generated method stub
                return o1.toString().compareToIgnoreCase(o2.toString());
            }

        });
        setCurrentConfig(configs.get(0));
        initGUI();

    }

    private void setCurrentConfig(SubConfiguration cfg) {
        currentConfig = cfg;

        keys = new ArrayList<String>();
        values = new ArrayList<Object>();

        createMap(cfg.getProperties(), keys, values, "");

    }

    @SuppressWarnings("unchecked")
    private void createMap(HashMap hashMap, ArrayList<String> keys, ArrayList<Object> values, String pre) {
        for (Iterator<Entry> it = hashMap.entrySet().iterator(); it.hasNext();) {
            Entry next = it.next();
            String key = pre.length() > 0 ? pre + "/" + next.getKey().toString() : next.getKey().toString();
            if (next.getValue() instanceof HashMap) {
                createMap((HashMap) next.getValue(), keys, values, key);
            } else {
                keys.add(key);
                values.add(next.getValue());

            }
        }

    }

    private void createMap(HashMap hashMap, HashMap<String, Object> map, String pre) {

    }

    private void initGUI() {
        JFrame frame = new JFrame("JDownloader Config - leave any warranty behind you!");
        frame.setLayout(new MigLayout("ins 10,wrap 1", "[grow,fill]", "[][grow,fill]"));
        frame.setPreferredSize(new Dimension(800, 600));

        configSelection = new JComboBox(configs.toArray(new SubConfiguration[] {}));
        configSelection.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                setCurrentConfig((SubConfiguration) configSelection.getSelectedItem());
                tableModel.fireTableDataChanged();
            }

        });
        table = new JXTable(tableModel = new ConfigTableModel());

        table.getTableHeader().setReorderingAllowed(false);

        table.getColumn(0).setPreferredWidth(100);
        table.addMouseListener(new JDMouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                Object key = tableModel.getValueAt(row, 0);
                Object value = tableModel.getValueAt(row, 1);

                try {
                    AbstractDialog.setDefaultDimension(new Dimension(550, 400));
                    ObjectConverter oc = new ObjectConverter();
                    String valuess = oc.toString(value);

                    String result = SimpleUserIO.getInstance().requestInputDialog(UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, "Edit value for " + key, "Please take care to keep xml structure", valuess, null, "Save", "Cancel");
                    try {

                        if (result != null) {
                            Object object = oc.toObject(result);
                            String[] configKeys = key.toString().split("/");

                            HashMap<String, Object> props = currentConfig.getProperties();
                            String myKey = null;
                            System.out.println("Save Object " + key);

                            for (String k : configKeys) {
                                Object next = props.get(k);
                                if (next instanceof HashMap) {
                                    System.out.println("sub Hashmap " + k);
                                    props = (HashMap) next;
                                } else {
                                    myKey = k;
                                    System.out.println("Save Object to key " + k);
                                    break;
                                }
                            }

                            props.put(myKey, object);
                            currentConfig.save();
                            setCurrentConfig(currentConfig);
                        }

                    } catch (Exception e1) {
                        SimpleUserIO.getInstance().requestMessageDialog("Could not save object. Failures in XML structure!");

                    }
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }
        });
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoStartEditOnKeyStroke(false);

          add = new JButton(JDTheme.II("gui.images.add", 24, 24));
         edit = new JButton(JDTheme.II("gui.images.edit", 24, 24));
         remove = new JButton(JDTheme.II("gui.images.delete", 24, 24));
        frame.add(configSelection, "split 4");
        frame.add(add,"alignx right");
        frame.add(remove,"alignx right");
        frame.add(edit,"alignx right");
        frame.add(table);
        frame.setVisible(true);
        frame.pack();

    }

    private class ConfigTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -5434313385327397539L;

        private String[] columnNames = { "Key", "Value" };

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return values.size();
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {

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
            return "";
        }

        private Entry<String, Object> getEntry(int row, int col) {
            Iterator<Entry<String, Object>> it = currentConfig.getProperties().entrySet().iterator();
            Entry<String, Object> ret = null;
            while (it.hasNext()) {
                ret = it.next();

            }
            return ret;

        }

        @Override
        public Class<?> getColumnClass(int c) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 5;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 2) {

            }
        }

    }

}
