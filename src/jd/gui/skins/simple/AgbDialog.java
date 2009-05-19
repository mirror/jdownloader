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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;

import jd.gui.skins.simple.components.JLinkButton;
import jd.nutils.Formatter;
import jd.nutils.Screen;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import net.miginfocom.swing.MigLayout;

/**
 * Dieser Dialog wird angezeigt, wenn ein Download mit einem Plugin getätigt
 * wird, dessen Agbs noch nicht akzeptiert wurden
 * 
 * @author eXecuTe
 */

public class AgbDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JButton btnCancel;

    private JButton btnOK;

    private JCheckBox checkAgbAccepted;

    private DownloadLink downloadLink;

    private Thread countdownThread;

    /**
     * Zeigt einen Dialog, in dem man die Hoster AGB akzeptieren kann
     * 
     * @param downloadLink
     *            abzuarbeitender Link
     */

    public AgbDialog(DownloadLink downloadLink, final int countdown) {
        super();

        this.downloadLink = downloadLink;

        setModal(true);
        // gapleft push
        setLayout(new MigLayout("wrap 1", "[center]", ""));
        getRootPane().setDefaultButton(btnOK);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle(JDLocale.L("gui.dialogs.agb_tos.title", "Allgemeine Geschäftsbedingungen nicht akzeptiert"));

        JLabel labelInfo = new JLabel(JDLocale.LF("gui.dialogs.agb_tos.description", "Die Allgemeinen Geschäftsbedingungen (AGB) von %s wurden nicht gelesen und akzeptiert.", downloadLink.getPlugin().getHost()));

        JLinkButton linkAgb = new JLinkButton(JDLocale.LF("gui.dialogs.agb_tos.readAgb", "%s AGB lesen", downloadLink.getPlugin().getHost()), downloadLink.getPlugin().getAGBLink());
        linkAgb.addActionListener(this);
        linkAgb.setFocusable(false);

        checkAgbAccepted = new JCheckBox(JDLocale.L("gui.dialogs.agb_tos.agbAccepted", "Ich bin mit den Allgemeinen Geschäftsbedingungen einverstanden"));
        checkAgbAccepted.addActionListener(this);
        checkAgbAccepted.setFocusable(false);

        btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnOK.addActionListener(this);

        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);
        btnCancel.setFocusable(false);

        add(labelInfo);
        add(linkAgb);
        add(checkAgbAccepted);
        add(btnOK, "split 2");
        add(btnCancel);

        countdownThread = new Thread() {

            // @Override
            public void run() {
                int c = countdown;

                while (--c >= 0) {
                    if (countdownThread == null) return;
                    setTitle(JDLocale.L("gui.dialogs.agb_tos.title", "Allgemeine Geschäftsbedingungen nicht akzeptiert") + " [" + Formatter.formatSeconds(c) + "]");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    if (!isVisible()) return;

                }
                dispose();
            }

        };
        countdownThread.start();

        pack();
        setResizable(false);
        setLocation(Screen.getCenterOfComponent(null, this));
        setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == btnOK) {
            downloadLink.getPlugin().setAGBChecked(checkAgbAccepted.isSelected());
            if (checkAgbAccepted.isSelected()) {
                downloadLink.getLinkStatus().reset();
            }
            dispose();
        } else if (e.getSource() == btnCancel) {
            dispose();
        }

        if (countdownThread != null && countdownThread.isAlive()) {
            countdownThread.interrupt();
        }
        countdownThread = null;
        setTitle(JDLocale.L("gui.dialogs.agb_tos.title", "Allgemeine Geschäftsbedingungen nicht akzeptiert"));
    }

}
