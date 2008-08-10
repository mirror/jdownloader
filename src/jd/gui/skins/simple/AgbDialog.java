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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import jd.gui.skins.simple.Link.JLinkButton;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

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
        JPanel panel = new JPanel();
        setContentPane(panel);

        this.downloadLink = downloadLink;

        setModal(true);
        setLayout(new GridBagLayout());
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

        JDUtilities.addToGridBag(this, labelInfo, 1, 1, 2, 1, 1, 1, new Insets(10, 5, 0, 5), GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, linkAgb, 1, 3, 2, 1, 1, 1, new Insets(5, 5, 10, 5), GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, checkAgbAccepted, 1, 4, 2, 1, 1, 1, new Insets(5, 5, 15, 5), GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnCancel, 2, 5, 1, 1, 1, 1, new Insets(5, 5, 5, 5), GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, btnOK, 1, 5, 1, 1, 1, 1, new Insets(5, 5, 5, 5), GridBagConstraints.NONE, GridBagConstraints.EAST);

        countdownThread = new Thread() {

            @Override
            public void run() {
                int c = countdown;

                while (--c >= 0) {
                    if (countdownThread == null) return;
                    setTitle(JDLocale.L("gui.dialogs.agb_tos.title", "Allgemeine Geschäftsbedingungen nicht akzeptiert") + " [" + JDUtilities.formatSeconds(c) + "]");

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

        int n = 10;
        panel.setBorder(new EmptyBorder(n, n, n, n));

        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));
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
