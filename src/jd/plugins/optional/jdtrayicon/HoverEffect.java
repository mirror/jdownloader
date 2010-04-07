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

package jd.plugins.optional.jdtrayicon;

import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;

import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;

public class HoverEffect extends JDMouseAdapter {

    private final AbstractButton comp;

    public HoverEffect(AbstractButton comp) {
        this.comp = comp;
    }

    @Override
    public void mouseEntered(MouseEvent evt) {
        comp.setOpaque(true);
        comp.setContentAreaFilled(true);
        comp.setBorderPainted(true);
    }

    @Override
    public void mouseExited(MouseEvent evt) {
        comp.setOpaque(false);
        comp.setContentAreaFilled(false);
        comp.setBorderPainted(false);
    }

}
