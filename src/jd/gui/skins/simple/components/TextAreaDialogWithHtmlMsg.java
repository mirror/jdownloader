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

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.gui.skins.simple.Link.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ist wie die Optionspane mit textfeld nur mit textarea
 * 
 * @author JD-Team
 */
public class TextAreaDialogWithHtmlMsg extends JDialog implements ActionListener, HyperlinkListener {

    private static final long serialVersionUID = -655039113948925165L;

    public static String showDialog(JFrame frame, String title, String question, String def) {
        TextAreaDialogWithHtmlMsg tda = new TextAreaDialogWithHtmlMsg(frame, title, question, def);
        return tda.getText();
    }

    private JButton btnOk;

    private JButton btnCancel;

    private String text = null;

    private JTextPane textArea;

    private TextAreaDialogWithHtmlMsg(JFrame frame, String title, String question, String def) {
        super(frame);

        this.setLayout(new BorderLayout());
        this.setName(title);
        this.setTitle(title);

        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Cancel"));
        btnCancel.addActionListener(this);

        btnOk = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnOk.addActionListener(this);

        textArea = new JTextPane();
        textArea.setEditable(true);
        textArea.requestFocusInWindow();

        JScrollPane scrollPane = new JScrollPane(textArea);
        this.add(scrollPane, BorderLayout.CENTER);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setPreferredSize(new Dimension((int) (screenSize.width * 0.9), (int) (screenSize.height * 0.9)));

        if (question != null) {
            JTextPane msg = new JTextPane();
            msg.setEditable(false);
            msg.setContentType("text/html");
            msg.setText(question);
            msg.addHyperlinkListener(this);
            this.add(msg, BorderLayout.NORTH);
        }
        if (def != null) textArea.setText(def);

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
            text = textArea.getText();
        }
        dispose();
    }

    private String getText() {
        return text;
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JLinkButton.openURL(e.getURL());
        }
    }

}
