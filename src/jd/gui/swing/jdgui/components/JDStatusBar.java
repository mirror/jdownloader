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

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.components.JDSpinner;
import jd.gui.swing.jdgui.components.modules.ModuleStatus;
import jd.gui.swing.jdgui.components.premiumbar.PremiumStatus;
import jd.nutils.Formatter;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class JDStatusBar extends JPanel implements ChangeListener, ControlListener {

    private static final long serialVersionUID = 3676496738341246846L;

    private final SubConfiguration dlConfig;

    private JDSpinner spMaxChunks;

    private JDSpinner spMaxDls;

    private JDSpinner spMaxSpeed;

    public JDStatusBar() {
        dlConfig = SubConfiguration.getConfig("DOWNLOAD");

        initGUI();
    }

    private void initGUI() {
        setLayout(new MigLayout("ins 0", "[fill,grow,left][fill,grow,right][][shrink,right][shrink,right][shrink,right]", "[22!]"));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().darker()));

        JDUtilities.getController().addControlListener(this);
        spMaxSpeed = new JDSpinner(JDL.L("gui.statusbar.speed", "Max. Speed"));
        spMaxSpeed.getSpinner().addChangeListener(this);
        spMaxSpeed.getSpinner().setModel(new SpinnerNumberModel(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0), 0, Integer.MAX_VALUE, 50));
        spMaxSpeed.setToolTipText(JDL.L("gui.tooltip.statusbar.speedlimiter", "Speed Limit (KiB/s) [0 = Infinite]"));
        colorizeSpinnerSpeed();

        spMaxDls = new JDSpinner(JDL.L("gui.statusbar.sim_ownloads", "Max. Dls."), "h 20!");
        spMaxDls.getSpinner().setModel(new SpinnerNumberModel(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2), 1, 20, 1));
        spMaxDls.setToolTipText(JDL.L("gui.tooltip.statusbar.simultan_downloads", "Maximum simultaneous Downloads [1..20]"));
        spMaxDls.getSpinner().addChangeListener(this);

        spMaxChunks = new JDSpinner(JDL.L("gui.statusbar.maxChunks", "Max. Con."), "h 20!");
        spMaxChunks.getSpinner().setModel(new SpinnerNumberModel(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2), 1, 20, 1));
        spMaxChunks.setToolTipText(JDL.L("gui.tooltip.statusbar.max_chunks", "Max. Connections/File"));
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
            spMaxSpeed.setColor(JDTheme.C("gui.color.statusbar.maxspeedhighlight", "ff0c03"));
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
                        spMaxDls.setValue(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2));
                    }
                });
            } else if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        spMaxChunks.setValue(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 1));
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
            spMaxSpeed.setText(JDL.L("gui.statusbar.speed", "Max. Speed"));
        } else {
            spMaxSpeed.setText("(" + Formatter.formatReadable(speed) + "/s)");
        }
    }

    public void setSpinnerSpeed(Integer speed) {
        spMaxSpeed.setValue(speed);
        colorizeSpinnerSpeed();
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
