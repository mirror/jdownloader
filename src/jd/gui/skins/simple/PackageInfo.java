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

package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.SubConfiguration;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Dies Klasse ziegt informationen zu einem FilePackage an
 */
public class PackageInfo extends JDialog implements ChangeListener {

    private SubConfiguration subConfig = null;
    private static final String PROPERTY_REFRESHRATE = "PROPERTY_PACKAGE_REFRESHRATE";

    private static final long serialVersionUID = -9146764850581039090L;
    private FilePackage fp;
    private int dlLinksSize = 0;
    private int i = 0;
    private JPanel panel;
    private JSlider slider;
    private JLabel lblSlider;
    private DecimalFormat c = new DecimalFormat("0.00");
    private HashMap<String, JComponent> hmObjects = new HashMap<String, JComponent>();
    private boolean firstRun = true;

    /**
     * @param frame
     * @param dlink
     */
    public PackageInfo(JFrame frame, FilePackage fp) {
        super();
        subConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        this.fp = fp;
        setLayout(new BorderLayout(2, 2));
        setTitle(JDLocale.L("gui.packageinfo.title", "Package Information:") + " " + fp.getName());
        setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.package_opened")));
        setResizable(true);
        setAlwaysOnTop(true);

        panel = new JPanel(new GridBagLayout());
        int n = 10;
        panel.setBorder(new EmptyBorder(n, n, n, n));
        panel.setBackground(Color.WHITE);
        panel.setForeground(Color.WHITE);
        this.add(new JScrollPane(panel));

        slider = new JSlider();
        slider.setMaximum(5000);
        slider.setMinimum(500);
        slider.setValue(subConfig.getIntegerProperty(PROPERTY_REFRESHRATE, 1000));
        slider.setOpaque(false);
        slider.setBorder(BorderFactory.createEmptyBorder());
        slider.addChangeListener(this);
        lblSlider = new JLabel(JDLocale.L("gui.packageinfo.rate", "Refreshrate") + " [" + slider.getValue() + "ms]");
        JPanel topPanel = new JPanel(new BorderLayout(2, 2));
        topPanel.setBorder(new EmptyBorder(0, n, 0, n));
        topPanel.add(lblSlider, BorderLayout.LINE_START);
        topPanel.add(slider);
        this.add(topPanel, BorderLayout.PAGE_START);

        new Thread() {
            @Override
            public void run() {
                do {
                    try {
                        Thread.sleep(slider.getValue());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    initDialog();
                    validate();
                } while (isVisible());
            }
        }.start();
        initDialog();
        pack();
        setLocation((int) (frame.getLocation().getX() + frame.getWidth() / 2 - getWidth() / 2), (int) (frame.getLocation().getY() + frame.getHeight() / 2 - getHeight() / 2));
        setVisible(true);
        frame.setMaximumSize(getToolkit().getScreenSize());
    }

    private void addEntry(String string, JComponent value) {
        if (hmObjects.get(string) == null) {
            JLabel key;
            JDUtilities.addToGridBag(panel, key = new JLabel(string), 0, i, 1, 1, 0, 1, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);

            JDUtilities.addToGridBag(panel, value, 1, i, 1, 1, 1, 0, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);

            key.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            value.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            i++;
            hmObjects.put(string, value);
            hmObjects.put("JLabel_" + string, key);
        } else {
            if (hmObjects.get(string).getClass() == JProgressBar.class) {
                JProgressBar tmpJBar = (JProgressBar) hmObjects.get(string);
                tmpJBar.setValue(((JProgressBar) value).getValue());
                tmpJBar.setString(((JProgressBar) value).getString());
                tmpJBar.setEnabled(((JProgressBar) value).isEnabled());
                tmpJBar.setBackground((((JProgressBar) value).getBackground()));
                JLabel tmpJLbl = (JLabel) hmObjects.get("JLabel_" + string);
                if (tmpJBar.getBackground().getRed() != 150 && tmpJBar.getBackground().getGreen() != 150 && tmpJBar.getBackground().getBlue() != 150) {
                    int col = new Float(200f / tmpJBar.getMaximum() * tmpJBar.getValue()).intValue();
                    if (col <= 50) {
                        tmpJLbl.setForeground(new Color(col, col, col));
                    } else {
                        tmpJLbl.setForeground(new Color(50, col, 50));
                    }
                    tmpJBar.setForeground(tmpJLbl.getForeground());
                } else {
                    tmpJLbl.setForeground(tmpJBar.getBackground());
                }
            } else if (hmObjects.get(string).getClass() == JTextField.class) {
                JTextField tmp = (JTextField) hmObjects.get(string);
                tmp.setText(((JTextField) value).getText());
                tmp.setHorizontalAlignment(JTextField.RIGHT);
            }
            hmObjects.get(string).repaint();
        }

    }

    private void addEntry(String label, String data) {
        if (hmObjects.get("JLableCaption_" + label) == null) {
            if (label == null && data == null) {
                JDUtilities.addToGridBag(panel, new JSeparator(), 0, i, 2, 1, 0, 0, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
                return;
            }

            JLabel key;
            JDUtilities.addToGridBag(panel, key = new JLabel(label), 0, i, 1, 1, 0, 1, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);
            key.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

            JLabel value;
            JDUtilities.addToGridBag(panel, value = new JLabel(data), 1, i, 1, 1, 1, 0, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);
            value.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            value.setHorizontalAlignment(SwingConstants.RIGHT);
            value.setForeground(Color.DARK_GRAY);
            hmObjects.put("JLableCaption_" + label, key);
            hmObjects.put("JLableValue_" + label, value);

            i++;
        } else {
            if (label != null && data != null) {
                JLabel key = (JLabel) hmObjects.get("JLableCaption_" + label);
                JLabel value = (JLabel) hmObjects.get("JLableValue_" + label);
                key.setText(label);
                value.setText(data);
            }
        }
    }

    private void initDialog() {
        if (firstRun == true) {
            panel.setVisible(false);
        }

        if (dlLinksSize != fp.getDownloadLinks().size()) {
            hmObjects.clear();
            dlLinksSize = fp.getDownloadLinks().size();
            panel.removeAll();
        }

        addEntry(JDLocale.L("gui.packageinfo.name", "Name"), fp.getName());
        if (fp.hasPassword()) {
            JTextField pw = new JTextField(fp.getPassword());
            pw.setEditable(false);
            addEntry(JDLocale.L("gui.packageinfo.password", "Password"), pw);
        }
        if (fp.hasComment()) {
            JTextField comment = new JTextField(fp.getComment());
            comment.setEditable(false);
            addEntry(JDLocale.L("gui.packageinfo.comment", "Comment"), comment);
        }
        addEntry(JDLocale.L("gui.packageinfo.dldirectory", "Downloaddirectory"), fp.getDownloadDirectory());
        addEntry(JDLocale.L("gui.packageinfo.packagesize", "Packagesize"), JDUtilities.formatKbReadable(fp.getTotalEstimatedPackageSize()) + " [" + fp.getTotalEstimatedPackageSize() + " KB]");
        addEntry(JDLocale.L("gui.packageinfo.loaded", "Loaded"), JDUtilities.formatKbReadable(fp.getTotalKBLoaded()) + " [" + fp.getTotalKBLoaded() + " KB]");
        addEntry(JDLocale.L("gui.packageinfo.links", "Links"), "");
        addEntry(JDLocale.L("gui.packageinfo.extractafter", "Autoextract"), fp.isExtractAfterDownload() ? "[x]" : "[ ]");
        int i = 0;
        JProgressBar p;
        for (DownloadLink link : fp.getDownloadLinks()) {
            p = new JProgressBar(0, 100);

            p.setMaximum(10000);
            p.setValue(link.getPercent());
            p.setStringPainted(true);
            p.setString(c.format(link.getPercent() / 100.0) + "% (" + JDUtilities.formatBytesToMB(link.getDownloadCurrent()) + "/" + JDUtilities.formatBytesToMB(Math.max(1, link.getDownloadSize())) + ") @ " + JDUtilities.formatKbReadable(link.getDownloadSpeed() / 1024) + "/s");
            p.setEnabled(link.isEnabled());
            if (link.isEnabled() == false) {
                p.setBackground(new Color(150, 150, 150));
            } else {
                p.setBackground(new Color(0, 0, 0));
            }
            addEntry(++i + ". " + link.getName(), p);
        }

        /*
         * Gewisse Formatierungsgeschichten werden erst beim zweiten Durchlauf
         * gesetzt. Um nicht eine Timerperiode abwarten zu müssen wird nach dem
         * ersten (unsichtbarem) Durchlauf direkt ein zweiter gestartet.
         */
        if (firstRun == true) {
            firstRun = false;
            initDialog();
            panel.setVisible(true);
        }
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == slider) {
            lblSlider.setText(JDLocale.L("gui.packageinfo.rate", "Refreshrate") + " [" + slider.getValue() + "ms]");
            subConfig.setProperty(PROPERTY_REFRESHRATE, slider.getValue());
            subConfig.save();
        }
    }
}
