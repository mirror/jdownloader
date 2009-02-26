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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class ScheduleControl extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JPanel panel;
    private JPanel aPanel;

    private JButton add;
    private JComboBox list;
    private JPanel menu;
    private JButton remove;

    private JButton show;
    private Timer status = new Timer(1, this);

    private Vector<String> listData = new Vector<String>();
    private Vector<ScheduleFrame> schedules = new Vector<ScheduleFrame>();

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
        setModal(true);
        setIconImage(JDTheme.I("gui.images.jd_logo"));
        setSize(450, 300);
        setResizable(false);
        setLocation(300, 300);

        Dimension size = new Dimension(150, 20);
        listData.add(JDLocale.L("addons.schedule.menu.create", "Create"));
        list = new JComboBox(listData);
        list.addActionListener(this);
        list.setMinimumSize(size);
        list.setPreferredSize(size);
        list.setMaximumSize(size);

        show = new JButton(JDLocale.L("addons.schedule.menu.edit", "Edit"));
        show.setEnabled(false);
        show.addActionListener(this);

        add = new JButton(JDLocale.L("addons.schedule.menu.add", "Add"));
        add.addActionListener(this);

        remove = new JButton(JDLocale.L("addons.schedule.menu.remove", "Remove"));
        remove.setEnabled(false);
        remove.addActionListener(this);

        menu = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        menu.add(list);
        menu.add(show);
        menu.add(add);
        menu.add(remove);

        aPanel = new JPanel(new BorderLayout(0, 0));

        panel = new JPanel(new BorderLayout());
        panel.add(menu, BorderLayout.NORTH);
        panel.add(aPanel, BorderLayout.CENTER);

        setContentPane(panel);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == add) {
            schedules.add(new ScheduleFrame(JDLocale.L("addons.schedule.menu.schedule", "Schedule") + " " + (schedules.size() + 1)));
            reloadList();
            list.setSelectedIndex(schedules.size() - 1);
        } else if (e.getSource() == remove) {
            schedules.remove(list.getSelectedIndex());
            reloadList();
            list.setSelectedIndex(schedules.size() - 1);
            setCenterPanel(null);
            renameLabels();
        } else if (e.getSource() == show) {
            if (show.getText().equals(JDLocale.L("addons.schedule.menu.edit", "Edit"))) {
                show.setText(JDLocale.L("addons.schedule.menu.close", "Close"));
                setCenterPanel(schedules.get(list.getSelectedIndex()));
                status.stop();
                changeControls(false);
            } else {
                show.setText(JDLocale.L("addons.schedule.menu.edit", "Edit"));
                setCenterPanel(null);
                status.start();
                changeControls(true);
            }
        } else if (e.getSource() == status) {
            int size = schedules.size();

            JPanel infoPanel = new JPanel(new GridLayout(size, 1));
            for (int i = 0; i < size; ++i) {
                ScheduleFrame s = schedules.get(i);
                infoPanel.add(new JLabel(JDLocale.L("addons.schedule.menu.schedule", "Schedule") + " " + (i + 1) + " " + JDLocale.L("addons.schedule.menu.status", "Status") + ": " + s.getStatusLabel().getText()));
            }
            setCenterPanel(infoPanel);
        } else if (e.getSource() == list) {
            remove.setEnabled(schedules.size() > 0);
            show.setEnabled(schedules.size() > 0);
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void changeControls(boolean b) {
        add.setEnabled(b);
        remove.setEnabled(b);
        list.setEnabled(b);
    }

    private void reloadList() {
        listData.clear();
        int size = schedules.size();

        if (size == 0) {
            listData.add(JDLocale.L("addons.schedule.menu.create", "Create"));
        } else {
            for (int i = 1; i <= size; ++i) {
                listData.add(" " + JDLocale.L("addons.schedule.menu.schedule", "Schedule") + " " + i);
            }
        }

        list.invalidate();
    }

    private void renameLabels() {
        for (int i = 0; i < schedules.size(); ++i) {
            ScheduleFrame s = schedules.get(i);
            s.getLabel().setText(listData.get(i));
        }
    }

    private void setCenterPanel(final JPanel newPanel) {
        aPanel.removeAll();
        if (newPanel != null) aPanel.add(newPanel, BorderLayout.CENTER);
    }

    public Timer getStatus() {
        return status;
    }

}
