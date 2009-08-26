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
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.maintab.ChangeHeader;
import jd.utils.JDUtilities;

public class MainTabbedPane extends JTabbedPane {

    private static final long serialVersionUID = -1531827591735215594L;
    private static MainTabbedPane INSTANCE;

    public synchronized static MainTabbedPane getInstance() {
        if (INSTANCE == null) INSTANCE = new MainTabbedPane();
        return INSTANCE;
    }

    @Override
    public void remove(Component component) {
        throw new RuntimeException(" This method is not allowed");
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        boolean ret = super.processKeyBinding(ks, e, condition, pressed);

        if (getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ks) != null) { return false; }
        if (getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(ks) != null) { return false; }
        return ret;
    }

    public void remove(View view) {

        super.remove(view);
        view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_REMOVE));
        if (getTabCount() > 0) setSelectedComponent(getComponentAt(0));
    }

    protected View latestSelection;

    /**
     * sets a * in the tab to show that the tab contains changes (has to be
     * saved)
     * 
     * @since 1.6
     * @param b
     * @param index
     *            if index <0 the selected tab is used
     */
    public void setChanged(boolean b, int index) {
        if (JDUtilities.getJavaVersion() < 1.6) return;
        if (index < 0) index = this.getSelectedIndex();
        ((ChangeHeader) this.getTabComponentAt(index)).setChanged(b);
    }

    /**
     * Sets an close Action to a tab.
     * 
     * @param a
     *            Action. if a == null the close button dissapears
     * @param index
     *            if index <0 the selected tab is used
     * @since 1.6
     */
    public void setClosableAction(Action a, int index) {
        if (JDUtilities.getJavaVersion() < 1.6) return;
        if (index < 0) index = this.getSelectedIndex();
        ((ChangeHeader) this.getTabComponentAt(index)).setCloseEnabled(a);
    }

    public void addTab(View view) {
        SwingGui.checkEDT();
        super.addTab(view.getTitle(), view.getIcon(), view, view.getTooltip());

        view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_ADD));
        if (JDUtilities.getJavaVersion() >= 1.6) {
            this.setTabComponentAt(this.getTabCount() - 1, new ChangeHeader(view));
        }
        this.setFocusable(false);
    }

    private MainTabbedPane() {

        this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.setOpaque(false);
        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (SwingGui.getInstance() != null) SwingGui.getInstance().setWaiting(true);
                try {
                    View comp = (View) getSelectedComponent();
                    if (comp == latestSelection) return;
                    if (latestSelection != null) {
                        latestSelection.setHidden();
                    }
                    latestSelection = comp;
                    comp.setShown();
                    revalidate();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }

            }

        });
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (SwingGui.getInstance() != null) SwingGui.getInstance().setWaiting(false);
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
        SwingGui.checkEDT();
        return (View) super.getSelectedComponent();
    }

    @Override
    public void setSelectedComponent(Component e) {
        SwingGui.checkEDT();
        super.setSelectedComponent(getComponentEquals((View) e));
    }

    /**
     * returns the component in this tab that equals view
     * 
     * @param view
     * @return
     */
    public View getComponentEquals(View view) {
        SwingGui.checkEDT();
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

}
