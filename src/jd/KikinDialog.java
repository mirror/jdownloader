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

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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

    private JTextPane textFieldAccept;

    /**
     * Radiobutton to accept Kikin Installadtion
     */
    private JRadioButton radioAccept;

    /**
     * Radio button to deny kikin installation
     */
    private JRadioButton radioDeny;

    private JTextPane textFieldDeny;

    private JTextPane textField;

    /**
     * 
     */
    public KikinDialog() {
        super(UserIO.NO_COUNTDOWN, JDL.L("gui.installer.kikin.title", "Kikin Installer"), null, JDL.L("gui.installer.kikin.ok", "Continue"), JDL.L("gui.installer.kikin.cancel", "Cancel"));
        init();
    }

    /**
     * 
     */
    private static final long serialVersionUID = -7647771640756844691L;

    public JComponent contentInit() {
        final JPanel cp = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[][fill,grow][]"));
        // cp.setLayout(new MigLayout("ins 0,wrap 1,debug", "[fill,grow]"));
        final JPanel p = new JPanel(new MigLayout("ins 5,wrap 2"));

        final JLabel lbl = new JLabel(JDL.L("gui.installer.kikin.message", "Free! Personalize your search experience"));
        p.add(lbl, "alignx left, aligny bottom");
        final Font f = lbl.getFont();

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

        // Create the radio buttons using the actions
        radioAccept = new JRadioButton();
        radioDeny = new JRadioButton();
        radioDeny.setSelected(true);
        // Associate the two buttons with a button group
        final ButtonGroup group = new ButtonGroup();
        group.add(radioAccept);
        group.add(radioDeny);

        radioAccept.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (radioAccept.isSelected()) {
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
        textField.setText("<style type='text/css'> body {        font-family: Geneva, Arial, Helvetica, sans-serif; font-size:9px;}</style>" + JDL.L("gui.installer.kikin.whatis3", "<b>kikin uses your browsing history to give you personalized content from sites you like.   <a href=\"http://jdownloader.org/kikin\">more...</a></b>"));
        textField.setEditable(false);
        final HyperlinkListener hyperlinkListener = new HyperlinkListener() {
            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        JLink.openURL(e.getURL());
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        JDUtilities.runCommand("cmd", new String[] { "/c", "start " + e.getURL() + "" }, null, 0);
                    }
                }
            }
        };
        textField.addHyperlinkListener(hyperlinkListener);
        textFieldAccept = new JTextPane();
        textFieldAccept.setContentType("text/html");

        textFieldAccept.setBorder(null);

        textFieldAccept.setOpaque(false);
        textFieldAccept.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        textFieldAccept.setText("<style type='text/css'> body {        font-family: Geneva, Arial, Helvetica, sans-serif; font-size:9px;}</style>" + JDL.L("gui.installer.kikin.agree3", "<span>Yes, I would like to install kikin. I agree to the <a href=\"http://www.kikin.com/terms\">Terms of Service</a> and <a href=\"http://www.kikin.com/privacy\">Privacy Policy</a></span>"));
        textFieldAccept.setEditable(false);

        textFieldAccept.addHyperlinkListener(hyperlinkListener);

        textFieldDeny = new JTextPane();
        textFieldDeny.setContentType("text/html");

        textFieldDeny.setBorder(null);

        textFieldDeny.setOpaque(false);
        textFieldDeny.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        textFieldDeny.setText("<style type='text/css'> body {        font-family: Geneva, Arial, Helvetica, sans-serif; font-size:9px;}</style>" + JDL.L("gui.installer.kikin.deny3", "<span>No, thanks</span>"));
        textFieldDeny.setEditable(false);
        textFieldDeny.addHyperlinkListener(hyperlinkListener);
        final JPanel pp = new JPanel(new MigLayout("ins 0,wrap 2", "[shrink][grow,fill]", "[]"));

        pp.add(textField, "spanx");
        pp.add(radioAccept, "aligny ");
        pp.add(textFieldAccept, "aligny bottom,gapbottom 4");
        pp.add(radioDeny, "aligny bottom");
        pp.add(textFieldDeny, "aligny bottom,gapbottom 4");
        pp.add(new JSeparator(), "spanx,growx,pushx");
        cp.add(pp, "growx,pushx");
        // btnOK.setEnabled(false);

        final String osString = OSDetector.getOSString();

        btnOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (radioAccept.isSelected()) {
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
                        new Browser().getPage("http://service.jdownloader.org/update/inst.php?k=1&o=" + osString + "&v=" + JDUtilities.getRevision());
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                } else {
                    try {
                        new Browser().getPage("http://service.jdownloader.org/update/inst.php?k=0&o=" + osString + "&v=" + JDUtilities.getRevision());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                try {
                    new Browser().getPage("http://service.jdownloader.org/update/inst.php?k=0&o=" + osString + "&v=" + JDUtilities.getRevision());
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
