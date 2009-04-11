package jd.gui.skins.simple.tasks;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;

public class CollapseButton extends JPanel {

    private JButton button;
    private JXCollapsiblePane collapsible;

    public CollapseButton(String host, ImageIcon ii) {
        this.setLayout(new MigLayout("ins 0,wrap 1, gap 0 0","grow,fill"));
        button = createButton(host, ii);
        add(button);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (collapsible.isCollapsed()) {
                    collapsible.setCollapsed(false);
                } else {
                  //  collapsible.setCollapsed(true);
                }

            }

        });
        collapsible = new JXCollapsiblePane();
        collapsible.setCollapsed(true);
        
        collapsible.setLayout(new MigLayout("ins 0,wrap 1,gap 0 0","grow,fill"));
        add(collapsible);

    }
public void setCollapsed(boolean b){
    collapsible.setCollapsed(b);
}
    public Container getContentPane() {
        return collapsible.getContentPane();
    }

    public JButton createButton(String string, Icon i) {
        JButton bt;

        if (i == null) {
            bt = new JButton(string);
        } else {
            bt = new JButton(string, i);
        }

        bt.setContentAreaFilled(false);
        bt.setCursor(Cursor.getPredefinedCursor(12));
        bt.setFocusPainted(false);
        bt.setBorderPainted(false);
        bt.setHorizontalAlignment(JButton.LEFT);
        // bt.addActionListener(this);
        bt.addMouseListener(new MouseAdapter() {

            private Font originalFont;

            @SuppressWarnings("unchecked")
            @Override
            public void mouseEntered(MouseEvent evt) {
                JButton src = (JButton) evt.getSource();

                originalFont = src.getFont();
                if (src.isEnabled()) {
                    Map attributes = originalFont.getAttributes();
                    attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    src.setFont(originalFont.deriveFont(attributes));
                }

            }

            @Override
            public void mouseExited(MouseEvent evt) {
                JButton src = (JButton) evt.getSource();
                src.setFont(originalFont);
            }
        });
        return bt;
    }

    public JButton getButton() {
     return button;
        
    }

}
