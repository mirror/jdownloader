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

import jd.config.SubConfiguration;
import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class ScheduleControl extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String PROPERTY_SCHEDULES = "PROPERTY_SCHEDULES";

    private SubConfiguration config;

    private JPanel panel;
    private JPanel aPanel;

    private JButton add;
    private JComboBox list;
    private JPanel menu;
    private JButton remove;

    private JButton show;
    private Timer status = new Timer(15 * 1000, this);

    private Vector<String> listData = new Vector<String>();
    private Vector<ScheduleFrame> schedules;

    @SuppressWarnings("unchecked")
    public ScheduleControl(SubConfiguration config) {
        this.config = config;
        schedules = (Vector<ScheduleFrame>) this.config.getProperty(PROPERTY_SCHEDULES, new Vector<ScheduleFrame>());
        reloadList();

        initGUI();
    }

    private void initGUI() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                status.stop();
                config.setProperty(PROPERTY_SCHEDULES, schedules);
            }
        });

        setTitle(JDLocale.L("addons.schedule.name", "Schedule"));
        setModal(true);
        setIconImage(JDTheme.I("gui.images.jd_logo"));
        setSize(450, 300);
        setResizable(false);
        setLocation(300, 300);

        Dimension size = new Dimension(150, 20);
        list = new JComboBox(listData);
        list.addActionListener(this);
        list.setMinimumSize(size);
        list.setPreferredSize(size);
        list.setMaximumSize(size);

        show = new JButton(JDLocale.L("addons.schedule.menu.edit", "Edit"));
        show.addActionListener(this);
        show.setEnabled(schedules.size() > 0);

        add = new JButton(JDLocale.L("addons.schedule.menu.add", "Add"));
        add.addActionListener(this);

        remove = new JButton(JDLocale.L("addons.schedule.menu.remove", "Remove"));
        remove.addActionListener(this);
        remove.setEnabled(schedules.size() > 0);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 5, 5));
        buttons.add(show);
        buttons.add(add);
        buttons.add(remove);

        menu = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        menu.add(list);
        menu.add(buttons);

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
            SwingUtilities.updateComponentTreeUI(this);
        } else if (e.getSource() == remove) {
            schedules.remove(list.getSelectedIndex());
            reloadList();
            list.setSelectedIndex(Math.max(0, schedules.size() - 1));
            setCenterPanel(null);
            renameLabels();
            SwingUtilities.updateComponentTreeUI(this);
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
            SwingUtilities.updateComponentTreeUI(this);
        } else if (e.getSource() == status) {
            int size = schedules.size();

            JPanel infoPanel = new JPanel(new GridLayout(size, 1, 10, 10));
            for (int i = 0; i < size; ++i) {
                ScheduleFrame s = schedules.get(i);
                infoPanel.add(new JLabel(JDLocale.L("addons.schedule.menu.schedule", "Schedule") + " " + (i + 1) + " " + JDLocale.L("addons.schedule.menu.status", "Status") + ": " + s.getStatusLabel().getText()));
            }
            setCenterPanel(infoPanel);
            SwingUtilities.updateComponentTreeUI(this);
        } else if (e.getSource() == list) {
            show.setEnabled(schedules.size() > 0);
            remove.setEnabled(schedules.size() > 0);
        }
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
                listData.add(JDLocale.L("addons.schedule.menu.schedule", "Schedule") + " " + i);
            }
        }
    }

    private void renameLabels() {
        for (int i = 0; i < schedules.size(); ++i) {
            schedules.get(i).getLabel().setText(listData.get(i));
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
