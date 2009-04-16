package jd.gui.skins.simple;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class JDStatusBar extends JPanel implements ChangeListener, ControlListener {
    private static final long serialVersionUID = 3676496738341246846L;

    private JLabel lblSimu;

    private JLabel lblSpeed;

    protected JSpinner spMax;

    protected JSpinner spMaxDls;

    private JSpinner spMaxChunks;

    private JLabel maxChunks;

    public JDStatusBar() {
        setLayout(new MigLayout("ins 0 0 0 0,", "[fill,grow,left][shrink,right][shrink,right][shrink,right][shrink,right][shrink,right]", "[21px!]"));

        // lblMessage.setIcon(statusIcon);
        // statusBarHandler = new LabelHandler(lblMessage,
        // JDLocale.L("sys.message.welcome", "Welcome to JDownloader"));

        JDUtilities.getController().addControlListener(this);
        lblSpeed = new JLabel(JDLocale.L("gui.statusbar.speed", "Max. Speed"));
        lblSimu = new JLabel(JDLocale.L("gui.statusbar.sim_ownloads", "Max.Dls."));
        maxChunks = new JLabel(JDLocale.L("gui.statusbar.maxChunks", "Max.Con."));
        spMax = new JSpinner();
        spMax.setModel(new SpinnerNumberModel(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0), 0, Integer.MAX_VALUE, 50));
        spMax.setPreferredSize(new Dimension(60, 20));
        spMax.setToolTipText(JDLocale.L("gui.tooltip.statusbar.speedlimiter", "Geschwindigkeitsbegrenzung festlegen(kb/s) [0:unendlich]"));
        spMax.addChangeListener(this);
        colorizeSpinnerSpeed();

        spMaxDls = new JSpinner();
        spMaxDls.setModel(new SpinnerNumberModel(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2), 1, 20, 1));
        spMaxDls.setPreferredSize(new Dimension(60, 20));
        spMaxDls.setToolTipText(JDLocale.L("gui.tooltip.statusbar.simultan_downloads", "Max. gleichzeitige Downloads"));
        spMaxDls.addChangeListener(this);

        spMaxChunks = new JSpinner();
        spMaxChunks.setModel(new SpinnerNumberModel(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2), 1, 20, 1));
        spMaxChunks.setPreferredSize(new Dimension(60, 20));
        spMaxChunks.setToolTipText(JDLocale.L("gui.tooltip.statusbar.max_chunks", "Max. Connections/File"));
        spMaxChunks.addChangeListener(this);

        add(new PremiumStatus(), "gaptop 1");
        add(maxChunks);
        add(spMaxChunks, "width 90!,height 20!");
        add(lblSimu);
        add(spMaxDls, "width 90!,height 20!");
        add(lblSpeed);
        add(spMax, "width 90!,height 20!");

    }

    void addItem(boolean seperator, JComponent where, Component component) {
        int n = 10;
        Dimension d = new Dimension(n, 0);
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(n, n));
        where.add(new Box.Filler(d, d, d));
        if (seperator) where.add(separator);
        where.add(component);
    }

    private void colorizeSpinnerSpeed() {
        /* fÃ¤rbt den spinner ein, falls speedbegrenzung aktiv */
        JSpinner.DefaultEditor spMaxEditor = (JSpinner.DefaultEditor) spMax.getEditor();
        if ((Integer) spMax.getValue() > 0) {
            lblSpeed.setForeground(JDTheme.C("gui.color.statusbar.maxspeedhighlight", "ff0c03"));
            spMaxEditor.getTextField().setForeground(Color.red);
        } else {
            lblSpeed.setForeground(Color.black);
            spMaxEditor.getTextField().setForeground(Color.black);
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
            Property p = (Property) event.getSource();
            if (spMax != null && event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SPEED)) {
                setSpinnerSpeed(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
            } else if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN)) {
                spMaxDls.setValue(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2));
            } else if (event.getID() == ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED) {
                // btnStartStop.setIcon(new
                // ImageIcon(JDImage.getImage(getStartStopDownloadImage())));
                // btnPause.setIcon(new
                // ImageIcon(JDUtilities.getImage(getPauseImage())));
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
            lblSpeed.setText(JDLocale.L("gui.statusbar.speed", "Max. Speed"));
        } else {
            lblSpeed.setText("(" + JDUtilities.formatKbReadable(speed / 1024) + "/s)");
        }
    }

    public void setSpinnerSpeed(Integer speed) {
        spMax.setValue(speed);
        colorizeSpinnerSpeed();
    }

    public void stateChanged(ChangeEvent e) {

        if (e.getSource() == spMax) {
            colorizeSpinnerSpeed();
            SubConfiguration subConfig = SubConfiguration.getConfig("DOWNLOAD");
            subConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, (Integer) spMax.getValue());
            subConfig.save();

        } else if (e.getSource() == spMaxDls) {
            SubConfiguration subConfig = SubConfiguration.getConfig("DOWNLOAD");
            subConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, (Integer) spMaxDls.getValue());
            subConfig.save();

        }
    }
}
