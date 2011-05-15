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

package jd.gui.swing.jdgui.components;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JSonWrapper;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.components.JDSpinner;
import jd.gui.swing.jdgui.components.modules.ModuleStatus;
import jd.gui.swing.jdgui.components.premiumbar.PremiumStatus;
import jd.gui.swing.laf.LookAndFeelController;
import jd.nutils.Formatter;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdownloader.gui.translate._GUI;

public class StatusBar extends JPanel implements ChangeListener, ControlListener {

    private static final long serialVersionUID = 3676496738341246846L;

    private final JSonWrapper dlConfig;

    private JDSpinner         spMaxChunks;

    private JDSpinner         spMaxDls;

    private JDSpinner         spMaxSpeed;

    public StatusBar() {
        dlConfig = JSonWrapper.get("DOWNLOAD");

        initGUI();
    }

    private void initGUI() {
        setLayout(new MigLayout("ins 0", "[fill,grow,left][fill,grow,right][][shrink,right][shrink,right][shrink,right]", "[22!]"));
        if (LookAndFeelController.getInstance().getLAFOptions().isPaintStatusbarTopBorder()) {
            setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().darker()));

        } else {
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, getBackground().darker()));

        }

        JDUtilities.getController().addControlListener(this);
        spMaxSpeed = new JDSpinner(_GUI._.gui_statusbar_speed());
        spMaxSpeed.getSpinner().addChangeListener(this);
        spMaxSpeed.getSpinner().setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 50));
        try {
            spMaxSpeed.setValue(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
        } catch (Throwable e) {
            spMaxSpeed.setValue(0);
            dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);
            dlConfig.save();
        }
        spMaxSpeed.setToolTipText(_GUI._.gui_tooltip_statusbar_speedlimiter());
        colorizeSpinnerSpeed();

        spMaxDls = new JDSpinner(_GUI._.gui_statusbar_sim_ownloads(), "h 20!");
        spMaxDls.getSpinner().setModel(new SpinnerNumberModel(2, 1, 20, 1));
        try {
            spMaxDls.setValue(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2));
        } catch (Throwable e) {
            spMaxDls.setValue(2);
            dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2);
            dlConfig.save();
        }
        spMaxDls.setToolTipText(_GUI._.gui_tooltip_statusbar_simultan_downloads());
        spMaxDls.getSpinner().addChangeListener(this);

        spMaxChunks = new JDSpinner(_GUI._.gui_statusbar_maxChunks(), "h 20!");
        spMaxChunks.getSpinner().setModel(new SpinnerNumberModel(2, 1, 20, 1));
        try {
            spMaxChunks.setValue(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        } catch (Throwable e) {
            dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2);
            dlConfig.save();
        }
        spMaxChunks.setToolTipText(_GUI._.gui_tooltip_statusbar_max_chunks());
        spMaxChunks.getSpinner().addChangeListener(this);

        add(PremiumStatus.getInstance());
        add(new ModuleStatus());
        add(new JSeparator(JSeparator.VERTICAL), "growy");
        add(spMaxChunks);
        add(spMaxDls);
        add(spMaxSpeed);

    }

    private void colorizeSpinnerSpeed() {
        /* färbt den spinner ein, falls speedbegrenzung aktiv */
        if (spMaxSpeed.getValue() > 0) {
            spMaxSpeed.setColor(new Color(255, 12, 3));
        } else {
            spMaxSpeed.setColor(null);
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
            final Property p = (Property) event.getCaller();
            if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SPEED)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        DownloadWatchDog.getInstance().getConnectionManager().setIncommingBandwidthLimit(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) * 1024);
                        setSpinnerSpeed(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
                    }
                });
            } else if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            spMaxDls.setValue(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2));
                        } catch (Throwable e) {
                            spMaxDls.setValue(2);
                        }
                    }
                });
            } else if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            spMaxChunks.setValue(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 1));
                        } catch (Throwable e) {
                            spMaxChunks.setValue(1);
                        }
                    }
                });
            }
        }
    }

    /**
     * Setzt die Downloadgeschwindigkeit
     * 
     * @param speed
     *            bytes pro sekunde
     */
    public void setSpeed(int speed) {
        if (speed <= 0) {
            spMaxSpeed.setText(_GUI._.gui_statusbar_speed());
        } else {
            spMaxSpeed.setText("(" + Formatter.formatReadable(speed) + "/s)");
        }
    }

    public void setSpinnerSpeed(Integer speed) {
        try {
            spMaxSpeed.setValue(speed);
            colorizeSpinnerSpeed();
        } catch (Throwable e) {
        }
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == spMaxSpeed.getSpinner()) {
            dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, spMaxSpeed.getValue());
            dlConfig.save();
        } else if (e.getSource() == spMaxDls.getSpinner()) {
            dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, spMaxDls.getValue());
            dlConfig.save();
        } else if (e.getSource() == spMaxChunks.getSpinner()) {
            dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, spMaxChunks.getValue());
            dlConfig.save();
        }
    }

}