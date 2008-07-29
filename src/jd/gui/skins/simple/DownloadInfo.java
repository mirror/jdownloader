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
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
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
    /**
     * 
     */
    private static final long serialVersionUID = -9146764850581039090L;
    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();
    private DownloadLink downloadLink;
    private int i = 0;
    private JPanel panel;
    private JScrollPane sp = null;

    /**
     * @param frame
     * @param dlink
     */
    public DownloadInfo(JFrame frame, DownloadLink dlink) {
        super(JDLocale.L("gui.linkinformation.title","Link Information: ")+dlink.getName());
        downloadLink = dlink;
        //setModal(true);
        setLayout(new BorderLayout(2,2));
        this.setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.link")));
        this.setResizable(false);
        this.setAlwaysOnTop(true);
       
        // this.getContentPane().setBackground(Color.WHITE);
        // this.getContentPane().setForeground(Color.WHITE);

       
        
        new Thread() {
            public void run() {
                do  {
                    
                    
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        
                        e.printStackTrace();
                    }
                    initDialog();
                    validate();
                }while((isVisible()));
                initDialog();
            }
        }.start();
        initDialog();
        pack();
        setLocation((int) (frame.getLocation().getX() + frame.getWidth() / 2 - this.getWidth() / 2), (int) (frame.getLocation().getY() + frame.getHeight() / 2 - this.getHeight() / 2));
        setVisible(true);
        

    }

    private void initDialog() {
        panel = new JPanel(new GridBagLayout());
        int n = 10;
        panel.setBorder(new EmptyBorder(n,n,n,n));
        panel.setBackground(Color.WHITE);
        panel.setForeground(Color.WHITE);
        if (sp != null) this.remove(sp);
        this.add(sp = new JScrollPane(panel), BorderLayout.CENTER);
        addEntry("file", new File(downloadLink.getFileOutput()).getName() + " @ " + downloadLink.getHost());
        addEntry(null, (String) null);
        if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().getPassword() != null) {
            addEntry(JDLocale.L("linkinformation.password.name", "Passwort"), downloadLink.getFilePackage().getPassword());
        }
        if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().getComment() != null) {
            addEntry(JDLocale.L("linkinformation.comment.name", "Kommentar"), downloadLink.getFilePackage().getComment());
        }
        if (downloadLink.getFilePackage() != null) {
            addEntry(JDLocale.L("linkinformation.package.name", "Packet"), downloadLink.getFilePackage().toString());
        }
        // if (downloadLink.getFilePackage() != null) {
        // addEntry("doUnrar",
        // downloadLink.getFilePackage().isUnPack()?"Yes":"No");
        // }
        // if (downloadLink.getFilePackage() != null) {
        // addEntry("writeInfoFile",
        // downloadLink.getFilePackage().isWriteInfoFile()?"Yes":"No");
        // }
        if (downloadLink.getDownloadMax() > 0) {
            addEntry(JDLocale.L("linkinformation.filesize.name", "Dateigröße"), JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()));
        }
        // JDLocale.L("linkinformation.", "")
        if (downloadLink.isAborted()) {
            addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.aborted", "Abgebrochen"));
        }
        if (downloadLink.isAvailabilityChecked()) {
            addEntry(JDLocale.L("linkinformation.available.name", "Verfügbar"), downloadLink.isAvailable() ? JDLocale.L("linkinformation.available.ok", "Datei OK") : JDLocale.L("linkinformation.available.error", "Fehler!"));
        } else {
            addEntry(JDLocale.L("linkinformation.available.name", "Verfügbar"), JDLocale.L("linkinformation.available.notchecked", "noch nicht überprüft"));
        }
        if (downloadLink.getDownloadSpeed() > 0) {
            addEntry(JDLocale.L("linkinformation.speed.name", "Geschwindigkeit"), downloadLink.getDownloadSpeed() / 1024 + " kb/s");
        }
        if (downloadLink.getFileOutput() != null) {
            addEntry(JDLocale.L("linkinformation.saveto.name", "Speichern in"), downloadLink.getFileOutput());
        }
        if (downloadLink.getLinkStatus().getRemainingWaittime() > 0) {
            addEntry(JDLocale.L("linkinformation.waittime.name", "Wartezeit"), downloadLink.getLinkStatus().getRemainingWaittime() + " sek");
        }
        if (downloadLink.getLinkStatus().isPluginActive()) {
            addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.underway", " ist in Bearbeitung"));
        } else {
            addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.notunderway", " ist nicht in Bearbeitung"));
        }
        if (!downloadLink.isEnabled()) {
            addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.deactivated", " ist deaktiviert"));
        } else {
            addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.activated", " ist aktiviert"));
        }

        addEntry("download.status", downloadLink.getLinkStatus().getStatusText());

        DownloadInterface dl;
        if (downloadLink.getLinkStatus().isPluginActive() && (dl = downloadLink.getDownloadInstance()) != null) {
            addEntry("download.chunks.label", "");
            int i = 1;
            for (Iterator<Chunk> it = dl.getChunks().iterator(); it.hasNext(); i++) {
                JProgressBar p;
                Chunk next = it.next();
                addEntry(JDLocale.L("download.chunks.connection", "Verbindung") + " " + i, p = new JProgressBar(0, 100));
                p.setValue(next.getBytesLoaded() * 100 / Math.max(1,next.getChunkSize()));
                p.setStringPainted(true);
                p.setString(JDUtilities.formatKbReadable((int)next.getBytesPerSecond()/1024)+"/s "+ JDUtilities.getPercent(next.getBytesLoaded(), next.getChunkSize()));
            }

        }
    }

    private void addEntry(String string, JComponent value) {

        JLabel key;
        JDUtilities.addToGridBag(panel, key = new JLabel(string), 0, i, 1, 1, 0, 1, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);

        JDUtilities.addToGridBag(panel, value, 1, i, 1, 1, 1, 0, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);
        key.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        value.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

        i++;

    }

    private void addEntry(String label, String data) {
        if (label == null && data == null) {
            JDUtilities.addToGridBag(panel, new JSeparator(), 0, i, 2, 1, 0, 0, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
            return;
        }
        JLabel key;
        JDUtilities.addToGridBag(panel, key = new JLabel(JDLocale.L("gui.linkInfo." + label, label)), 0, i, 1, 1, 0, 1, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);
        JLabel value;
        JDUtilities.addToGridBag(panel, value = new JLabel(data), 1, i, 1, 1, 1, 0, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);
        key.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        value.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        value.setForeground(Color.DARK_GRAY);

        i++;
    }
}
