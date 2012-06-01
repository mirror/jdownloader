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

package jd.gui.swing.components;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Map;

import javax.swing.JComponent;

import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;

public class JDUnderlinedText extends JDMouseAdapter {

    private final JComponent comp;

    private Font originalFont;

    public JDUnderlinedText(JComponent comp) {
        this.comp = comp;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void mouseEntered(MouseEvent evt) {
        originalFont = comp.getFont();
        if (comp.isEnabled()) {
            Map attributes = originalFont.getAttributes();
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            comp.setFont(originalFont.deriveFont(attributes));
        }
    }

    @Override
    public void mouseExited(MouseEvent evt) {
        comp.setFont(originalFont);
    }

}
