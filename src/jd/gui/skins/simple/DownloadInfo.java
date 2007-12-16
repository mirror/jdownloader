package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Dies Klasse ziegt informationen zu einem DownloadLink an
 */
public class DownloadInfo extends JDialog {
    /**
     * 
     */
    private static final long serialVersionUID = -9146764850581039090L;
    @SuppressWarnings("unused")
    private static Logger     logger           = JDUtilities.getLogger();
    private DownloadLink      downloadLink;
    private int               i                = 0;
    private JPanel            panel;
    /**
     * @param frame
     * @param dlink
     */
    public DownloadInfo(JFrame frame, DownloadLink dlink) {
        super(frame);
        downloadLink = dlink;
        setModal(true);
        setLayout(new BorderLayout());
        this.setBackground(Color.WHITE);
        this.setTitle("Link Information");
        panel = new JPanel(new GridBagLayout());
        this.add(panel, BorderLayout.CENTER);
        initDialog();
        pack();
        setLocation((int) (frame.getLocation().getX() + frame.getWidth() / 2 - this.getWidth() / 2), (int) (frame.getLocation().getY() + frame.getHeight() / 2 - this.getHeight() / 2));
        setVisible(true);
    }
    private void initDialog() {
        addEntry("file", new File(downloadLink.getFileOutput()).getName() + " @ " + downloadLink.getHost());
        addEntry(null, null);
        if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().getPassword() != null) {
            addEntry("password", downloadLink.getFilePackage().getPassword());
        }
        if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().getComment() != null) {
            addEntry("comment", downloadLink.getFilePackage().getComment());
        }
        if (downloadLink.getFilePackage() != null) {
            addEntry("package", downloadLink.getFilePackage().toString());
        }
        if (downloadLink.getFilePackage() != null) {
            addEntry("doUnrar", downloadLink.getFilePackage().isUnPack()?"Yes":"No");
        }
        if (downloadLink.getFilePackage() != null) {
            addEntry("writeInfoFile", downloadLink.getFilePackage().isWriteInfoFile()?"Yes":"No");
        }
        if (downloadLink.getDownloadMax() > 0) {
            addEntry("fileSize", downloadLink.getDownloadMax() + " Bytes");
        }
        if (downloadLink.isAborted()) {
            addEntry("download", "Abgebrochen");
        }
        if (downloadLink.isAvailabilityChecked()) {
            addEntry("available", downloadLink.isAvailable() ? "Datei OK" : "Fehler!");
        }
        else {
            addEntry("available", "ist nicht überprüft");
        }
        if (downloadLink.getDownloadSpeed() > 0) {
            addEntry("speed", downloadLink.getDownloadSpeed() / 1024 + " kb/s");
        }
        if (downloadLink.getFileOutput() != null) {
            addEntry("saveTo", downloadLink.getFileOutput());
        }
        if (downloadLink.getRemainingWaittime() > 0) {
            addEntry("waitTime", downloadLink.getRemainingWaittime() + " sek");
        }
        if (downloadLink.isInProgress()) {
            addEntry("download", " ist in Bearbeitung");
        }
        else {
            addEntry("download", " ist nicht in Bearbeitung");
        }
        if (!downloadLink.isEnabled()) {
            addEntry("download", " ist deaktiviert");
        }
        else {
            addEntry("download", " ist aktiviert");
        }
        switch (downloadLink.getStatus()) {
            case DownloadLink.STATUS_TODO:
                addEntry("download.status", "In Warteschlange");
                break;
            case DownloadLink.STATUS_DONE:
                addEntry("download.status", downloadLink.getStatusText() + "Fertig");
                break;
            case DownloadLink.STATUS_ERROR_FILE_ABUSED:
                addEntry("download.status", downloadLink.getStatusText() + " ABUSED!");
                break;
            case DownloadLink.STATUS_ERROR_FILE_NOT_FOUND:
                addEntry("download.status", downloadLink.getStatusText() + " FILE NOT FOUND");
                break;
            case DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE:
                addEntry("download.status", downloadLink.getStatusText() + " Kurzzeitig nicht verfügbar");
                break;
            case DownloadLink.STATUS_ERROR_UNKNOWN:
                addEntry("download.status", downloadLink.getStatusText() + " Unbekannter Fehler");
                break;
            default:
                addEntry("download.status", downloadLink.getStatusText() + "Status ID: " + downloadLink.getStatus());
                break;
        }
        if (downloadLink.getPlugin().getCurrentStep() != null) {
            addEntry("Plugin Status", downloadLink.getPlugin().getInitID() + " : " + downloadLink.getPlugin().getCurrentStep().toString());
        }
    }
    private void addEntry(String label, String data) {
        if (label == null && data == null) {
            JDUtilities.addToGridBag(panel, new JSeparator(), 0, i, 2, 1, 0, 0, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
            return;
        }
        JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L("gui.linkInfo."+label,label)), 0, i, 1, 1, 0, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, new JLabel(data), 1, i, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        i++;
    }
}
