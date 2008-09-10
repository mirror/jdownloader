//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Dies Klasse ziegt informationen zu einem FilePackage an
 */
public class PackageInfo extends JDialog {
    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();
    /**
     * 
     */
    private static final long serialVersionUID = -9146764850581039090L;
    private FilePackage fp;
    private int i = 0;
    private JPanel panel;
    private DecimalFormat c = new DecimalFormat("0.00");
    private HashMap<String, JComponent> hmObjects = new HashMap<String, JComponent>();

    /**
     * @param frame
     * @param dlink
     */
    public PackageInfo(JFrame frame, FilePackage fp) {
        super();
        this.fp = fp;
        setLayout(new BorderLayout(2, 2));
        setTitle(JDLocale.L("gui.packageinfo.title", "Package Information: ") + fp.getName());
        setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.package_opened")));
        setResizable(true);
        setAlwaysOnTop(true);

        panel = new JPanel(new GridBagLayout());
        int n = 10;
        panel.setBorder(new EmptyBorder(n, n, n, n));
        panel.setBackground(Color.WHITE);
        panel.setForeground(Color.WHITE);
        this.add(new JScrollPane(panel));

        new Thread() {
            @Override
            public void run() {
                do {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    initDialog();
                    validate();
                } while (isVisible());
                initDialog();
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
                tmpJLbl.setForeground(tmpJBar.getBackground());
            } else if (hmObjects.get(string).getClass() == JTextField.class) {
                JTextField tmp = (JTextField) hmObjects.get(string);
                tmp.setText(((JTextField) value).getText());
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
        addEntry(JDLocale.L("gui.packageinfo.name", "Name"), fp.getName());
        if (fp.hasPassword()) addEntry(JDLocale.L("gui.packageinfo.password", "Password"), new JTextField(fp.getPassword()));
        if (fp.hasComment()) addEntry(JDLocale.L("gui.packageinfo.comment", "Comment"), fp.getComment());
        addEntry(JDLocale.L("gui.packageinfo.dldirectory", "Downloaddirectory"), fp.getDownloadDirectory());
        addEntry(JDLocale.L("gui.packageinfo.packagesize", "Packagesize"), JDUtilities.formatKbReadable(fp.getTotalEstimatedPackageSize()) + " [" + fp.getTotalEstimatedPackageSize() + " KB]");
        addEntry(JDLocale.L("gui.packageinfo.loaded", "Loaded"), JDUtilities.formatKbReadable(fp.getTotalKBLoaded()) + " [" + fp.getTotalKBLoaded() + " KB]");
        addEntry(JDLocale.L("gui.packageinfo.links", "Links"), "");

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
    }
}
