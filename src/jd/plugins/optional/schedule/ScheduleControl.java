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

package jd.plugins.optional.schedule;

import java.awt.BorderLayout;
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
import javax.swing.Timer;

import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class ScheduleControl extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JButton add = new JButton(JDLocale.L("addons.schedule.menu.add", "Add"));
    private Choice list = new Choice();
    private JPanel menu = new JPanel();
    private JPanel panel = new JPanel();
    private JButton remove = new JButton(JDLocale.L("addons.schedule.menu.remove", "Remove"));

    private JButton show = new JButton(JDLocale.L("addons.schedule.menu.edit", "Edit"));
    private Timer status = new Timer(1, this);

    private Vector<ScheduleFrame> schedules = new Vector<ScheduleFrame>();

    private boolean visible = false;

    public ScheduleControl() {
        initGUI();
    }

    private void initGUI() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                status.stop();
            }
        });

        setTitle("Scheduler by Tudels");
        setIconImage(JDTheme.I("gui.images.jd_logo"));
        setSize(450, 300);
        setResizable(false);
        setLocation(300, 300);

        list.add(JDLocale.L("addons.schedule.menu.create", "Create"));

        menu.setLayout(new FlowLayout());
        menu.add(new JLabel("          "));
        menu.add(list);
        menu.add(show);
        menu.add(add);
        menu.add(remove);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(menu, BorderLayout.NORTH);
        getContentPane().add(panel, BorderLayout.CENTER);

        add.addActionListener(this);
        remove.addActionListener(this);
        show.addActionListener(this);

    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == add) {
            schedules.add(new ScheduleFrame(JDLocale.L("addons.schedule.menu.schedule", "Schedule") + " " + (schedules.size() + 1)));
            reloadList();
        } else if (e.getSource() == remove) {
            try {
                schedules.remove(list.getSelectedIndex());
                reloadList();
                panel.removeAll();
                renameLabels();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource() == show) {
            try {
                if (visible == false) {
                    status.stop();
                    panel.removeAll();
                    visible = true;
                    panel.add(schedules.get(list.getSelectedIndex()));
                    show.setText(JDLocale.L("addons.schedule.menu.close", "Close"));
                    enableButtons(false);
                } else {
                    visible = false;
                    show.setText(JDLocale.L("addons.schedule.menu.edit", "Edit"));
                    panel.removeAll();
                    status.start();
                    enableButtons(true);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource() == status) {
            int size = schedules.size();

            panel.removeAll();
            panel.setLayout(new GridLayout(size, 1));
            for (int i = 0; i < size; ++i) {
                ScheduleFrame s = schedules.get(i);
                panel.add(new JLabel(JDLocale.L("addons.schedule.menu.schedule", "Schedule") + " " + (i + 1) + " " + JDLocale.L("addons.schedule.menu.status", "Status") + ": " + s.getStatusLabel().getText()));
            }
        }
    }

    private void enableButtons(boolean b) {
        add.setEnabled(b);
        remove.setEnabled(b);
        list.setEnabled(b);
    }

    private void reloadList() {
        list.removeAll();
        int size = schedules.size();

        for (int i = 1; i <= size; ++i) {
            list.add(" " + JDLocale.L("addons.schedule.menu.schedule", "Schedule") + " " + i);
        }
        if (size == 0) {
            list.add(JDLocale.L("addons.schedule.menu.create", "Create"));
        }
    }

    private void renameLabels() {
        int size = schedules.size();
        for (int i = 0; i < size; ++i) {
            ScheduleFrame s = schedules.get(i);
            s.getLabel().setText(list.getItem(i));
        }

    }

    public Timer getStatus() {
        return status;
    }

}
