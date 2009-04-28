package jd.gui.userio.dialog;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import jd.gui.UserIO;
import jd.nutils.JDFlags;

public class InputDialog extends AbstractDialog implements KeyListener, MouseListener {

    private String defaultMessage;
    private String message;
    private JTextPane messageArea;
    private JTextPane input;

    public InputDialog(int flag, String title, String message, String defaultMessage, ImageIcon icon, String okOption, String cancelOption) {
        super(flag, title, icon, okOption, cancelOption);
        this.defaultMessage = defaultMessage;
        this.message = message;
        init();
    }

    /**
     * 
     */
    private static final long serialVersionUID = 9206575398715006581L;

    @Override
    public void contentInit(JPanel contentpane) {
        messageArea = new JTextPane();
        messageArea.setBorder(null);
        messageArea.setBackground(null);
        messageArea.setOpaque(false);
        messageArea.setText(this.message);
        messageArea.setEditable(false);
        contentpane.add(messageArea);
        if (JDFlags.hasAllFlags(flag, UserIO.STYLE_LARGE)) {
            input = new JTextPane();

            input.setText(this.defaultMessage);
            input.addKeyListener(this);
            input.addMouseListener(this);

            contentpane.add(new JScrollPane(input), "height 20:60:n,pushy,growy");
        } else {
            input = new JTextPane();
            input.setBorder(BorderFactory.createEtchedBorder());
            input.setText(this.defaultMessage);
            input.addKeyListener(this);
            input.addMouseListener(this);

            contentpane.add(input, "pushy,growy");
        }

    }

    protected void packed() {
        input.selectAll();
        requestFocus();
        input.requestFocusInWindow();

    }

    public String getReturnID() {
        if ((this.getReturnValue() & UserIO.RETURN_OK) == 0) { return null; }
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
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }

}
