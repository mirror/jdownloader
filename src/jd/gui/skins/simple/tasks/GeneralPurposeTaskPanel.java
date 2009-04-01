package jd.gui.skins.simple.tasks;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jvnet.substance.api.ComponentState;
import org.jvnet.substance.api.SubstanceColorScheme;
import org.jvnet.substance.utils.SubstanceColorSchemeUtilities;

public class GeneralPurposeTaskPanel extends TaskPanel implements ActionListener, ChangeListener, ControlListener {

    private static final long serialVersionUID = 3880548380852933192L;
    private JCheckBox reconnect;
    private JCheckBox clipboard;
    private JCheckBox premium;
    private JSpinner speedlimit;
    private JSpinner simultanDownloads;
    private JSpinner chunks;
    private JButton manReconnect;
    private Color[] speedColors;
    private static final String GAP_LEFT = "gapleft 5";
    private static final String cfgNS = "gui.taskpanes.generalpurpose.";

    public GeneralPurposeTaskPanel(String l, ImageIcon ii) {
        super(l, ii, "generalPurposeTaskPanel");

        this.initGui();
        JDUtilities.getController().addControlListener(this);
        this.setCollapsed(false);
    }

    private Color[] getAlertColors() {
        if (JDUtilities.getJavaVersion() >= 1.6 && SimpleGUI.isSubstance()) {
            SubstanceColorScheme colorScheme = SubstanceColorSchemeUtilities.getColorScheme(speedlimit, ComponentState.SELECTED);
            colorScheme = colorScheme.shift(Color.RED, 0.95, Color.RED, 0.95);
            return new Color[] { colorScheme.getFocusRingColor(), ((JSpinner.DefaultEditor) speedlimit.getEditor()).getTextField().getForeground() };
        } else {
            return new Color[] { Color.RED, Color.WHITE };
        }
    }

    private void initGui() {
        this.setLayout(new MigLayout("ins 0,wrap 2", "[fill,grow]5[70!,fill]", "[]0[]0[]0[]"));
        addButtonReconnect();

        add(new JSeparator(), "spanx,gaptop 3");
        addCheckBoxReconnect();
        addCheckBoxClipBoard();
        addCheckBoxPremium();

        add(new JSeparator(), "spanx");

        addSpinnerSpeed();
        addSpinnerDownloads();
        addSpinnerChunks();
    }

    @Override
    protected JButton addButton(JButton bt) {
        bt.addActionListener(this);
        bt.setHorizontalAlignment(JButton.LEFT);
        add(bt, "spanx,alignx leading,gaptop 2");
        return bt;
    }

    private void addButtonReconnect() {
        this.manReconnect = addButton(this.createButton(JDLocale.L(cfgNS + "reconnectnoew", "Reconnect Now!"), JDTheme.II("gui.images.config.reconnect", 16, 16)));
    }

    private void addSpinnerChunks() {
        JLabel lbl;
        add(lbl = new JLabel(JDLocale.L(cfgNS + "chunks", "# chunks")), GAP_LEFT);
        int value = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 4);

        add(chunks = new JSpinner(new SpinnerNumberModel(value, 1, 30, 1)), "alignx right");

