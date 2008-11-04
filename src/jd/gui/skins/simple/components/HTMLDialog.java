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

package jd.gui.skins.simple.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

/**
 * Diese Klasse ist wie die Optionspane mit textfeld nur mit textarea
 * 
 * @author JD-Team
 */
public class HTMLDialog extends JDialog implements ActionListener, HyperlinkListener {

    private static final long serialVersionUID = -7741748123426268439L;

    public static boolean showDialog(JFrame frame, String title, String question) {
        HTMLDialog tda = new HTMLDialog(frame, title, question);

        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        int minWidth = Math.min(tda.getPreferredSize().width, (int) (size.width * .75));
        int minHeight = Math.min(tda.getPreferredSize().height, (int) (size.height * .75));
        tda.setPreferredSize(new Dimension(Math.max(minWidth, 640), Math.max(minHeight, 480)));
        tda.pack();

        return tda.success;
    }

    private JButton btnOk;

    private JButton btnCancel;

    private boolean success = false;

    private HTMLDialog(JFrame frame, String title, String html) {
        super(frame);

        this.setLayout(new BorderLayout());
        this.setName(title);
        this.setTitle(title);

        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Cancel"));
        btnCancel.addActionListener(this);

        btnOk = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnOk.addActionListener(this);

        JTextPane htmlArea = new JTextPane();
        htmlArea.setEditable(false);
        htmlArea.setContentType("text/html");
        htmlArea.setText(html);
        htmlArea.requestFocusInWindow();
        htmlArea.addHyperlinkListener(this);

        JScrollPane scrollPane = new JScrollPane(htmlArea);
        this.add(scrollPane, BorderLayout.CENTER);

        JPanel p = new JPanel();
        p.add(btnOk);
        p.add(btnCancel);
        this.add(p, BorderLayout.SOUTH);

        this.pack();

        this.getRootPane().setDefaultButton(btnOk);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setVisible(true);

        this.setLocation(JDUtilities.getCenterOfComponent(frame, this));
        this.setVisible(false);
        this.setModal(true);
        this.setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOk) {
            success = true;
        }
        dispose();
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                JLinkButton.openURL(e.getURL());
            } catch (BrowserLaunchingInitializingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (UnsupportedOperatingSystemException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

}
