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
import java.io.File;
import java.text.DecimalFormat;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import jd.plugins.DownloadLink;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterface.Chunk;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Dies Klasse ziegt informationen zu einem DownloadLink an
 */
public class DownloadInfo extends JFrame {
    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();
    /**
     * 
     */
    private static final long serialVersionUID = -9146764850581039090L;
    private DownloadLink downloadLink;
    private int i = 0;
    private JPanel panel;
    private DecimalFormat c = new DecimalFormat("0.00");

    /**
     * @param frame
     * @param dlink
     */
    public DownloadInfo(JFrame frame, DownloadLink dLink) {
        super();
        downloadLink = dLink;
        setLayout(new BorderLayout(2, 2));
        setTitle(JDLocale.L("gui.linkinfo.title", "Link Information: ") + downloadLink.getName());
        setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.link")));
        setResizable(false);
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
        frame.setMaximumSize(getToolkit().getScreenSize());
    }

    private void addEntry(String label, JComponent value) {
        JLabel key;
        JDUtilities.addToGridBag(panel, key = new JLabel(label), 0, i, 1, 1, 0, 1, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);
        key.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

        JDUtilities.addToGridBag(panel, value, 1, i, 1, 1, 1, 0, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);
        value.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

        i++;
    }

    private void addEntry(String label, String data) {
        JLabel value = new JLabel(data);
        value.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        value.setForeground(Color.DARK_GRAY);
        addEntry(label, value);
    }

    private void initDialog() {
        this.i = 0;
        panel.removeAll();
        addEntry(JDLocale.L("gui.linkinfo.file", "File"), new File(downloadLink.getFileOutput()).getName() + " @ " + downloadLink.getHost());
        if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().hasPassword()) {
            addEntry(JDLocale.L("gui.linkinfo.password", "Passwort"), new JTextField(downloadLink.getFilePackage().getPassword()));
        }
        if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().hasComment()) {
            addEntry(JDLocale.L("gui.linkinfo.comment", "Kommentar"), downloadLink.getFilePackage().getComment());
        }
        if (downloadLink.getFilePackage() != null) {
            addEntry(JDLocale.L("gui.linkinfo.package", "Packet"), downloadLink.getFilePackage().toString());
        }
        if (downloadLink.getDownloadSize() > 0) {
            addEntry(JDLocale.L("gui.linkinfo.filesize", "Dateigröße"), JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()));
        }
        if (downloadLink.isAborted()) {
            addEntry(JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("linkinformation.download.aborted", "Abgebrochen"));
        }
        if (downloadLink.isAvailabilityChecked()) {
            addEntry(JDLocale.L("gui.linkinfo.available", "Verfügbar"), downloadLink.isAvailable() ? JDLocale.L("gui.linkinfo.available.ok", "Datei OK") : JDLocale.L("linkinformation.available.error", "Fehler!"));
        } else {
            addEntry(JDLocale.L("gui.linkinfo.available", "Verfügbar"), JDLocale.L("gui.linkinfo.available.notchecked", "noch nicht überprüft"));
        }
        if (downloadLink.getDownloadSpeed() > 0) {
            addEntry(JDLocale.L("gui.linkinfo.speed", "Geschwindigkeit"), downloadLink.getDownloadSpeed() / 1024 + " kb/s");
        }
        if (downloadLink.getFileOutput() != null) {
            addEntry(JDLocale.L("gui.linkinfo.saveto", "Speichern in"), downloadLink.getFileOutput());
        }
        if (downloadLink.getLinkStatus().getRemainingWaittime() > 0) {
            addEntry(JDLocale.L("gui.linkinfo.waittime", "Wartezeit"), downloadLink.getLinkStatus().getRemainingWaittime() + " sek");
        }
        if (downloadLink.getLinkStatus().isPluginActive()) {
            addEntry(JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.underway", "ist in Bearbeitung"));
        } else {
            addEntry(JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.notunderway", "ist nicht in Bearbeitung"));
        }
        if (!downloadLink.isEnabled()) {
            addEntry(JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.deactivated", "ist deaktiviert"));
        } else {
            addEntry(JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.activated", "ist aktiviert"));
        }

        addEntry("download.status", downloadLink.getLinkStatus().getStatusString());

        DownloadInterface dl = downloadLink.getDownloadInstance();
        if (downloadLink.getLinkStatus().isPluginActive() && dl != null) {
            addEntry(JDLocale.L("download.chunks.label", "Chunks"), "");
            int i = 1;
            JProgressBar p;
            for (Chunk chunk : dl.getChunks()) {
                addEntry(JDLocale.L("download.chunks.connection", "Verbindung") + " " + i, p = new JProgressBar(0, 100));
                p.setMaximum(10000);
                p.setValue(chunk.getPercent());
                p.setStringPainted(true);
                p.setString(c.format(chunk.getPercent() / 100.0) + " % @ " + JDUtilities.formatKbReadable(chunk.getBytesPerSecond() / 1024) + "/s");
            }

        }
    }
}
