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

package jd.gui.swing.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.controlling.ClipboardHandler;
import jd.gui.UserIO;
import jd.gui.swing.Factory;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.userio.DummyFrame;
import jd.nutils.JDImage;
import jd.nutils.Screen;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.update.JDUpdater;

public class AboutDialog extends JDialog {

    private static final long serialVersionUID = -7647771640756844691L;

    public AboutDialog() {
        super(DummyFrame.getDialogParent());

        JLabel lbl = new JLabel("JDownloader");
        lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getSize() * 2.0f));

        String branch = null;
        String version = null;
        try {
            branch = JDUpdater.getInstance().getBranch().getName();
            version = "JDownloader -" + branch + "- Build " + JDUtilities.getRevision();
        } catch (Throwable e2) {
            version = "JDownloader Build " + JDUtilities.getRevision();
        }

        JPanel links = new JPanel(new MigLayout("ins 0", "[]push[]push[]push[]"));
        try {
            JButton btn = Factory.createButton(_GUI._.jd_gui_swing_components_AboutDialog_license(), NewTheme.I().getIcon("premium", 16), new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    String license = JDIO.readFileToString(JDUtilities.getResourceFile("licenses/jdownloader.license"));
                    UserIO.getInstance().requestMessageDialog(UserIO.STYLE_LARGE | UserIO.NO_ICON | UserIO.NO_COUNTDOWN, _GUI._.jd_gui_swing_components_AboutDialog_license_title(), license);
                }

            });
            btn.setBorder(null);

            links.add(btn);
            links.add(new JLink(_GUI._.jd_gui_swing_components_AboutDialog_homepage(), NewTheme.I().getIcon("url", 16), new URL("http://www.jdownloader.org/home?lng=en")));
            links.add(new JLink(_GUI._.jd_gui_swing_components_AboutDialog_forum(), NewTheme.I().getIcon("board", 16), new URL("http://board.jdownloader.org")));
            links.add(new JLink(_GUI._.jd_gui_swing_components_AboutDialog_contributers(), NewTheme.I().getIcon("contributer", 16), new URL("http://jdownloader.org/knowledge/wiki/contributers")));
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(version).append("\r\n");
        sb.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\r\n");
        sb.append("Java Version: ").append(System.getProperty("java.version"));

        final String info = sb.toString();
        JButton btn = Factory.createButton(_GUI._.jd_gui_swing_components_AboutDialog_copy(), NewTheme.I().getIcon("copy", 16), new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                ClipboardHandler.getClipboard().copyTextToClipboard(info);
            }

        });
        btn.setBorder(null);
        btn.setVerticalTextPosition(JButton.BOTTOM);
        btn.setHorizontalTextPosition(JButton.CENTER);

        this.setLayout(new MigLayout("ins 10, wrap 3", "[]15[]push[right]"));
        this.add(new JLabel(JDImage.getImageIcon("logo/jd_logo_128_128")), "aligny center, spany 6");
        this.add(lbl, "spanx");
        this.add(new JLabel("© AppWork GmbH 2007-2011"), "spanx");
        this.add(new JLabel(version), "gaptop 10");
        this.add(btn, "aligny center, spany 3");
        this.add(new JLabel("JRE Vendor: " + System.getProperty("java.vendor")));
        this.add(new JLabel("JRE Version: " + System.getProperty("java.version")));
        this.add(new JLabel("Synthetica License Registration Number (#289416475)"), "gaptop 10, spanx");
        this.add(links, "gaptop 15, growx, pushx, spanx");
        this.pack();

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setTitle(_GUI._.jd_gui_swing_components_AboutDialog_title());
        this.setResizable(false);
        this.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), this));

        /*
         * Fixes Always-on-Top-Bug in windows. Bugdesc: found in svn
         */
        DummyFrame.getDialogParent().setAlwaysOnTop(true);
        DummyFrame.getDialogParent().setAlwaysOnTop(false);

        this.setVisible(true);
    }

}