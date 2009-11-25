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

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;
import jd.utils.locale.JDL;

abstract public class ClosableView extends View {

    private static final long serialVersionUID = 8698758386841005256L;
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.views.ClosableView.";
    private JMenuBar menubar;
    private CloseAction closeAction;

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
        closeAction = new CloseAction();
        if (menubar.getComponentCount() > count) {
            add(menubar, "dock NORTH,height 16!,gapbottom 2");
        }
    }

    public CloseAction getCloseAction() {
        return closeAction;
    }

    /**
     * May be overridden to add some more menu Items
     * 
     * @param menubar
     */
    protected void initMenu(JMenuBar menubar) {
    }

    /**
     * CLoses this view
     */
    public void close() {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                closeAction.actionPerformed(null);
                return null;
            }

        }.start();

    }

    public class CloseAction extends AbstractAction {
        private static final long serialVersionUID = -771203720364300914L;
        private int height;
        private int width;

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }

        public CloseAction() {
            Icon ic = UIManager.getIcon("InternalFrame.closeIcon");
            this.height = ic.getIconHeight();
            this.width = ic.getIconWidth();
            this.putValue(AbstractAction.SMALL_ICON, ic);
            this.putValue(AbstractAction.SHORT_DESCRIPTION, JDL.L(JDL_PREFIX + "closeTab", "Close Tab"));
        }

        public void actionPerformed(ActionEvent e) {
            MainTabbedPane.getInstance().remove(ClosableView.this);
        }
    }
}
