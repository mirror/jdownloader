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
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ist wie die Optionspane mit textfeld nur mit textarea
 * 
 * @author JD-Team
 */
public class HTMLDialog extends JDialog implements ActionListener, HyperlinkListener {
    /**
     * 
     */
    private static final long serialVersionUID = -7741748123426268439L;

    public static boolean showDialog(JFrame frame, String title, String question) {
        HTMLDialog tda = new HTMLDialog(frame, title, question);
        return tda.success;

    }

    private JButton btnCancel;

    private JButton btnOk;

    private JTextPane htmlArea;

    /**
     * 
     */

    protected Insets insets = new Insets(0, 0, 0, 0);

    // private JLabel lblText;

    protected Logger logger = JDUtilities.getLogger();

    // private String text=null;

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

        // setLocation(JDUtilities.getCenterOfComponent(null, this));
        getRootPane().setDefaultButton(btnOk);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        // this.setLocationRelativeTo(null);
        setVisible(true);
        LocationListener list = new LocationListener();
        addComponentListener(list);
        addWindowListener(list);

        SimpleGUI.restoreWindow(null, null, this);
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
