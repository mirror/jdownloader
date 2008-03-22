//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


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
            addEntry(JDLocale.L("linkinformation.password.name", "Passwort"), downloadLink.getFilePackage().getPassword());
        }
        if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().getComment() != null) {
            addEntry(JDLocale.L("linkinformation.comment.name", "Kommentar"), downloadLink.getFilePackage().getComment());
        }
        if (downloadLink.getFilePackage() != null) {
            addEntry(JDLocale.L("linkinformation.package.name", "Packet"), downloadLink.getFilePackage().toString());
        }
//        if (downloadLink.getFilePackage() != null) {
//            addEntry("doUnrar", downloadLink.getFilePackage().isUnPack()?"Yes":"No");
//        }
//        if (downloadLink.getFilePackage() != null) {
//            addEntry("writeInfoFile", downloadLink.getFilePackage().isWriteInfoFile()?"Yes":"No");
//        }
        if (downloadLink.getDownloadMax() > 0) {
            addEntry(JDLocale.L("linkinformation.filesize.name", "Dateigröße"), JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()));
        }
        //JDLocale.L("linkinformation.", "")
        if (downloadLink.isAborted()) {
            addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.aborted", "Abgebrochen"));
        }
        if (downloadLink.isAvailabilityChecked()) {
            addEntry(JDLocale.L("linkinformation.available.name", "Verfügbar"), downloadLink.isAvailable() ? JDLocale.L("linkinformation.available.ok", "Datei OK") : JDLocale.L("linkinformation.available.error", "Fehler!"));
        }
        else {
            addEntry(JDLocale.L("linkinformation.available.name", "Verfügbar"), JDLocale.L("linkinformation.available.notchecked", "noch nicht überprüft"));
        }
        if (downloadLink.getDownloadSpeed() > 0) {
            addEntry(JDLocale.L("linkinformation.speed.name", "Geschwindigkeit"), downloadLink.getDownloadSpeed() / 1024 + " kb/s");
        }
        if (downloadLink.getFileOutput() != null) {
            addEntry(JDLocale.L("linkinformation.saveto.name", "Speichern in"), downloadLink.getFileOutput());
        }
        if (downloadLink.getRemainingWaittime() > 0) {
            addEntry(JDLocale.L("linkinformation.waittime.name", "Wartezeit"), downloadLink.getRemainingWaittime() + " sek");
        }
        if (downloadLink.isInProgress()) {
            addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.underway", " ist in Bearbeitung"));
        }
        else {
            addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.notunderway", " ist nicht in Bearbeitung"));
        }
        if (!downloadLink.isEnabled()) {
            addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.deactivated", " ist deaktiviert"));
        }
        else {
            addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.activated", " ist aktiviert"));
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
            addEntry(JDLocale.L("linkinformation.pluginstatus.name", "Plugin Status"), downloadLink.getPlugin().getInitID() + " : " + downloadLink.getPlugin().getCurrentStep().toString());
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
