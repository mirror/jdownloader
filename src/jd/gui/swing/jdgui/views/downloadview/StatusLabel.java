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

package jd.gui.swing.jdgui.views.downloadview;

import javax.swing.Icon;
import javax.swing.JPanel;

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
    private boolean enabled[] = new boolean[ICONCOUNT];

    public StatusLabel() {
        super(new MigLayout("ins 0", "[]0[fill,grow,align right]"));

        add(left = new JRendererLabel());

        for (int i = 0; i < ICONCOUNT; i++) {
            add(rights[i] = new JRendererLabel(), "dock east");
            rights[i].setOpaque(false);
            enabled[i] = true;
        }

        left.setOpaque(false);
        this.setOpaque(true);
    }

    public void setText(String text) {
        left.setText(text);
    }

    public void setIcon(int i, Icon icon) {
        if (i < ICONCOUNT) rights[i].setIcon(icon);
        if (i < ICONCOUNT) enabled[i] = true;
    }

    public void setIcon(int i, Icon icon, boolean enabled) {
        if (i < ICONCOUNT) rights[i].setIcon(icon);
        if (i < ICONCOUNT) rights[i].setEnabled(enabled);
        if (i < ICONCOUNT) this.enabled[i] = enabled;
    }

    public void setEnabled(boolean b) {
        for (int i = 0; i < ICONCOUNT; i++) {
            if (!enabled[i]) {
                rights[i].setEnabled(false);
            } else {
                rights[i].setEnabled(b);
            }
        }
    }

    /**
     * Remember, that its always the same panel instance. so we have to reset to
     * defaults before each cellrenderer call.
     * 
     * @param counter
     */
    public void clearIcons(int counter) {
        for (int i = counter; i < ICONCOUNT; i++) {
            rights[i].setIcon(null);
        }
    }

}
