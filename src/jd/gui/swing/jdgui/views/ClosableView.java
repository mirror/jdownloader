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

import java.awt.event.ActionEvent;

import javax.swing.JMenuBar;

import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.JDCloseButton;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

public abstract class ClosableView extends View {

    private static final long serialVersionUID = 8698758386841005256L;
    private JMenuBar menubar;
    private JDCloseButton closeButton;

    public ClosableView() {
        super();
    }

    /**
     * has to be called to init the close menu
     */
    public void init() {
        menubar = new JMenuBar();
        int count = menubar.getComponentCount();
        initMenu(menubar);
        closeButton = new JDCloseButton() {
            private static final long serialVersionUID = -8427069347798591918L;

            public void actionPerformed(ActionEvent e) {
                MainTabbedPane.getInstance().remove(ClosableView.this);
            }
        };
        if (menubar.getComponentCount() > count) {
            add(menubar, "dock NORTH,h pref!,gapbottom 2");
        }
    }

    public JDCloseButton getCloseButton() {
        return closeButton;
    }

    /**
     * May be overridden to add some more menu Items
     * 
     * @param menubar
     */
    protected void initMenu(JMenuBar menubar) {
    }

    /**
     * Closes this view
     */
    public void close() {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                closeButton.actionPerformed(null);
                return null;
            }

        }.start();
    }

}
