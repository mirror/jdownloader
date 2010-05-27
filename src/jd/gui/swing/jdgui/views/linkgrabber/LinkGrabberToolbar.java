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
        super("action.linkgrabber.addall", "action.addurl", "action.load", "action.linkgrabber.clearlist");
    }

    @Override
    protected String getColConstraints(String[] list) {
        return "3[left]push[]5[]push[right]3";
    }

    @Override
    public String getButtonConstraint(int i, ToolBarAction action) {
        return "sizegroup toolbar";
    }

}
