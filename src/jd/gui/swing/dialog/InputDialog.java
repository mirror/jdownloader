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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;

import jd.gui.UserIO;
import jd.nutils.JDFlags;
import net.miginfocom.swing.MigLayout;

public class InputDialog extends AbstractDialog implements KeyListener, MouseListener {

    private static final long serialVersionUID = 9206575398715006581L;
    private String defaultMessage;
    private String message;
    private JTextPane messageArea;
    private JTextComponent input;

    public InputDialog(int flag, String title, String message, String defaultMessage, ImageIcon icon, String okOption, String cancelOption) {
        super(flag, title, icon, okOption, cancelOption);

        this.defaultMessage = defaultMessage;
        this.message = message;

        init();
    }

    @Override
    public JComponent contentInit() {
        JPanel contentpane = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]"));
        messageArea = new JTextPane();
        messageArea.setBorder(null);
        messageArea.setBackground(null);
        messageArea.setOpaque(false);
        messageArea.setText(this.message);
        messageArea.setEditable(false);
        messageArea.putClientProperty("Synthetica.opaque", Boolean.FALSE);

        contentpane.add(messageArea);
        if (JDFlags.hasAllFlags(flag, UserIO.STYLE_LARGE)) {
            input = new JTextPane();
            input.setText(this.defaultMessage);
            input.addKeyListener(this);
            input.addMouseListener(this);
            contentpane.add(new JScrollPane(input), "height 20:60:n,pushy,growy,w 450");
        } else if (JDFlags.hasAllFlags(flag, UserIO.STYLE_PASSWORD)) {
            input = new JPasswordField();
            input.setBorder(BorderFactory.createEtchedBorder());
            input.setText(this.defaultMessage);
            input.addKeyListener(this);
            input.addMouseListener(this);
            contentpane.add(input, "pushy,growy,w 450");
        } else {
            input = new JTextField();
            input.setBorder(BorderFactory.createEtchedBorder());
            input.setText(this.defaultMessage);
            input.addKeyListener(this);
            input.addMouseListener(this);
            contentpane.add(input, "pushy,growy,w 450");
        }

        return contentpane;
    }

    @Override
    protected void packed() {
        input.selectAll();
        requestFocus();
        input.requestFocusInWindow();
    }

    public String getReturnID() {
        if ((this.getReturnValue() & (UserIO.RETURN_OK | UserIO.RETURN_COUNTDOWN_TIMEOUT)) == 0) { return null; }
        if (input.getText() == null || input.getText().equals("")) return null;
        return input.getText();
    }

    public void keyPressed(KeyEvent e) {
        this.interrupt();
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        this.interrupt();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}
