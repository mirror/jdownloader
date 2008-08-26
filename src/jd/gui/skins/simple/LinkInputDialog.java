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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Mit dieser Klasse werden in einem Dialog Links entgegengenommen
 * 
 * @author eXecuTe
 */
public class LinkInputDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -5108880482000959849L;

    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();

    private JButton btnCancel;

    private JButton btnOK;

    private String linksString = null;

    private JTextArea textArea;

    /**
     * Zeigt einen neuen LinkInputDialog und gibt die eingetragenen Links bzw.
     * null bei Abbruch zurück.
     * 
     * @param owner
     *            Das übergeordnete Fenster
     * @param clipboard
     *            Text in der Zwischenablage
     */
    public static String showDialog(JFrame owner, String clipboard) {
        LinkInputDialog lid = new LinkInputDialog(owner, clipboard);
        return lid.getText();
    }

    private LinkInputDialog(JFrame owner, String clipboard) {
        super(owner);

        this.setModal(true);
        this.setTitle(JDLocale.L("gui.menu.action.add.name", "Links hinzufügen"));

        btnOK = new JButton(JDLocale.L("gui.btn_add", "Hinzufügen"));
        btnOK.addActionListener(this);
        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);

        textArea = new JTextArea(10, 30);
        textArea.setEditable(true);
        textArea.setText(clipboard);
        textArea.setCaretPosition(0);

        JScrollPane textScrollPane = new JScrollPane(textArea);
        textScrollPane.setAutoscrolls(true);

        int n = 10;
        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER, n, 0));
        bpanel.add(btnOK);
        bpanel.add(btnCancel);

        JPanel panel = new JPanel(new BorderLayout(n, n));
        panel.setBorder(new EmptyBorder(n, n, n, n));
        panel.add(bpanel, BorderLayout.SOUTH);
        panel.add(textScrollPane, BorderLayout.CENTER);

        this.setContentPane(panel);
        this.getRootPane().setDefaultButton(btnOK);
        this.setPreferredSize(new Dimension(400, 300));
        this.pack();
        this.setLocation(JDUtilities.getCenterOfComponent(owner, this));
        this.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            linksString = textArea.getText();
        }

        this.dispose();
    }

    private String getText() {
        return linksString;
    }

}
