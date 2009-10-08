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

package jd.gui.swing.jdgui.views;

import javax.swing.Icon;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.View;

/**
 * A Wrapper for compatibility to old TabbedPanels
 * 
 * @author Coalado
 * 
 */
public class TabbedPanelView extends View {

    private static final long serialVersionUID = 5496287547880139678L;
    private SwitchPanel tabbedPanel;

    public TabbedPanelView(SwitchPanel tabbedPanel) {
        super();
        this.tabbedPanel = tabbedPanel;
        this.setContent(tabbedPanel);

    }

    @Override
    public boolean equals(Object e) {
   
        if (!(e instanceof TabbedPanelView)) return false;

        return ((TabbedPanelView) e).tabbedPanel == this.tabbedPanel;
     
    }

    @Override
    public Icon getIcon() {
       
        return View.getDefaultIcon();
        
    }

    @Override
    public String getTitle() {
        return tabbedPanel.getName();
    }

    @Override
    public String getTooltip() {
        return tabbedPanel.getToolTipText();
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

}
