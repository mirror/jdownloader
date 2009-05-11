package jd.gui.skins.simple;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.nutils.Formatter;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXStatusBar;

public class JDStatusBar extends JPanel implements ChangeListener, ControlListener {

    private static final long serialVersionUID = 3676496738341246846L;

    private SubConfiguration dlConfig = null;

    private JLabel lblMaxChunks;

    private JSpinner spMaxChunks;

    private JLabel lblMaxDls;

    private JSpinner spMaxDls;

    private JLabel lblMaxSpeed;

    private JSpinner spMaxSpeed;

    public JDStatusBar() {
        dlConfig = SubConfiguration.getConfig("DOWNLOAD");

        initGUI();
    }

    private void initGUI() {
        setLayout(new MigLayout("ins 0 0 0 0,debug", "[fill,grow,left][shrink,right][shrink,right][shrink,right][shrink,right][shrink,right]", "[23px!]"));

        JDUtilities.getController().addControlListener(this);

        lblMaxSpeed = new JLabel(JDLocale.L("gui.statusbar.speed", "Max. Speed"));
        spMaxSpeed = new JSpinner();
        spMaxSpeed.setModel(new SpinnerNumberModel(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0), 0, Integer.MAX_VALUE, 50));
        spMaxSpeed.setToolTipText(JDLocale.L("gui.tooltip.statusbar.speedlimiter", "Geschwindigkeitsbegrenzung festlegen (KB/s) [0:unendlich]"));
        spMaxSpeed.addChangeListener(this);
        colorizeSpinnerSpeed();

        lblMaxDls = new JLabel(JDLocale.L("gui.statusbar.sim_ownloads", "Max. Dls."));
        spMaxDls = new JSpinner();
        spMaxDls.setModel(new SpinnerNumberModel(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2), 1, 20, 1));
        spMaxDls.setToolTipText(JDLocale.L("gui.tooltip.statusbar.simultan_downloads", "Max. gleichzeitige Downloads"));
        spMaxDls.addChangeListener(this);

        lblMaxChunks = new JLabel(JDLocale.L("gui.statusbar.maxChunks", "Max. Con."));
        spMaxChunks = new JSpinner();
        spMaxChunks.setModel(new SpinnerNumberModel(dlConfig.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2), 1, 20, 1));
        spMaxChunks.setToolTipText(JDLocale.L("gui.tooltip.statusbar.max_chunks", "Max. Connections/File"));
        spMaxChunks.addChangeListener(this);

        add(new PremiumStatus(), "gaptop 1");
        JPanel p = new JPanel(new MigLayout("ins 0"));
        p.setOpaque(false);
        p.add(lblMaxChunks);
        p.add(spMaxChunks, "width 70!");
        add(p);
        p = new JPanel(new MigLayout("ins 0"));
        p.add(lblMaxDls);
        p.add(spMaxDls, "width 70!");
        p.setOpaque(false);
        add(p);

        p = new JPanel(new MigLayout("ins 0"));
        p.add(lblMaxSpeed);
        p.add(spMaxSpeed, "width 70!");
        p.setOpaque(false);
        add(p);

    }

    private void colorizeSpinnerSpeed() {
        /* färbt den spinner ein, falls speedbegrenzung aktiv */
        JSpinner.DefaultEditor spMaxEditor = (JSpinner.DefaultEditor) spMaxSpeed.getEditor();
        if ((Integer) spMaxSpeed.getValue() > 0) {
            lblMaxSpeed.setForeground(JDTheme.C("gui.color.statusbar.maxspeedhighlight", "ff0c03"));
            spMaxEditor.getTextField().setForeground(JDTheme.C("gui.color.statusbar.maxspeedhighlight", "ff0c03"));
        } else {
            lblMaxSpeed.setForeground(null);
            spMaxEditor.getTextField().setForeground(null);
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
            lblMaxSpeed.setText(JDLocale.L("gui.statusbar.speed", "Max. Speed"));
        } else {
            lblMaxSpeed.setText("(" + Formatter.formatReadable(speed) + "/s)");
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
