//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.gui.UserIO;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.dialog.AbstractDialog;
import jd.http.Browser;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class KikinDialog extends AbstractDialog {

    private JLabel label;
    private JCheckBox checkbox;
    private JTextPane textField;

    public KikinDialog() {
        super(UserIO.NO_COUNTDOWN, JDL.L("gui.installer.kikin.title", "Kikin Installer"), null, JDL.L("gui.installer.kikin.ok", "Continue"), JDL.L("gui.installer.kikin.cancel", "Cancel"));

        init();
    }

    /**
* 
*/
    private static final long serialVersionUID = -7647771640756844691L;

    public JComponent contentInit() {
        JPanel cp = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[][fill,grow][]"));
        // cp.setLayout(new MigLayout("ins 0,wrap 1,debug", "[fill,grow]"));
        JPanel p = new JPanel(new MigLayout("ins 5,wrap 2"));

        JLabel lbl;
        p.add(lbl = new JLabel(JDL.L("gui.installer.kikin.message", "Free! Personalize your search experience")), "alignx left, aligny bottom");
        Font f = lbl.getFont();

        // bold
        lbl.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));

        p.add(new JLabel(JDImage.getImageIcon(JDUtilities.getResourceFile("tools/Windows/kikin/kikin.png"))), "alignx right,aligny top");
        p.add(new JSeparator(), "spanx,growx,pushx");
        if (JDL.getLocale().getLanguageCode().equals("de")) {
            label = new JLabel(JDImage.getImageIcon(JDUtilities.getResourceFile("tools/Windows/kikin/ins_de.png")));
        } else {
            label = new JLabel(JDImage.getImageIcon(JDUtilities.getResourceFile("tools/Windows/kikin/ins_en.png")));
        }
        cp.add(p, "growx, pushx");
        cp.add(label, "alignx left,aligny top");
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setVerticalAlignment(SwingConstants.TOP);
        checkbox = new JCheckBox();

        checkbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (checkbox.isSelected()) {
                    btnOK.setEnabled(true);
                    btnOK.setToolTipText(null);
                } else {
                    btnOK.setEnabled(false);
                    btnOK.setToolTipText(JDL.L("gui.installer.kikin.tooltip", "Please read and accept the conditions"));
                }

            }

        });

        textField = new JTextPane();
        textField.setContentType("text/html");

        textField.setBorder(null);

        textField.setOpaque(false);
        textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        textField.setText("<style type='text/css'> body {        font-family: Geneva, Arial, Helvetica, sans-serif; font-size:9px;}</style>" + JDL.L("gui.installer.kikin.agree", "<b><a href=\"http://jdownloader.org/kikin\">What is Kikin?</a> <br/>Best Parts? kikin is free and works automatically.<br>I agree to the kikin <a href=\"http://www.kikin.com/terms-of-service\">Terms of Service</a> and <a href=\"http://www.kikin.com/privacy-policy\">Privacy Policy</a></b>"));
        textField.setEditable(false);
        textField.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {

                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        JLink.openURL(e.getURL());
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        JDUtilities.runCommand("cmd", new String[] { "/c", "start " + e.getURL() + "" }, null, 0);
                    }
                }

            }

        });
        JPanel pp = new JPanel(new MigLayout("ins 0,wrap 2", "[shrink][grow,fill]", "[]"));
        pp.add(checkbox, "aligny bottom");
        pp.add(textField, "aligny bottom,gapbottom 2");
        pp.add(new JSeparator(), "spanx,growx,pushx");
        cp.add(pp, "growx,pushx");
        btnOK.setEnabled(false);
        btnOK.setToolTipText(JDL.L("gui.installer.kikin.tooltip", "Please read and accept the conditions"));
        btnOK.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // KikinDialog.this.setVisible(false);
                File file = JDUtilities.getResourceFile("tools/Windows/kikin/kikin_installer.exe");
                // Executer exec = new Executer(file.getAbsolutePath());
                // exec.setWaitTimeout(1000000);
                // exec.start();
                // exec.waitTimeout();
                // if (exec.getException() != null) {
                System.out.println("Install " + file.getAbsolutePath());
                JDUtilities.runCommand("cmd", new String[] { "/c", "start  " + file.getName() + "" }, file.getParent(), 10 * 60000);
                // }
                try {
                    new Browser().getPage("http://service.jdownloader.org/update/inst.php?k=1&o=" + OSDetector.getOSString() + "&v=" + JDUtilities.getRevision());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            }

        });

        btnCancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                try {
                    new Browser().getPage("http://service.jdownloader.org/update/inst.php?k=0&o=" + OSDetector.getOSString() + "&v=" + JDUtilities.getRevision());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }

        });
        return cp;
    }

    public Integer getReturnID() {
        return super.getReturnValue();
    }

    protected void packed() {
        this.setSize(550, 400);
    }

}
