//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.skins.simple;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import jd.gui.skins.SwingGui;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class JDEventQueue extends EventQueue {

    private int lastPoint = 0;

    private JPanel mouseOver;

    public JDEventQueue() {
        super();

        mouseOver = new JPanel(new MigLayout("ins 3"));
        mouseOver.add(new JLabel(JDTheme.II("gui.images.help", 24, 24)), "alignx left");
        mouseOver.setBorder(BorderFactory.createLineBorder(mouseOver.getBackground().darker()));
    }

    abstract class MenuAbstractAction extends AbstractAction {

        private static final long serialVersionUID = 1L;
        JTextComponent c;

        public MenuAbstractAction(JTextComponent c, String text, ImageIcon icon, String acc) {
            super(text);
            this.c = c;
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
            }

            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(acc));

        }

        // @Override
        public boolean isEnabled() {
            return c.isEditable() && c.isEnabled() && c.getSelectedText() != null;
        }
    }

    // @Override
    protected void dispatchEvent(AWTEvent ev) {
        qh: if (ev instanceof MouseEvent) {
            MouseEvent e = (MouseEvent) ev;

            if ((e.getID() == MouseEvent.MOUSE_RELEASED || e.getID() == MouseEvent.MOUSE_CLICKED) && lastPoint > 0) {

                lastPoint--;
                break qh;
            }

        }

        super.dispatchEvent(ev);
        if (!(ev instanceof MouseEvent)) { return; }

        MouseEvent e = (MouseEvent) ev;
        if (!e.isPopupTrigger()) { return; }
        if (e.getComponent() == null) return;
        Component c = null;
        Point point = e.getPoint();
        if (e.getSource() instanceof JDialog) {
            c = SwingUtilities.getDeepestComponentAt((JDialog) e.getSource(), (int) point.getX(), (int) point.getY());
        } else {
            Component source = SwingGui.getInstance().getContentPane();
            point.x -= (source.getLocationOnScreen().x - SwingGui.getInstance().getLocationOnScreen().x);
            point.y -= (source.getLocationOnScreen().y - SwingGui.getInstance().getLocationOnScreen().y);
            c = SwingUtilities.getDeepestComponentAt(source, (int) point.getX(), (int) point.getY());
        }
        if (!(c instanceof JTextComponent)) { return; }
        if (MenuSelectionManager.defaultManager().getSelectedPath().length > 0) { return; }
        final JTextComponent t = (JTextComponent) c;

        JPopupMenu menu = new JPopupMenu();

        menu.add(new MenuAbstractAction(t, JDL.L("gui.textcomponent.context.cut", "Ausschneiden"), JDTheme.II("gui.icons.cut", 16, 16), JDL.L("gui.textcomponent.context.cut.acc", "ctrl X")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.cut();
            }
        });
        menu.add(new MenuAbstractAction(t, JDL.L("gui.textcomponent.context.copy", "Kopieren"), JDTheme.II("gui.icons.copy", 16, 16), JDL.L("gui.textcomponent.context.copy.acc", "ctrl C")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.copy();
            }

            // @Override
            public boolean isEnabled() {
                return c.isEnabled() && c.getSelectedText() != null;
            }
        });
        menu.add(new MenuAbstractAction(t, JDL.L("gui.textcomponent.context.paste", "Einfügen"), JDTheme.II("gui.icons.paste", 16, 16), JDL.L("gui.textcomponent.context.paste.acc", "ctrl V")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.paste();
            }

            // @Override
            public boolean isEnabled() {
                if (c.isEditable() && c.isEnabled()) {
                    Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
                    return contents.isDataFlavorSupported(DataFlavor.stringFlavor);
                } else {
                    return false;
                }
            }
        });
        menu.add(new MenuAbstractAction(t, JDL.L("gui.textcomponent.context.delete", "Löschen"), JDTheme.II("gui.icons.delete", 16, 16), JDL.L("gui.textcomponent.context.delete.acc", "DELETE")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.replaceSelection(null);
            }
        });

        menu.add(new MenuAbstractAction(t, JDL.L("gui.textcomponent.context.selectall", "Alles auswählen"), JDTheme.II("gui.icons.select_all", 16, 16), JDL.L("gui.textcomponent.context.selectall.acc", "ctrl A")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.selectAll();
            }

            // @Override
            public boolean isEnabled() {
                return c.isEnabled() && c.getText().length() > 0;
            }
        });

        Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), t);
        menu.show(t, pt.x, pt.y);
    }
}
