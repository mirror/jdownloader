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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

import jd.config.Configuration;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ScheduleFrame extends JPanel implements ActionListener {

    private static final long serialVersionUID = 3069680111186861393L;

    private final String dateFormat = "HH:mm:ss | dd.MM.yy";

    private Timer c;
    private SpinnerDateModel date_model;
    private JLabel label;
    private JSpinner maxdls;
    private JSpinner maxspeed;
    private JCheckBox premium;
    private JCheckBox reconnect;
    private JSpinner repeat;
    private JButton start;
    private JLabel status;
    private JCheckBox stop_start;
    private Timer t;
    private JSpinner time;

    // Konstruktor des Fensters und Aussehen
    public ScheduleFrame(String title) {
        initGUI(title);
    }

    private void initGUI(String title) {
        c = new Timer(1000, this);

        t = new Timer(10000, this);

        date_model = new SpinnerDateModel();

        start = new JButton(JDLocale.L("addons.schedule.menu.start", "Start"));
        start.setBorderPainted(false);
        start.setFocusPainted(false);

        maxdls = new JSpinner(new SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2), 1, 10, 1));
        maxdls.setBorder(BorderFactory.createEmptyBorder());

        maxspeed = new JSpinner(new SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED), 0, 50000, 50));
        maxspeed.setBorder(BorderFactory.createEmptyBorder());

        time = new JSpinner(date_model);
        time.setToolTipText("Select your time. Format: HH:mm:ss | dd.MM.yy");
        time.setEditor(new JSpinner.DateEditor(time, dateFormat));
        time.setBorder(BorderFactory.createEmptyBorder());

        repeat = new JSpinner(new SpinnerNumberModel(0, 0, 24, 1));
        repeat.setBorder(BorderFactory.createEmptyBorder());
        repeat.setToolTipText("Enter h | 0 = disable");

        premium = new JCheckBox();
        premium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM));

        reconnect = new JCheckBox();
        reconnect.setSelected(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT));

        status = new JLabel(JDLocale.L("addons.schedule.menu.running", " Not Running!"));

        stop_start = new JCheckBox();

        setLayout(new GridLayout(9, 2));

        this.add(new JLabel(JDLocale.L("addons.schedule.menu.maxdl", " max. Downloads")));
        this.add(maxdls);

        this.add(new JLabel(JDLocale.L("addons.schedule.menu.maxspeed", " max. DownloadSpeed")));
        this.add(maxspeed);

        this.add(new JLabel(JDLocale.L("addons.schedule.menu.premium", "Premium")));
        this.add(premium);

        this.add(new JLabel(JDLocale.L("addons.schedule.menu.reconnect", " Reconnect ?")));
        this.add(reconnect);

        this.add(new JLabel(JDLocale.L("addons.schedule.menu.start_stop", " Start/Stop DL ?")));
        this.add(stop_start);

        this.add(new JLabel(JDLocale.L("addons.schedule.menu.time", " Select Time:")));
        this.add(time);

        this.add(new JLabel(JDLocale.L("addons.schedule.menu.redo", " Redo in h:")));
        this.add(repeat);

        label = new JLabel(title);
        this.add(label);
        this.add(start);

        this.add(status);

        start.addActionListener(this);
        t.setRepeats(false);
    }

    public void actionPerformed(ActionEvent e) {
        int var = parseTime();

        if (e.getSource() == start) {
            if (var > 0) {
                if (t.isRunning() == false || c.isRunning() == false) {
                    start.setText(JDLocale.L("addons.schedule.menu.stop", "Stop"));
                    t.setInitialDelay(var);
                    t.start();
                    c.start();
                    status.setText(JDLocale.L("addons.schedule.menu.started", "Started!"));
                    time.setEnabled(false);
                } else {
                    start.setText(JDLocale.L("addons.schedule.menu.start", "Start"));
                    t.stop();
                    c.stop();
                    status.setText(JDLocale.L("gui.btn_cancel", " Aborted!"));
                    time.setEnabled(true);
                }
            } else {
                status.setText(JDLocale.L("addons.schedule.menu.p_time", " Select positive time!"));
            }
        }

        if (e.getSource() == t) {

            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, maxspeed.getValue());
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, maxdls.getValue());
            JDUtilities.getSubConfig("DOWNLOAD").save();
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, premium.isSelected());
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, !reconnect.isSelected());
            JDUtilities.getConfiguration().save();
            if (stop_start.isSelected() == true) {
                JDUtilities.getController().toggleStartStop();
            }
            if ((Integer) repeat.getValue() > 0) {
                int r = (Integer) repeat.getValue();
                Date new_time = date_model.getDate();
                long var2 = new_time.getTime();
                var2 = var2 + r * 3600000;
                new_time.setTime(var2);
                date_model.setValue(new_time);
                var = parseTime();
                t.setInitialDelay(var);
                t.start();
            } else {
                start.setText(JDLocale.L("addons.schedule.menu.start", "Start"));
                c.stop();
                status.setText(JDLocale.L("addons.schedule.menu.finished", " Finished!"));
                time.setEnabled(true);
            }
        } else if (e.getSource() == c) {
            String remainString = JDUtilities.formatSeconds(var / 1000);
            String remain = JDLocale.L("addons.schedule.menu.remain", "Remaining:") + " " + remainString;
            status.setText(remain);
        }

    }

    // Berechnen der TimerZeit
    private int parseTime() {
        long startTime = Calendar.getInstance().getTime().getTime();
        long endTime = date_model.getDate().getTime();
        return (int) (endTime - startTime);
    }

    public JLabel getLabel() {
        return label;
    }

    public JLabel getStatusLabel() {
        return status;
    }

}
