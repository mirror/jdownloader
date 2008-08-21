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
import java.util.Iterator;
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
import jd.utils.JDUtilities;

/**
 * Dies Klasse ziegt informationen zu einem DownloadLink an
 */
public class PackageInfo extends JDialog {
    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();
    /**
     * 
     */
    private static final long serialVersionUID = -9146764850581039090L;
    private jd.plugins.FilePackage fp;
    private int i = 0;
    private JPanel panel;
    private JScrollPane sp;
    private DecimalFormat c = new DecimalFormat("0.00");

    /**
     * @param frame
     * @param dlink
     */
    public PackageInfo(JFrame frame, FilePackage fp) {
        super();
        this.fp = fp;
        setLayout(new BorderLayout(2, 2));
        setResizable(false);        
        setTitle(JDLocale.L("gui.frame.packageinfo.title", "Package Information: " + fp.getName()));
        setAlwaysOnTop(true);
        new Thread() {
            @Override
            public void run() {
                do {
                    try {
                        Thread.sleep(5000);
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
    }

    private void addEntry(String string, JComponent value) {
        JLabel key;
        JDUtilities.addToGridBag(panel, key = new JLabel(string), 0, i, 1, 1, 0, 1, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);

        JDUtilities.addToGridBag(panel, value, 1, i, 1, 1, 1, 0, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);

        key.setBorder(new EmptyBorder(0, 0, 0, 5));
        value.setBorder(new EmptyBorder(0, 5, 0, 0));
        i++;

    }

    private void addEntry(String label, String data) {
        if (label == null && data == null) {
            JDUtilities.addToGridBag(panel, new JSeparator(), 0, i, 2, 1, 0, 0, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
            return;
        }
        JLabel key;
        JDUtilities.addToGridBag(panel, key = new JLabel(JDLocale.L("gui.frame.packageinfo.entry" + label, label)), 0, i, 1, 1, 0, 1, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);
        JLabel value;
        JDUtilities.addToGridBag(panel, value = new JLabel(data), 1, i, 1, 1, 1, 0, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);

        key.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        value.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        value.setForeground(Color.DARK_GRAY);
        i++;
    }

    private void initDialog() {
        panel = new JPanel(new GridBagLayout());
        int n = 10;
        panel.setBorder(new EmptyBorder(n, n, n, n));
        panel.setBackground(Color.WHITE);
        panel.setForeground(Color.WHITE);
        if (sp != null) {
            this.remove(sp);
        }
        this.add(sp = new JScrollPane(panel), BorderLayout.CENTER);
        addEntry("name", fp.getName());
        addEntry(null, (String) null);
        if (fp.hasPassword()) addEntry("password", new JTextField(fp.getPassword()));
        addEntry(null, (String) null);
        if (fp.hasComment()) addEntry("comment", fp.getComment());
        addEntry("dldirectory", fp.getDownloadDirectory());
        addEntry("packagesize", JDUtilities.formatKbReadable(fp.getTotalEstimatedPackageSize()) + " " + fp.getTotalEstimatedPackageSize() + " KB");
        addEntry("loaded", JDUtilities.formatKbReadable(fp.getTotalKBLoaded()) + " " + fp.getTotalKBLoaded() + " KB");
        addEntry("links", "");
        DownloadLink next = null;
        int i = 1;
        for (Iterator<DownloadLink> it = fp.getDownloadLinks().iterator(); it.hasNext(); i++) {
            next = it.next();
            JProgressBar p;

            addEntry(i + ". " + next.getName(), p = new JProgressBar(0, 100));
            p.setMaximum(10000);
            p.setValue(next.getPercent());
            p.setStringPainted(true);
            p.setString(JDUtilities.formatKbReadable(next.getDownloadSpeed() / 1024) + "/s " + c.format(next.getPercent() / 100.0) + " %| " + next.getDownloadCurrent() + "/" + next.getDownloadSize() + " bytes");

        }
    }
}