        lbl.setToolTipText(JDLocale.L(cfgNS + "tooltip.chunks", "Chunkload boosts downloadspeed. Recommended: 5 Chunks"));
        chunks.setToolTipText(JDLocale.L(cfgNS + "tooltip.chunks", "Chunkload boosts downloadspeed. Recommended: 5 Chunks"));
        chunks.addChangeListener(this);
    }

    private void addSpinnerDownloads() {
        JLabel lbl;
        add(lbl = new JLabel(JDLocale.L(cfgNS + "simultandownloads", "# downloads")), GAP_LEFT);
        int value = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2);
        add(simultanDownloads = new JSpinner(new SpinnerNumberModel(value, 1, 30, 1)), "alignx right");
        simultanDownloads.setToolTipText(JDLocale.L(cfgNS + "tooltip.simdownloads", "Tell JDownloader how many downloads to start at once"));
        lbl.setToolTipText(JDLocale.L(cfgNS + "tooltip.simdownloads", "Tell JDownloader how many downloads to start at once"));
        simultanDownloads.addChangeListener(this);
    }

    private void addSpinnerSpeed() {
        JLabel lbl;
        add(lbl = new JLabel(JDLocale.L(cfgNS + "speed", "Speedlimit")), GAP_LEFT);
        int value = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);
        add(speedlimit = new JSpinner(new SpinnerNumberModel(0, 0, 0, 0)), "alignx right");
        ((SpinnerNumberModel) speedlimit.getModel()).setValue(value);
        ((SpinnerNumberModel) speedlimit.getModel()).setMaximum(Integer.MAX_VALUE);
        ((SpinnerNumberModel) speedlimit.getModel()).setMinimum(0);
        ((SpinnerNumberModel) speedlimit.getModel()).setStepSize(50);
        speedlimit.setToolTipText(JDLocale.L(cfgNS + "tooltip.speedlimit", "Limit the bandwith JDownloader uses to download"));
        lbl.setToolTipText(JDLocale.L(cfgNS + "tooltip.speedlimit", "Limit the bandwith JDownloader uses to download"));
        speedlimit.addChangeListener(this);
        this.speedColors = getAlertColors();
        updateSpinnerSpeedColor();
        // JLabel lbl;
        // add(lbl = new JLabel(JDLocale.L(cfgNS + "speed", "Speedlimit")),
        // GAP_LEFT);
        // add(speedlimit = new JSpinner(new
        // SpinnerNumberModel(JDUtilities.getSubConfig
        // ("DOWNLOAD").getIntegerProperty
        // (Configuration.PARAM_DOWNLOAD_MAX_SPEED), 0, Integer.MAX_VALUE, 50)),
        // "alignx right");
        // lbl.setToolTipText(JDLocale.L(cfgNS + "tooltip.speedlimit",
        // "Limit the bandwith JDownloader uses to download"));
        // speedlimit.setToolTipText(JDLocale.L(cfgNS + "tooltip.speedlimit",
        // "Limit the bandwith JDownloader uses to download"));
        // speedlimit.addChangeListener(this);
    }

    private void updateSpinnerSpeedColor() {
        JSpinner.DefaultEditor spMaxEditor = (JSpinner.DefaultEditor) speedlimit.getEditor();
        System.out.println("  " + speedlimit.getValue());
        if ((Integer) speedlimit.getValue() > 0) {
            spMaxEditor.getTextField().setForeground(speedColors[0]);
        } else {
            spMaxEditor.getTextField().setForeground(speedColors[1]);
        }
    }

    private void addCheckBoxPremium() {
        add(this.premium = new JCheckBox(JDLocale.L(cfgNS + "premium", "Enable Premium")), "spanx,alignx leading");
        premium.setToolTipText(JDLocale.L(cfgNS + "tooltip.premium", "Enable Premiumusage globaly"));
        premium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
        premium.addChangeListener(this);
        premium.setContentAreaFilled(false);
        premium.setFocusPainted(false);
    }

    private void addCheckBoxClipBoard() {
        add(this.clipboard = new JCheckBox(JDLocale.L(cfgNS + "clipboard", "Observe Clipboard")), "spanx,alignx leading");
        clipboard.setToolTipText(JDLocale.L(cfgNS + "tooltip.clipboard", "Enable the clipboard observer to detect links you copied"));
        clipboard.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, false));
        clipboard.addChangeListener(this);
        clipboard.setContentAreaFilled(false);
        clipboard.setFocusPainted(false);
    }

    private void addCheckBoxReconnect() {
        add(this.reconnect = new JCheckBox(JDLocale.L(cfgNS + "reconnect", "Auto. Reconnect")), "spanx,alignx leading");
        reconnect.setToolTipText(JDLocale.L(cfgNS + "tooltip.reconnect", "Enable automated Reconnect to avoid waitingtimes"));
        reconnect.setSelected(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));
        reconnect.addChangeListener(this);
        reconnect.setContentAreaFilled(false);
        reconnect.setFocusPainted(false);
    }

    public void actionPerformed(ActionEvent e) {

    }

    public void stateChanged(ChangeEvent e) {

        if (e.getSource() == this.speedlimit) {
            updateSpinnerSpeedColor();
            SubConfiguration subConfig = JDUtilities.getSubConfig("DOWNLOAD");
            subConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, (Integer) speedlimit.getValue());
            subConfig.save();

        } else if (e.getSource() == this.simultanDownloads) {
            SubConfiguration subConfig = JDUtilities.getSubConfig("DOWNLOAD");
            subConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, (Integer) simultanDownloads.getValue());
            subConfig.save();

        } else if (e.getSource() == this.premium) {
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) != premium.isSelected()) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, premium.isSelected());
                JDUtilities.getConfiguration().save();
            }
        }
    }

    public void setSpinnerSpeed(Integer speed) {
        speedlimit.setValue(speed);
        updateSpinnerSpeedColor();
        speedlimit.invalidate();
    }

    public void controlEvent(final ControlEvent event) {

        GuiRunnable run = new GuiRunnable() {
            private static final long serialVersionUID = 8726498576488124702L;

            public void run() {

                guiControl(event);

            }
        };
        EventQueue.invokeLater(run);
    }

    private void guiControl(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
            Property p = (Property) event.getSource();
            if (this.speedlimit != null && event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SPEED)) {
                setSpinnerSpeed(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
            } else if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN)) {
                this.simultanDownloads.setValue(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2));
            } else if (p == JDUtilities.getConfiguration() && event.getParameter().equals(Configuration.PARAM_USE_GLOBAL_PREMIUM)) {
                this.premium.setSelected(p.getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
            } else if (event.getID() == ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED) {
                // btnStartStop.setIcon(new
                // ImageIcon(JDImage.getImage(getStartStopDownloadImage
                // ())));
                // btnPause.setIcon(new
                // ImageIcon(JDUtilities.getImage(getPauseImage())));
            }
        }

    }

}
