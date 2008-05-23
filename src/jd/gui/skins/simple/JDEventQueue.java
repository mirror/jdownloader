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
import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

public class JDEventQueue extends EventQueue {
    protected void dispatchEvent(AWTEvent ev) {
        super.dispatchEvent(ev);
        if (!(ev instanceof MouseEvent)) return;
        MouseEvent e = (MouseEvent) ev;
        if (!e.isPopupTrigger()) return;
        Component c = SwingUtilities.getDeepestComponentAt(e.getComponent(), e.getX(), e.getY());
        if (!(c instanceof JTextComponent)) return;
        if (MenuSelectionManager.defaultManager().getSelectedPath().length > 0) return;
        JTextComponent t = (JTextComponent) c;
        JPopupMenu menu = new JPopupMenu();
        menu.add(new MenuAbstractAction(t, "Cut") {public void actionPerformed(ActionEvent e) {c.cut();}});
        menu.add(new MenuAbstractAction(t, "Copy") {
            public void actionPerformed(ActionEvent e) {
                c.copy();
            }
            public boolean isEnabled() {
                return c.isEnabled() && c.getSelectedText() != null;
            }
        });
        menu.add(new MenuAbstractAction(t, "Paste") {
            public void actionPerformed(ActionEvent e) {
                c.paste();
            }
            public boolean isEnabled() {
                if (c.isEditable() && c.isEnabled()) {
                    Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
                    return contents.isDataFlavorSupported(DataFlavor.stringFlavor);
                } else
                    return false;
            }
        });
        menu.add(new MenuAbstractAction(t, "Delete") {public void actionPerformed(ActionEvent e) {c.replaceSelection(null);}});
        menu.addSeparator();
        menu.add(new MenuAbstractAction(t, "Select All") {
            public void actionPerformed(ActionEvent e) {
                c.selectAll();
            }

            public boolean isEnabled() {
                return c.isEnabled() && c.getText().length() > 0;
            }
        });

        Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), t);
        menu.show(t, pt.x, pt.y);
    }
    
    abstract class MenuAbstractAction extends AbstractAction {
        JTextComponent c;
        public MenuAbstractAction(JTextComponent c, String text) {super(text);this.c = c;}
        public boolean isEnabled() {return c.isEditable() && c.isEnabled() && c.getSelectedText() != null;}
    }
}
