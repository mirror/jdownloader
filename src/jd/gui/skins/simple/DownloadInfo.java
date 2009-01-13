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
import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
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
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterface.Chunk;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Dies Klasse ziegt informationen zu einem DownloadLink an
 */
public class DownloadInfo extends JFrame implements ChangeListener {

    private SubConfiguration subConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
    private static final String PROPERTY_REFRESHRATE = "PROPERTY_LINK_REFRESHRATE";

    private static final long serialVersionUID = -9146764850581039090L;
    private DownloadLink downloadLink;
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
    public DownloadInfo(JFrame frame, DownloadLink dLink) {
        super();
        downloadLink = dLink;
        setLayout(new BorderLayout(2, 2));
        setTitle(JDLocale.L("gui.linkinfo.title", "Link Information:") + " " + downloadLink.getName());
        setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.link")));
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
        lblSlider = new JLabel(JDLocale.L("gui.linkinfo.rate", "Refreshrate") + " [" + slider.getValue() + "ms]");
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

    private void addEntry(String name, String string, JComponent value) {
        if (hmObjects.get(name) == null) {
            JLabel key;
            JDUtilities.addToGridBag(panel, key = new JLabel(string), 0, i, 1, 1, 0, 1, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);

            JDUtilities.addToGridBag(panel, value, 1, i, 1, 1, 1, 0, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);

            key.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            value.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            i++;
            hmObjects.put(name, value);
            hmObjects.put("JLabel_" + name, key);
        } else {
            if (hmObjects.get(name).getClass() == JProgressBar.class) {
                JProgressBar tmpJBar = (JProgressBar) hmObjects.get(name);
                tmpJBar.setValue(((JProgressBar) value).getValue());
                tmpJBar.setString(((JProgressBar) value).getString());
                tmpJBar.setEnabled(((JProgressBar) value).isEnabled());
                tmpJBar.setBackground((((JProgressBar) value).getBackground()));
                JLabel tmpJLbl = (JLabel) hmObjects.get("JLabel_" + name);
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
            } else if (hmObjects.get(name).getClass() == JTextField.class) {
                JTextField tmp = (JTextField) hmObjects.get(name);
                tmp.setText(((JTextField) value).getText());
                tmp.setHorizontalAlignment(JTextField.RIGHT);
            }
            i++;
            hmObjects.get(name).repaint();
        }
    }

    private void addEntry(String name, String label, String data) {
        if (hmObjects.get("JLabelCaption_" + name) == null) {
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
            hmObjects.put("JLabelCaption_" + name, key);
            hmObjects.put("JLabelValue_" + name, value);

            i++;
        } else {
            if (label != null && data != null) {
                JLabel key = (JLabel) hmObjects.get("JLabelCaption_" + name);
                JLabel value = (JLabel) hmObjects.get("JLabelValue_" + name);
                key.setText(label);
                value.setText(data);
                i++;
            }
        }
    }

    private void removeEntry(String name) {
        if (hmObjects.get("JLabelCaption_" + name) != null) {
            panel.remove(hmObjects.get("JLabelCaption_" + name));
            panel.remove(hmObjects.get("JLabelValue_" + name));

            hmObjects.remove("JLabelCaption_" + name);
            hmObjects.remove("JLabelValue_" + name);
            i--;
        } else if (hmObjects.get(name) != null) {
            panel.remove(hmObjects.get(name));
            panel.remove(hmObjects.get("JLabel_" + name));

            hmObjects.remove(name);
            hmObjects.remove("JLabel_" + name);
            i--;
        }
    }

    private void initDialog() {
        if (firstRun == true) {
            panel.setVisible(false);
        }

        addEntry("file", JDLocale.L("gui.linkinfo.file", "File"), new File(downloadLink.getFileOutput()).getName() + " @ " + downloadLink.getHost());

        if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().hasPassword()) {
            JTextField pw = new JTextField(downloadLink.getFilePackage().getPassword());
            pw.setEditable(false);
            addEntry("pass", JDLocale.L("gui.linkinfo.password", "Passwort"), pw);
        } else {
            removeEntry("pass");
        }

        if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().hasComment()) {
            JTextField comment = new JTextField(downloadLink.getFilePackage().getComment());
            comment.setEditable(false);
            addEntry("comment", JDLocale.L("gui.linkinfo.comment", "Kommentar"), comment);
        } else {
            removeEntry("comment");
        }

