package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import jd.JDUtilities;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;

/**
 * Dies Klasse ziegt informationen zu einem DownloadLink an
 */
public class DownloadInfo extends JDialog {

    /**
     * 
     */
    private static final long serialVersionUID = -9146764850581039090L;

    @SuppressWarnings("unused")
    private static Logger     logger           = Plugin.getLogger();

    private DownloadLink      downloadLink;


    private int               i                = 0;

    private JPanel panel;

    public DownloadInfo(JFrame frame,DownloadLink dlink) {
        super(frame);
        downloadLink = dlink;
        setModal(true);
        setLayout(new BorderLayout());
        this.setBackground(Color.WHITE);
        this.setTitle("Link Information");
        panel= new JPanel(new GridBagLayout());
        this.add(panel, BorderLayout.CENTER);
        initDialog();
        pack();
       
     
        setLocation((int)(frame.getLocation().getX()+ frame.getWidth()/2-this.getWidth()/2),(int)(frame.getLocation().getY()+ frame.getHeight()/2-this.getHeight()/2));
        setVisible(true);
 
       
    }

    private void initDialog() {
        try {

            addEntry("Datei", new File(downloadLink.getFileOutput()).getName() + " @ " + new URL(downloadLink.getUrlDownloadDecrypted()).getHost());
            addEntry(null, null);

            if (downloadLink.getPassword() != null) {
                addEntry("Archiv Passwort", downloadLink.getPassword());

            }
            if (downloadLink.getComment() != null) {
                addEntry("Kommentar", downloadLink.getComment());
            }
            if (downloadLink.getFilePackage() != null) {
                addEntry("Paket", downloadLink.getFilePackage().toString());
            }

            if (downloadLink.getDownloadMax() > 0) {
                addEntry("Dateigröße", downloadLink.getDownloadMax() + " Bytes");

            }
            switch (downloadLink.getStatus()) {
                case DownloadLink.STATUS_TODO:
                    addEntry("Download Status", "In Warteschlange");
                    break;
                case DownloadLink.STATUS_DONE:
                    addEntry("Download Status", downloadLink.getStatusText());
                    break;
                case DownloadLink.STATUS_ERROR_FILE_ABUSED:
                    addEntry("Download Status", downloadLink.getStatusText() + " ABUSED!");
                    break;
                case DownloadLink.STATUS_ERROR_FILE_NOT_FOUND:
                    addEntry("Download Status", downloadLink.getStatusText() + " FILE NOT FOUND");
                    break;
                case DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE:
                    addEntry("Download Status", downloadLink.getStatusText() + " Kurzzeitig nicht verfügbar");
                    break;
                case DownloadLink.STATUS_ERROR_UNKNOWN:
                    addEntry("Download Status", downloadLink.getStatusText() + " Unbekannter Fehler");
                    break;

            }

            if (downloadLink.getPlugin().getCurrentStep() != null) {
                addEntry("Plugin Status", downloadLink.getPlugin().getCurrentStep().toString());
            }

        }
        catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void addEntry(String label, String data) {

        if (label == null && data == null) {
            JDUtilities.addToGridBag(panel, new JSeparator(), 0, i, 2, 1, 0, 0, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
            return;
        }
        JDUtilities.addToGridBag(panel, new JLabel(label), 0, i, 1, 1, 0, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, new JLabel(data), 1, i, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        i++;

    }

}
