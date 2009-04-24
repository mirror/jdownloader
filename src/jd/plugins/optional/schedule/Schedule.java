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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import net.miginfocom.swing.MigLayout;

public class Schedule extends PluginOptional {

    private static final String PROPERTY_SCHEDULES = "PROPERTY_SCHEDULES";

    private JPanel panel;
    private JPanel aPanel;
    private JPanel menu;

    private JComboBox list;
    private JButton add;
    private JButton remove;
    private JButton show;

    private Timer status;

    private Vector<String> listData = new Vector<String>();
    private Vector<ScheduleFrame> schedules = null;

    private JTabbedPanel tabbedPanel;

    public Schedule(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getIconKey() {

        return "gui.images.config.eventmanager";
    }

    @SuppressWarnings("unchecked")
    private void initGUI() {
        if (schedules != null) return;

        schedules = (Vector<ScheduleFrame>) getPluginConfig().getProperty(PROPERTY_SCHEDULES, new Vector<ScheduleFrame>());
        logger.finer("Scheduler: restored " + schedules.size() + " schedules");
        reloadList();

        tabbedPanel = new JTabbedPanel() {

            private static final long serialVersionUID = 4758934444244058336L;

            @Override
            public void onDisplay() {
            }

            @Override
            public void onHide() {
                getPluginConfig().setProperty(PROPERTY_SCHEDULES, schedules);
                getPluginConfig().save();

                status.stop();
            }

        };

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

        menu = new JPanel(new BorderLayout(5, 5));
        menu.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        menu.add(list, BorderLayout.CENTER);
        menu.add(buttons, BorderLayout.EAST);

        aPanel = new JPanel(new BorderLayout(0, 0));

        panel = new JPanel(new BorderLayout(5, 5));
        panel.add(menu, BorderLayout.NORTH);
        panel.add(aPanel, BorderLayout.CENTER);

        tabbedPanel.setLayout(new MigLayout("ins 0, wrap 1", "[fill,grow]", "[fill,grow]"));
        tabbedPanel.add(panel);
        status = new Timer(1 * 1000, this);
        status.setInitialDelay(1000);
        status.start();

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 0) {
            initGUI();
            SimpleGUI.CURRENTGUI.getContentPane().display(tabbedPanel);
        } else if (e.getSource() == add) {
            schedules.add(new ScheduleFrame(JDLocale.L("addons.schedule.menu.schedule", "Schedule") + " " + (schedules.size() + 1)));
            reloadList();
            list.setSelectedIndex(schedules.size() - 1);
            SwingUtilities.updateComponentTreeUI(aPanel);
        } else if (e.getSource() == remove) {
            schedules.remove(list.getSelectedIndex());
            reloadList();
            list.setSelectedIndex(Math.max(0, schedules.size() - 1));
            setCenterPanel(null);
            renameLabels();
            SwingUtilities.updateComponentTreeUI(aPanel);
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
            SwingUtilities.updateComponentTreeUI(aPanel);
        } else if (e.getSource() == status) {
            int size = schedules.size();

            JPanel infoPanel = new JPanel(new GridLayout(size, 1, 10, 10));
            for (int i = 0; i < size; ++i) {
                ScheduleFrame s = schedules.get(i);
                infoPanel.add(new JLabel(JDLocale.L("addons.schedule.menu.schedule", "Schedule") + " " + (i + 1) + " " + JDLocale.L("addons.schedule.menu.status", "Status") + ": " + s.getStatusLabel().getText()));
            }
            setCenterPanel(infoPanel);
            SwingUtilities.updateComponentTreeUI(aPanel);
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

    public static int getAddonInterfaceVersion() {
        return 3;
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem(getHost(), 0).setActionListener(this));
        return menu;
    }

    @Override
    public String getCoder() {
        return "JD-Team / Tudels";
    }

    @Override
    public String getHost() {
        return JDLocale.L("addons.schedule.name", "Schedule");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public boolean initAddon() {
        logger.info("Schedule OK");
        return true;
    }

    @Override
    public void onExit() {
    }

}
