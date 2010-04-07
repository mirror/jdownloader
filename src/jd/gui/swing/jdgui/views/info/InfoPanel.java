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

package jd.gui.swing.jdgui.views.info;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.EventListenerList;

import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.interfaces.DroppedPanel;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public abstract class InfoPanel extends DroppedPanel {

    private static final long serialVersionUID = 5465564955866972884L;

    protected EventListenerList listenerList;

    protected final Color valueColor;
    protected final Color titleColor;
    private final HashMap<String, JComponent> map;

    public InfoPanel(String iconKey) {
        super();
        SwingGui.checkEDT();

        listenerList = new EventListenerList();
        map = new HashMap<String, JComponent>();
        valueColor = getBackground().darker().darker().darker().darker().darker();
        titleColor = getBackground().darker().darker();

        setLayout(new MigLayout("ins 5", "[]5[]", "[][]"));
        add(new JLabel(JDTheme.II(iconKey, 32, 32)), "spany 2,cell 0 0,gapleft 1");
    }

    /**
     * Adds an <code>ActionListener</code> to the InfoPanel.
     * 
     * @param l
     *            the <code>ActionListener</code> to be added
     */
    public void addActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
        listenerList.add(ActionListener.class, l);
    }

    /**
     * Removes an <code>ActionListener</code> from the InfoPanel.
     * 
     * @param l
     *            the listener to be removed
     */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);

    }

    public void broadcastEvent(final ActionEvent e) {
        for (ActionListener listener : listenerList.getListeners(ActionListener.class)) {
            listener.actionPerformed(e);
        }
    }

    /**
     * Updates an entry previously added my addInfoEntry. Use as key the
     * previously used title
     * 
     * @param key
     * @param string
     */
    protected void updateInfo(String key, Object value) {
        SwingGui.checkEDT();
        JComponent c = map.get(key);

        if (c != null && c instanceof JLabel) {
            ((JLabel) c).setText(value.toString());
        }
    }

    /**
     * Adds an info entry at x ,y title has to be constant and value may be
     * updated later by using updateInfo(..)
     * 
     * @param title
     * @param value
     * @param x
     * @param y
     */
    protected void addInfoEntry(String title, String value, int x, int y) {
        SwingGui.checkEDT();
        JLabel myValue = new JLabel(value);
        myValue.setForeground(valueColor);
        addComponent(title, myValue, x, y);
    }

    protected void addComponent(JComponent myComponent, int x, int y) {
        SwingGui.checkEDT();
        x *= 2;
        x += 1;
        myComponent.setForeground(valueColor);
        add(myComponent, "gapleft 20,cell " + x + " " + y + ",spanx 2,growx");
        map.put(myComponent.getName(), myComponent);
    }

    protected void addComponent(String title, JComponent myComponent, int x, int y) {
        SwingGui.checkEDT();
        x *= 2;
        x += 1;
        JLabel myTitle = new JLabel((title != null && title.length() > 0) ? title + ":" : "");
        myTitle.setForeground(titleColor);
        myComponent.setForeground(valueColor);
        add(myTitle, "gapleft 20,alignx right,cell " + x + " " + y);
        add(myComponent, "cell " + (x + 1) + " " + y);
        map.put(title, myComponent);
    }

    protected JComponent getComponent(String key) {
        return map.get(key);
    }

    @Override
    public void onHide() {
    }

    @Override
    public void onShow() {
    }

}
