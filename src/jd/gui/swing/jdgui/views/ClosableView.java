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
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.images.NewTheme;

public abstract class ClosableView extends View {

    private static final long serialVersionUID = 8698758386841005256L;
    private JPanel            menubar;
    private ExtButton         closeButton;

    public ClosableView() {
        super();
    }

    /**
     * has to be called to init the close menu
     */
    public void init() {
        menubar = new JPanel();
        int count = menubar.getComponentCount();
        initMenuPanel(menubar);
        closeButton = new ExtButton() {
            {
                setRolloverEffectEnabled(true);
                addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        MainTabbedPane.getInstance().remove(ClosableView.this);
                        onClosed();
                        onRollOut();
                    }
                });
            }

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            protected void onRollOut() {
                setContentAreaFilled(false);
                setIcon(NewTheme.I().getIcon("close", -1));
            }

            /**
             * 
             */
            protected void onRollOver() {
                setIcon(NewTheme.I().getIcon("close.on", -1));
            }

        };

        if (menubar.getComponentCount() > count) {
            add(menubar, "dock NORTH,h pref!,gapbottom 2");
        }
    }

    public ExtButton getCloseButton() {
        return closeButton;
    }

    /**
     * May be overridden to add some more menu Items
     * 
     * @param menubar
     */
    protected void initMenuPanel(JPanel menubar) {
    }

    /**
     * Closes this view
     */
    public void close() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                closeButton.doClick();
            }
        };
    }

    public void onClosed() {
    }

}
