package jd.plugins.optional.schedule;

import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jd.utils.JDLocale;

public class ScheduleControl extends JDialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    JButton add = new JButton(JDLocale.L("addons.schedule.menu.add", "Add"));
    Choice list = new Choice();
    JPanel menu = new JPanel();
    JPanel panel = new JPanel();
    JButton remove = new JButton(JDLocale.L("addons.schedule.menu.remove", "Remove"));

    JButton show = new JButton(JDLocale.L("addons.schedule.menu.edit", "Edit"));
    Timer status = new Timer(1, this);

    Vector<ScheduleFrame> v = new Vector<ScheduleFrame>();

    boolean visible = false;

    public ScheduleControl() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                status.stop();
            }
        });

        setTitle("Scheduler by Tudels");
        this.setSize(450, 300);
        setResizable(false);
        this.setLocation(300, 300);

        menu.setLayout(new FlowLayout());
        menu.add(new JLabel("          "));
        menu.add(list);
        list.add(JDLocale.L("addons.schedule.menu.create", "Create"));
        menu.add(show);
        menu.add(add);
        menu.add(remove);
        menu.add(new JLabel("          "));

        getContentPane().setLayout(new FlowLayout());
        getContentPane().add(menu);
        getContentPane().add(panel);

        add.addActionListener(this);
        remove.addActionListener(this);
        show.addActionListener(this);

        SwingUtilities.updateComponentTreeUI(this);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == add) {
            int a = v.size() + 1;
            v.add(new ScheduleFrame("Schedule " + a));
            reloadList();
            SwingUtilities.updateComponentTreeUI(this);
        }

        if (e.getSource() == remove) {
            try {
                // ScheduleFrame s = (ScheduleFrame) v.elementAt(list.getSelectedIndex());

                v.remove(list.getSelectedIndex());
                reloadList();
                panel.removeAll();
                renameLabels();
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Exception ex) {
            }
        }

        if (e.getSource() == show) {
            try {
                int item = list.getSelectedIndex();
                ScheduleFrame sched;

                if (visible == false) {
                    status.stop();
                    panel.removeAll();
                    sched = v.elementAt(item);
                    visible = true;
                    panel.add(sched);
                    show.setText(JDLocale.L("addons.schedule.menu.close", "Close"));
                    controls(false);
                } else {
                    visible = false;
                    show.setText(JDLocale.L("addons.schedule.menu.edit", "Edit"));
                    panel.removeAll();
                    status.start();
                    controls(true);
                }
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Exception ex) {
            }
        }

        if (e.getSource() == status) {

            int size = v.size();

            panel.removeAll();
            panel.setLayout(new GridLayout(size, 1));
            for (int i = 0; i < size; ++i) {
                ScheduleFrame s = v.elementAt(i);
                int a = i + 1;
                panel.add(new JLabel("Schedule " + a + " Status: " + s.status.getText()));
            }
            SwingUtilities.updateComponentTreeUI(this);

        }
    }

    public void controls(boolean b) {

        add.setEnabled(b);
        remove.setEnabled(b);
        list.setEnabled(b);

    }

    public void reloadList() {

        list.removeAll();
        int size = v.size();

        String[] s = new String[size + 10];

        for (int i = 1; i <= size; ++i) {
            s[i] = " Schedule " + i;
            list.add(s[i]);
        }
        if (size == 0) {
            list.add("Create");
        }

    }

    public void renameLabels() {
        int size = v.size();
        for (int i = 0; i < size; ++i) {
            ScheduleFrame s = v.elementAt(i);
            s.label.setText(list.getItem(i));
        }

    }
}