        if (downloadLink.getFilePackage() != null) {
            addEntry("package", JDLocale.L("gui.linkinfo.package", "Packet"), downloadLink.getFilePackage().toString());
        } else {
            removeEntry("package");
        }

        if (downloadLink.getDownloadSize() > 0) {
            addEntry("filesize", JDLocale.L("gui.linkinfo.filesize", "Dateigröße"), JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()));
        } else {
            removeEntry("filesize");
        }

        if (downloadLink.isAborted()) {
            addEntry("aborted", JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("linkinformation.download.aborted", "Abgebrochen"));
        } else {
            removeEntry("aborted");
        }

        if (downloadLink.isAvailabilityChecked()) {
            addEntry("avaible", JDLocale.L("gui.linkinfo.available", "Verfügbar"), downloadLink.isAvailable() ? JDLocale.L("gui.linkinfo.available.ok", "Datei OK") : JDLocale.L("linkinformation.available.error", "Fehler!"));
        } else {
            addEntry("avaible", JDLocale.L("gui.linkinfo.available", "Verfügbar"), JDLocale.L("gui.linkinfo.available.notchecked", "noch nicht überprüft"));
        }

        if (downloadLink.getDownloadSpeed() > 0) {
            addEntry("speed", JDLocale.L("gui.linkinfo.speed", "Geschwindigkeit"), downloadLink.getDownloadSpeed() / 1024 + " kb/s");
        } else {
            removeEntry("speed");
        }

        if (downloadLink.getFileOutput() != null) {
            addEntry("saveto", JDLocale.L("gui.linkinfo.saveto", "Speichern in"), downloadLink.getFileOutput());
        } else {
            removeEntry("saveto");
        }

        if (downloadLink.getLinkStatus().getRemainingWaittime() > 0) {
            addEntry("waittime", JDLocale.L("gui.linkinfo.waittime", "Wartezeit"), downloadLink.getLinkStatus().getRemainingWaittime() + " sek");
        } else {
            removeEntry("waittime");
        }

        if (downloadLink.getLinkStatus().isPluginActive()) {
            addEntry("linkstatus", JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.underway", "ist in Bearbeitung"));
        } else {
            addEntry("linkstatus", JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.notunderway", "ist nicht in Bearbeitung"));
        }

        if (!downloadLink.isEnabled()) {
            addEntry("enabled", JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.deactivated", "ist deaktiviert"));
        } else {
            addEntry("enabled", JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.activated", "ist aktiviert"));
        }

        addEntry("dlstatus", "download.status", downloadLink.getLinkStatus().getStatusString());

        DownloadInterface dl = downloadLink.getDownloadInstance();
        if (downloadLink.getLinkStatus().isPluginActive() && dl != null) {
            addEntry("chunks", JDLocale.L("download.chunks.label", "Chunks"), "");
            int j = 0;
            JProgressBar p;
            for (Chunk chunk : dl.getChunks()) {
                j++;
                p = new JProgressBar(0, 100);
                p.setMaximum(10000);
                p.setValue(chunk.getPercent());
                p.setStringPainted(true);
                p.setString(c.format(chunk.getPercent() / 100.0) + " % @ " + JDUtilities.formatKbReadable(chunk.getBytesPerSecond() / 1024) + "/s");
                addEntry("conn_" + j, JDLocale.L("download.chunks.connection", "Verbindung") + " " + j, p);
            }
        } else {
            removeEntry("chunks");
            for (int j = 0; j <= 20; ++j) {
                removeEntry("conn_" + j);
            }
        }

        if (firstRun == true) {
            firstRun = false;
            initDialog();
            panel.setVisible(true);
        }
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == slider) {
            lblSlider.setText(JDLocale.L("gui.linkinfo.rate", "Refreshrate") + " [" + slider.getValue() + "ms]");
            subConfig.setProperty(PROPERTY_REFRESHRATE, slider.getValue());
            subConfig.save();
        }
    }
}
