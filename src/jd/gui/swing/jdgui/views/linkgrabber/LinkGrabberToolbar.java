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

package jd.gui.swing.jdgui.views.linkgrabber;

import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.views.ViewToolbar;

public class LinkGrabberToolbar extends ViewToolbar {

    private static final long serialVersionUID = 1L;

    public LinkGrabberToolbar() {
        super("action.addurl", "action.load", "action.linkgrabber.addall", "action.linkgrabber.clearlist");
    }

    @Override
    public String getButtonConstraint(int i, ToolBarAction action) {
        if (i < 3) {
            return "dock west, sizegroup toolbar, gapright " + (i == 1 ? "10" : "5");
        } else {
            return "dock east, sizegroup toolbar, gapright 3";
        }
    }

}
