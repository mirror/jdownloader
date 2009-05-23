package jd.kikin;

//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.userio.dialog.AbstractDialog;
import jd.nutils.Executer;
import jd.nutils.JDImage;
import jd.nutils.nativeintegration.LocaleBrowser;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class KikinDialog extends AbstractDialog {

    private JLabel label;
    private JCheckBox checkbox;
    private JTextPane textField;

    public KikinDialog() {
        super(UserIO.NO_COUNTDOWN, JDLocale.L("gui.installer.kikin.title", "Kikin Installer"), null, JDLocale.L("gui.installer.kikin.ok", "Continue"), JDLocale.L("gui.installer.kikin.cancel", "Cancel"));

        init();
    }

    /**
* 
*/
    private static final long serialVersionUID = -7647771640756844691L;

    public void contentInit(JPanel cp) {

        // cp.setLayout(new MigLayout("ins 0,wrap 1,debug", "[fill,grow]"));
        JPanel p = new JPanel(new MigLayout("ins 5,wrap 2"));
        p.setBackground(Color.WHITE);
        cp.setBackground(Color.WHITE);
        p.add(new JLabel(JDLocale.L("gui.installer.kikin.message", "Personalize your search experience")), "alignx left, aligny bottom");
        p.add(new JLabel(JDImage.getImageIcon(JDUtilities.getResourceFile("tools/Windows/kikin/kikin.png"))), "alignx right,aligny top");
        p.add(new JSeparator(), "spanx,growx,pushx");
        String loc = JDLocale.getLocale();
        if (JDLocale.getLocale().equalsIgnoreCase("german")) {
            label = new JLabel(JDImage.getImageIcon(JDUtilities.getResourceFile("tools/Windows/kikin/ins_de.png")));
        } else {
            label = new JLabel(JDImage.getImageIcon(JDUtilities.getResourceFile("tools/Windows/kikin/ins_en.png")));
        }
        cp.add(p, "growx, pushx");
        cp.add(label);
        checkbox = new JCheckBox();

        checkbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (checkbox.isSelected()) {
                    btnOK.setEnabled(true);
                    btnOK.setToolTipText(null);
                } else {
                    btnOK.setEnabled(false);
                    btnOK.setToolTipText(JDLocale.L("gui.installer.kikin.tooltip", "Please read and accept the conditions"));
                }

            }

        });
        cp.add(checkbox, "split 2,growx 0,gaptop 18");
        checkbox.setBackground(Color.WHITE);
        textField = new JTextPane();
        textField.setContentType("text/html");
        textField.setBackground(Color.WHITE);
        textField.setBorder(null);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.setText(JDLocale.L("gui.installer.kikin.agree", "<b>Best Parts? kikin is free and works automatically.<br>I agree to the kikin <a href=\"http://www.kikin.com/terms-of-service\">Terms of Service</a> and <a href=\"http://www.kikin.com/privacy-policy\">Privacy Policy</a> </b>"));
        textField.setEditable(false);
        textField.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {
                // (e);

                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {

                   

                    try {
                        LocaleBrowser.openDefaultURL(e.getURL());
                    } catch (Exception e1) {
                        JDLogger.getLogger().warning("Could not open URL " + e.getURL());
                    }

                    //
                }
            }

        });
        cp.add(textField, "pushx, growx,gaptop 3");
        cp.add(new JSeparator(), "spanx,growx,pushx");
        btnOK.setEnabled(false);
        btnOK.setToolTipText(JDLocale.L("gui.installer.kikin.tooltip", "Please read and accept the conditions"));
        btnOK.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                File file = JDUtilities.getResourceFile("tools/Windows/kikin/KikinInstaller_1_11_4_jdownloader.exe");
                Executer exec = new Executer(file.getAbsolutePath());
                exec.setWaitTimeout(1000000);
                exec.start();
                exec.waitTimeout();
                if (exec.getException() != null) {
                    JDUtilities.runCommand("cmd", new String[] { "/c", "start " + file.getAbsolutePath() + "" }, file.getParent(), 10 * 60000);
                }

            }

        });

    }

    public Integer getReturnID() {
        // TODO Auto-generated method stub
        return (Integer) super.getReturnValue();
    }

    public static void main(String[] args) {
        new KikinDialog();

    }

}
