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
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.components.premiumbar.PremiumStatus;
import jd.nutils.Formatter;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class JDStatusBar extends JPanel implements ChangeListener, ControlListener {

    private static final long serialVersionUID = 3676496738341246846L;

    private SubConfiguration dlConfig = null;

    private JDSpinner spMaxChunks;

    private JDSpinner spMaxDls;

    private JDSpinner spMaxSpeed;

    public JDStatusBar() {
        dlConfig = SubConfiguration.getConfig("DOWNLOAD");

        initGUI();
    }

    private class JDSpinner extends JPanel {
        private static final long serialVersionUID = 8892482065686899916L;

        private JLabel lbl;

        private JSpinner spn;

        public JDSpinner(String label) {
            super(new MigLayout("ins 0", "[][grow,fill]"));

            lbl = new JLabel(label) {
                private static final long serialVersionUID = 8794670984465489135L;

                @Override
                public Point getToolTipLocation(MouseEvent e) {
                    return new Point(0, -25);
                }
            };
            spn = new JSpinner();
            spn.addChangeListener(JDStatusBar.this);

            add(lbl);
            add(spn, "w 70!, h 20!");
        }

        public JSpinner getSpinner() {
            return spn;
        }

        public void setText(String s) {
            lbl.setText(s);
        }

        public void setValue(Integer i) {
            spn.setValue(i);
        }

        public Integer getValue() {
            return (Integer) spn.getValue();
        }

        public void setColor(Color c) {
            lbl.setForeground(c);
            ((DefaultEditor) spn.getEditor()).getTextField().setForeground(c);
        }

        @Override
        public void setToolTipText(String s) {
            lbl.setToolTipText(s);
        }

    }

    private void initGUI() {
        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().darker()));

        setLayout(new MigLayout("ins 0 0 0 0", "[fill,grow,left][shrink,right][shrink,right][shrink,right][shrink,right][shrink,right]", "[23px!]"));

        JDUtilities.getController().addControlListener(this);

        spMaxSpeed = new JDSpinner(JDL.L("gui.statusbar.speed", "Max. Speed"));
        spMaxSpeed.getSpinner().setModel(new SpinnerNumberModel(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0), 0, Integer.MAX_VALUE, 50));
        spMaxSpeed.setToolTipText(JDL.L("gui.tooltip.statusbar.speedlimiter", "Geschwindigkeitsbegrenzung festlegen (KB/s) [0:unendlich]"));
        colorizeSpinnerSpeed();

        spMaxDls = new JDSpinner(JDL.L("gui.statusbar.sim_ownloads", "Max. Dls."));
        spMaxDls.getSpinner().setModel(new SpinnerNumberModel(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2), 1, 20, 1));
        spMaxDls.setToolTipText(JDL.L("gui.tooltip.statusbar.simultan_downloads", "Max. gleichzeitige Downloads"));

        spMaxChunks = new JDSpinner(JDL.L("gui.statusbar.maxChunks", "Max. Con."));
        spMaxChunks.getSpinner().setModel(new SpinnerNumberModel(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2), 1, 20, 1));
        spMaxChunks.setToolTipText(JDL.L("gui.tooltip.statusbar.max_chunks", "Max. Connections/File"));

        add(new PremiumStatus(), "gaptop 1,aligny top");
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
        if (event.getID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
            final Property p = (Property) event.getSource();
            if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SPEED)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
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
        if (e.getSource() == spMaxSpeed) {
            dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, (Integer) spMaxSpeed.getValue());
            dlConfig.save();
        } else if (e.getSource() == spMaxDls) {
            dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, (Integer) spMaxDls.getValue());
            dlConfig.save();
        } else if (e.getSource() == spMaxChunks) {
            dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, (Integer) spMaxChunks.getValue());
            dlConfig.save();
        }
    }

}
