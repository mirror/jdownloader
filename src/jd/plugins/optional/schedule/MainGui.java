package jd.plugins.optional.schedule;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import jd.config.SubConfiguration;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class MainGui extends SwitchPanel implements ActionListener, MouseListener {
    private static final long serialVersionUID = 3439995751143746593L;

    private static final String JDL_PREFIX = "jd.plugins.optional.schedule.MainGui.";

    private MyTableModel tableModel;

    private JTable table;

    private JButton add;

    private JButton delete;

    private JButton edit;

    private JTabbedPane tabs;

    private SimpleDateFormat time;

    private SimpleDateFormat date;

    private static MainGui instance;

    public MainGui(SubConfiguration cfg) {
        instance = this;

        time = new SimpleDateFormat("HH:mm");
        date = new SimpleDateFormat("dd.MM.yyyy");

        setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]", "[fill,grow][]"));

        tabs = new JTabbedPane();
        JPanel p = new JPanel();

        tableModel = new MyTableModel();
        table = new JTable(tableModel);
        table.addMouseListener(this);
        table.getColumnModel().getColumn(0).setMaxWidth(30);

        p.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]", "[fill,grow][]"));
        p.add(new JScrollPane(table));

        add = new JButton("+");
        add.addActionListener(this);

        delete = new JButton("-");
        delete.addActionListener(this);
        delete.setEnabled(false);

        edit = new JButton(JDL.L(JDL_PREFIX + "edit", "Edit"));
        edit.addActionListener(this);
        edit.setEnabled(false);

        JPanel buttons = new JPanel();
        buttons.add(add);
        buttons.add(delete);
        buttons.add(edit);

        buttons.add(new JSeparator());

        p.add(buttons);
        tabs.addTab("Main", p);
        this.add(tabs);

        tableModel.fireTableRowsInserted(0, Schedule.getInstance().getActions().size());
    }

    public static MainGui getInstance() {
        return instance;
    }

    public void addAction(Actions act) {
        tableModel.fireTableRowsInserted(Schedule.getInstance().getActions().size(), Schedule.getInstance().getActions().size());

        removeTab(act);
    }

    public void updateAction(Actions act) {
        tableModel.fireTableRowsUpdated(0, Schedule.getInstance().getActions().size());

        removeTab(act);
    }

    public void removeTab(Actions act) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (tabs.getTitleAt(i).equals(act.getName())) {
                tabs.remove(i);
                return;
            }
        }
    }

    private class MyTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -2404339596786592942L;

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return "";
            case 1:
                return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.name", "Name");
            case 2:
                return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.date", "Date");
            case 3:
                return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.time", "Time");
            case 4:
                return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.repeats", "Repeats");
            case 5:
                return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.number", "# of actions");
            }
            return super.getColumnName(column);
        }

        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                String name = (String) table.getValueAt(row, 1);

                for (Actions a : Schedule.getInstance().getActions()) {
                    if (name.equals(a.getName())) {
                        a.setEnabled((Boolean) value);
                        Schedule.getInstance().save();
                        return;
                    }
                }
            }
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        public int getColumnCount() {
            return 6;
        }

        public int getRowCount() {
            return Schedule.getInstance().getActions().size();
        }

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
            case 4:
                return String.class;
            case 5:
                return Integer.class;
            }

            return null;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0:
                return Schedule.getInstance().getActions().get(rowIndex).isEnabled();
            case 1:
                return Schedule.getInstance().getActions().get(rowIndex).getName();
            case 2:
                return date.format(Schedule.getInstance().getActions().get(rowIndex).getDate());
            case 3:
                return time.format(Schedule.getInstance().getActions().get(rowIndex).getDate());
            case 4:
                switch (Schedule.getInstance().getActions().get(rowIndex).getRepeat()) {
                case 0:
                    return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.once", "Only once");
                case 60:
                    return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.hourly", "Hourly");
                case 1440:
                    return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.daily", "Daily");
                case 10080:
                    return JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.weekly", "Weekly");
                default:
                    int hour = Schedule.getInstance().getActions().get(rowIndex).getRepeat() / 60;
                    return JDL.LF("jd.plugins.optional.schedule.MainGui.MyTableModel.add.interval", "Interval: %sh %sm", hour, Schedule.getInstance().getActions().get(rowIndex).getRepeat() - (hour * 60));
                }
            case 5:
                return Schedule.getInstance().getActions().get(rowIndex).getExecutions().size();
            }

            return null;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == add) {
            tabs.addTab("Schedule " + new Integer(Schedule.getInstance().getActions().size() + 1), new AddGui(new Actions("Schedule " + new Integer(Schedule.getInstance().getActions().size() + 1)), false));
            tabs.setSelectedIndex(tabs.getTabCount() - 1);
        } else if (e.getSource() == delete) {
            Schedule.getInstance().removeAction(table.getSelectedRow());
            tableModel.fireTableRowsDeleted(table.getSelectedRow(), table.getSelectedRow());
            delete.setEnabled(false);
            edit.setEnabled(false);
        } else if (e.getSource() == edit) {
            Actions a = Schedule.getInstance().getActions().get(table.getSelectedRow());
            tabs.addTab(a.getName(), new AddGui(a, true));
            tabs.setSelectedIndex(tabs.getTabCount() - 1);
        }
    }

    public void changeTabText(String oldText, String newText) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (tabs.getTitleAt(i).equals(oldText)) {
                tabs.setTitleAt(i, newText);
                return;
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (table.getSelectedColumnCount() > 0) {
            delete.setEnabled(true);
            edit.setEnabled(true);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void onHide() {
    }

    public void onShow() {
    }


}