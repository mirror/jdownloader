package jd.gui.skins.simple;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import jd.utils.JDLocale;
import jd.utils.JDTheme;

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
        menu.add(new MenuAbstractAction(t, JDLocale.L("gui.textcomponent.context.cut", "Ausschneiden"),JDTheme.II("gui.icons.cut",16,16),JDLocale.L("gui.textcomponent.context.cut.acc","ctrl X")) {
            public void actionPerformed(ActionEvent e) {
                c.cut();
            }
        });
        menu.add(new MenuAbstractAction(t, JDLocale.L("gui.textcomponent.context.copy","Kopieren"),JDTheme.II("gui.icons.copy",16,16),JDLocale.L("gui.textcomponent.context.copy.acc","ctrl C")) {
            public void actionPerformed(ActionEvent e) {
                c.copy();
            }

            public boolean isEnabled() {
                return c.isEnabled() && c.getSelectedText() != null;
            }
        });
        menu.add(new MenuAbstractAction(t, JDLocale.L("gui.textcomponent.context.paste","Einfügen"),JDTheme.II("gui.icons.paste",16,16),JDLocale.L("gui.textcomponent.context.paste.acc","ctrl V")) {
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
        menu.add(new MenuAbstractAction(t, JDLocale.L("gui.textcomponent.context.delete","Löschen"),JDTheme.II("gui.icons.delete",16,16),JDLocale.L("gui.textcomponent.context.delete.acc","DELETE")) {
            public void actionPerformed(ActionEvent e) {
                c.replaceSelection(null);
            }
        });
        menu.addSeparator();
        menu.add(new MenuAbstractAction(t, JDLocale.L("gui.textcomponent.context.selectall","Alles auswählen"),JDTheme.II("gui.icons.select_all",16,16),JDLocale.L("gui.textcomponent.context.selectall.acc","ctrl A")) {
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
       

        public MenuAbstractAction(JTextComponent c, String text,ImageIcon icon, String acc) {
            super(text);
            this.c = c;
            if(icon!=null)putValue(Action.SMALL_ICON, icon);  
           
         
                putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(acc));
       
          
            
        }

        public boolean isEnabled() {
            return c.isEditable() && c.isEnabled() && c.getSelectedText() != null;
        }
    }
}
