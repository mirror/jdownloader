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

import java.awt.Component;
import java.awt.LayoutManager;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.appwork.utils.event.Eventsender;

/**
 * a panel which gets informed if it gets displayed or removed from display
 * 
 * @author Coalado
 * 
 */
public abstract class SwitchPanel extends JPanel {

    private static final long                                  serialVersionUID = -7856570342778191232L;
    private boolean                                            currentlyVisible = false;
    private Eventsender<SwitchPanelListener, SwitchPanelEvent> broadcaster;

    public SwitchPanel(LayoutManager layout) {
        super(layout);
        initBroadcaster();

    }

    public SwitchPanel() {
        super();

        initBroadcaster();
    }

    private void initBroadcaster() {
        broadcaster = new Eventsender<SwitchPanelListener, SwitchPanelEvent>() {

            @Override
            protected void fireEvent(SwitchPanelListener listener, SwitchPanelEvent event) {
                listener.onPanelEvent(event);
            }

        };
    }

    /**
     * returns the panels eventbroadcaster
     * 
     * @return
     */
    public Eventsender<SwitchPanelListener, SwitchPanelEvent> getBroadcaster() {
        return broadcaster;
    }

    /**
     * DO NEVER call this method directly. This is a callback
     */
    abstract protected void onShow();

    /**
     * DO NEVER call this method directly. This is a callback
     */
    abstract protected void onHide();

    /**
     * invokes the view chain of this panel. all nestes views get informed, too
     */
    public void setShown() {
        if (currentlyVisible) return;
        this.currentlyVisible = true;
        onShow();
        broadcaster.fireEvent(new SwitchPanelEvent(this, SwitchPanelEvent.ON_SHOW));
        distributeView(this);
    }

    /**
     * invokes the view chain of this panel. all nestes views get informed, too
     */
    public void setHidden() {
        if (!currentlyVisible) return;
        this.currentlyVisible = false;
        onHide();
        broadcaster.fireEvent(new SwitchPanelEvent(this, SwitchPanelEvent.ON_HIDE));
        distributeHide(this);
    }

    protected void distributeView(JComponent switchPanel) {
        for (Component comp : switchPanel.getComponents()) {
            if (!(comp instanceof JComponent)) continue;
            if (comp == switchPanel) continue;
            if (comp instanceof SwitchPanel) {
                ((SwitchPanel) comp).setShown();
            } else {
                distributeView((JComponent) comp);
            }
        }
    }

    protected void distributeHide(JComponent switchPanel) {
        for (Component comp : switchPanel.getComponents()) {
            if (!(comp instanceof JComponent)) continue;
            if (comp == switchPanel) continue;
            if (comp instanceof SwitchPanel) {
                ((SwitchPanel) comp).setHidden();
            } else {
                distributeHide((JComponent) comp);
            }
        }
    }

    /**
     * returns if the panel is currently visible on screen
     * 
     * @return
     */
    public boolean isShown() {
        return currentlyVisible;
    }

}
