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

package jd.gui.swing.jdgui.components;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JPanel;

import jd.nutils.JDImage;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.renderer.JRendererLabel;

/**
 * A Renderercomponent, that supports multiple JLabels in one cell. TODO: ignore
 * setter if nothing to change e.g. (setIcon(xy)) should do nothing if xy is
 * already set.
 * 
 * @author coalado
 */
public class StatusLabel extends JPanel {

    private static final long serialVersionUID = -378709535509849986L;
    public static final int ICONCOUNT = 5;
    private JRendererLabel left;
    private JRendererLabel[] rights = new JRendererLabel[ICONCOUNT];

    public StatusLabel() {
        super(new MigLayout("ins 0", "[]0[fill,grow,align right]"));
        add(left = new JRendererLabel());
        left.setOpaque(false);
        for (int i = 0; i < ICONCOUNT; i++) {
            add(rights[i] = new JRendererLabel(), "dock east");
            rights[i].setOpaque(false);
        }
        this.setOpaque(true);
    }

    /**
     * clears the icon for left, setIcon AFTER setText
     */
    public void setText(String text, Icon icon) {
        left.setIcon(icon);
        left.setText(text);
        left.setToolTipText(text);
    }

    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        if (left == null) return;
        left.setForeground(fg);
        for (int i = 0; i < rights.length; i++) {
            rights[i].setForeground(fg);
        }
    }

    public void setIcon(int i, Icon icon, String text, String tooltip) {
        if (i < 0 && ICONCOUNT > 0) {
            left.setIcon(icon);
            // left.setText(text);
            left.setToolTipText(tooltip);
        } else {
            if (i < 0 || i >= ICONCOUNT) return;
            rights[i].setIcon(icon);
            rights[i].setText(text);
            rights[i].setToolTipText(tooltip);
        }
    }

    @Override
    public void setEnabled(boolean b) {
        if (left == null) return;

        if (b == false) left.setDisabledIcon(JDImage.getDisabledIcon(left.getIcon()));
        left.setEnabled(b);
        for (int i = 0; i < ICONCOUNT; i++) {
            if (b == false) rights[i].setDisabledIcon(JDImage.getDisabledIcon(rights[i].getIcon()));
            rights[i].setEnabled(b);
        }
    }

    /**
     * Remember, that its always the same panel instance. so we have to reset to
     * defaults before each cellrenderer call.
     */
    public void clearIcons(int counter) {
        for (int i = counter; i < ICONCOUNT; i++) {
            rights[i].setIcon(null);
            rights[i].setText(null);
            rights[i].setToolTipText(null);
        }
    }

    @Override
    public String getToolTipText() {
        StringBuilder sb = new StringBuilder();
        if (left.getToolTipText() != null) {
            sb.append(left.getToolTipText());
        }
        for (int i = rights.length - 1; i >= 0; --i) {
            if (rights[i].getToolTipText() != null) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(rights[i].getToolTipText());
            }
        }
        if (sb.length() > 0) return sb.toString();
        return null;
    }

}
