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

package jd.gui.swing.jdgui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.maintab.ClosableTabHeader;
import jd.gui.swing.jdgui.views.ClosableView;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.event.GUIEvent;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;

public class MainTabbedPane extends JTabbedPane {

    private static final long     serialVersionUID = -1531827591735215594L;
    private static MainTabbedPane INSTANCE;
    protected View                latestSelection;

    public synchronized static MainTabbedPane getInstance() {
        if (INSTANCE == null) INSTANCE = new MainTabbedPane();
        return INSTANCE;
    }

    /**
     * Use {@link MainTabbedPane#remove(View)}!
     */
    @Override
    public void remove(Component component) {
        throw new RuntimeException("This method is not allowed!");
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        boolean ret = super.processKeyBinding(ks, e, condition, pressed);
        if (getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ks) != null) return false;
        if (getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(ks) != null) return false;
        return ret;
    }

    public void remove(View view) {
        if (!this.contains(view)) return;
        boolean selected = (getSelectedView() == view);
        super.remove(view);
        if (view != null) view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_REMOVE));
        if (selected && getTabCount() > 0) setSelectedComponent(getComponentAt(0));
    }

    public void addTab(View view) {
        if (this.contains(view)) return;
        if (view instanceof ClosableView) {
            addClosableTab((ClosableView) view);
        } else {
            super.addTab(view.getTitle(), view.getIcon(), view, view.getTooltip());
            view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_ADD));
            this.setFocusable(false);
        }
    }

    private void addClosableTab(ClosableView view) {

        super.addTab(view.getTitle(), view.getIcon(), view, view.getTooltip());
        view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_ADD));
        this.setTabComponentAt(this.getTabCount() - 1, new ClosableTabHeader(view));

        this.setFocusable(false);

    }

    private MainTabbedPane() {
        this.setMinimumSize(new Dimension(300, 100));
        this.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        this.setOpaque(false);

        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (JDGui.getInstance() != null) JDGui.getInstance().setWaiting(true);
                try {
                    View comp = (View) getSelectedComponent();
                    if (comp == latestSelection) return;
                    if (latestSelection != null) {
                        latestSelection.setHidden();
                    }
                    GUIEventSender.getInstance().fireEvent(new GUIEvent(MainTabbedPane.this, GUIEvent.Type.TAB_SWITCH, latestSelection, comp));
                    latestSelection = comp;
                    comp.setShown();
                    revalidate();

                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }

        });
    }

    public void notifyCurrentTab() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                View comp = (View) getSelectedComponent();
                GUIEventSender.getInstance().fireEvent(new GUIEvent(MainTabbedPane.this, GUIEvent.Type.TAB_SWITCH, latestSelection, comp));
            }
        };
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (JDGui.getInstance() != null) JDGui.getInstance().setWaiting(false);
    }

    /**
     * gets called form the main frame if it gets closed
     */
    public void onClose() {
        getSelectedView().setHidden();
    }

    /**
     * returns the currently selected View
     */
    public View getSelectedView() {
        return (View) super.getSelectedComponent();
    }

    @Override
    public void setSelectedComponent(Component e) {
        super.setSelectedComponent(getComponentEquals((View) e));
    }

    /**
     * returns the component in this tab that equals view
     * 
     * @param view
     * @return
     */
    public View getComponentEquals(View view) {
        for (int i = 0; i < this.getTabCount(); i++) {
            Component c = this.getComponentAt(i);
            if (c.equals(view)) return (View) c;
        }
        return null;
    }

    /**
     * CHecks if there is already a tabbepanel of this type in this pane.
     * 
     * @param view
     * @return
     */
    public boolean contains(View view) {
        for (int i = 0; i < this.getTabCount(); i++) {
            Component c = this.getComponentAt(i);
            if (c.equals(view)) return true;
        }
        return false;
    }

    public boolean isLinkgrabberView() {
        return getSelectedView() instanceof LinkGrabberView;
    }

    public boolean isDownloadView() {
        return getSelectedView() instanceof DownloadsView;
    }

    // public void mouseClicked(MouseEvent e) {
    // try {
    // int tabNumber = getUI().tabForCoordinate(this, e.getX(), e.getY());
    // if (tabNumber < 0) return;
    // Rectangle rect = ((CloseTabIcon) getIconAt(tabNumber)).getBounds();
    // if (rect.contains(e.getX(), e.getY())) {
    // // the tab is being closed
    // ((ClosableView) this.getComponentAt(tabNumber)).close();
    // }
    // } catch (ClassCastException e2) {
    // }
    // }
    //
    // public void mouseEntered(MouseEvent e) {
    // }
    //
    // public void mouseExited(MouseEvent e) {
    // }
    //
    // public void mousePressed(MouseEvent e) {
    // }
    //
    // public void mouseReleased(MouseEvent e) {
    // }

}
