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

package jd.utils;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.parser.Regex;

public class CheckJava {
    public class HTMLDialog extends JDialog implements ActionListener, HyperlinkListener {
        private static final long serialVersionUID = -7741748123426268439L;
        private JButton btnCancel;
        private JButton btnOk;
        private JTextPane htmlArea;
        protected Insets insets = new Insets(0, 0, 0, 0);
        protected Logger logger = JDUtilities.getLogger();
        private JScrollPane scrollPane;
        private boolean success = false;

        private HTMLDialog(JFrame frame, String title, String html) {
            super(frame);
            setLayout(new BorderLayout());
            setName(title);
            btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Cancel"));
            btnCancel.addActionListener(this);
            btnOk = new JButton(JDLocale.L("gui.btn_ok", "OK"));
            btnOk.addActionListener(this);
            setTitle(title);
            htmlArea = new JTextPane();
            scrollPane = new JScrollPane(htmlArea);
            htmlArea.setEditable(false);
            htmlArea.setContentType("text/html");
            htmlArea.setText(html);
            htmlArea.requestFocusInWindow();
            htmlArea.addHyperlinkListener(this);

            this.add(scrollPane, BorderLayout.CENTER);
            JPanel p = new JPanel();
            p.add(btnOk);
            p.add(btnCancel);
            this.add(p, BorderLayout.SOUTH);

            pack();
            getRootPane().setDefaultButton(btnOk);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setVisible(true);
            LocationListener list = new LocationListener();
            addComponentListener(list);
            addWindowListener(list);
            setVisible(false);
            setModal(true);
            setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnOk) {
                success = true;
                dispose();
            } else {
                dispose();
            }
        }

        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                JLinkButton.openURL(e.getURL());
            }
        }
    }

    public boolean check() {
        String runtimeName = System.getProperty("java.runtime.name").toLowerCase();
        String runtimeVersion = System.getProperty("java.runtime.version").toLowerCase();

        if (new Regex(runtimeVersion, "1\\.5").count() < 0 || new Regex(runtimeVersion, "1\\.6").count() < 0 || new Regex(runtimeName, "IcedTea").count() > 0) {
            String html = String.format(JDLocale.L("gui.javacheck.html", "<link href='http://jdownloader.org/jdcss.css' rel='stylesheet' type='text/css' /><div style='width:534px;height;200px'><h2>You useses a wrong Java version. Please use a original Sun Java. Start jDownloader anyway?<table width='100%%'><tr><th colspan='2'>Your Java Version:</th></tr><tr><th>Runtime Name</th><td>%s</td></tr><tr><th>Runtime Version</th><td>%s</td></tr></table></div>"), runtimeName, runtimeVersion);
            HTMLDialog tda = new HTMLDialog(null, JDLocale.L("gui.javacheck.title", "Wrong Java Version"), html);
            return tda.success;
        }

        return true;
    }
}
