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

package jd.gui.swing.jdgui.interfaces;

import org.appwork.utils.event.DefaultIntEvent;

/**
 * 
 * @author Coalado
 * 
 */
public class SwitchPanelEvent extends DefaultIntEvent {

    /**
     * panel is noe visible on screen
     */
    public static final int ON_SHOW   = 0;
    /**
     * Panel is not visible any more
     */
    public static final int ON_HIDE   = 1;
    /**
     * panel has been added to the gui. this does NOT mean that it is visible.
     * e.g. it may be a tab of a tabbedpane, but not the selected one
     */
    public static final int ON_ADD    = 2;

    /**
     * Panel has been removed from gui. see SwitchPanelEvent.ON_ADD
     */
    public static final int ON_REMOVE = 3;

    public SwitchPanelEvent(Object source, int ID, Object parameter) {
        super(source, ID, parameter);

    }

    public SwitchPanelEvent(Object source, int ID) {
        this(source, ID, null);
    }

}
