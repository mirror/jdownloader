package jd.gui.skins.simple.components;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Map;

import javax.swing.JComponent;

public class JDUnderlinedText extends MouseAdapter {

    private Font originalFont;

    @SuppressWarnings("unchecked")
    //@Override
    public void mouseEntered(MouseEvent evt) {
        if (!(evt.getSource() instanceof JComponent)) return;
        JComponent src = (JComponent) evt.getSource();

        originalFont = src.getFont();
        if (src.isEnabled()) {
            Map attributes = originalFont.getAttributes();
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            src.setFont(originalFont.deriveFont(attributes));
        }

    }

    //@Override
    public void mouseExited(MouseEvent evt) {
        if (!(evt.getSource() instanceof JComponent)) return;
        JComponent src = (JComponent) evt.getSource();
        src.setFont(originalFont);
    }

}
