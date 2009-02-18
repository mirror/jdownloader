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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

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
    public JDEventQueue() {
        super();
        // comment out in production code
        // JDUtilities.getLogger().fine("Enter " + JDEventQueue.class.getSimpleName());
        eventQueueInteruptionTest();
    }
    
    abstract class MenuAbstractAction extends AbstractAction {
        /**
         * 
         */
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

        @Override
        public boolean isEnabled() {
            return c.isEditable() && c.isEnabled() && c.getSelectedText() != null;
        }
    }

    @Override
    protected void dispatchEvent(AWTEvent ev) {
        super.dispatchEvent(ev);
        if (!(ev instanceof MouseEvent)) { return; }
        MouseEvent e = (MouseEvent) ev;
        if (!e.isPopupTrigger()) { return; }
        if (e.getComponent() == null) return;
        Component c = SwingUtilities.getDeepestComponentAt(e.getComponent(), e.getX(), e.getY());
        if (!(c instanceof JTextComponent)) { return; }
        if (MenuSelectionManager.defaultManager().getSelectedPath().length > 0) { return; }
        final JTextComponent t = (JTextComponent) c;
        
        if (t.getSelectedText() == null) {
            t.requestFocusInWindow();
            int length = t.getText().length();
            t.select(0, length);
//          t.setCaretPosition(length);
        }
        
        JPopupMenu menu = new JPopupMenu();
        menu.add(new MenuAbstractAction(t, JDLocale.L("gui.textcomponent.context.cut", "Ausschneiden"), JDTheme.II("gui.icons.cut", 16, 16), JDLocale.L("gui.textcomponent.context.cut.acc", "ctrl X")) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.cut();
            }
        });
        menu.add(new MenuAbstractAction(t, JDLocale.L("gui.textcomponent.context.copy", "Kopieren"), JDTheme.II("gui.icons.copy", 16, 16), JDLocale.L("gui.textcomponent.context.copy.acc", "ctrl C")) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.copy();
            }

            @Override
            public boolean isEnabled() {
                return c.isEnabled() && c.getSelectedText() != null;
            }
        });
        menu.add(new MenuAbstractAction(t, JDLocale.L("gui.textcomponent.context.paste", "Einfügen"), JDTheme.II("gui.icons.paste", 16, 16), JDLocale.L("gui.textcomponent.context.paste.acc", "ctrl V")) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.paste();
            }

            @Override
            public boolean isEnabled() {
                if (c.isEditable() && c.isEnabled()) {
                    Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
                    return contents.isDataFlavorSupported(DataFlavor.stringFlavor);
                } else {
                    return false;
                }
            }
        });
        menu.add(new MenuAbstractAction(t, JDLocale.L("gui.textcomponent.context.delete", "Löschen"), JDTheme.II("gui.icons.delete", 16, 16), JDLocale.L("gui.textcomponent.context.delete.acc", "DELETE")) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.replaceSelection(null);
            }
        });
//        if ((c instanceof JDTextField) || (c instanceof JDTextArea)) {
//            menu.addSeparator();
//            if ((c instanceof JDTextField)) {
//                if (((JDTextField) c).getUndoManager().canUndo()) {
//                    menu.add(((JDTextField) c).getUndoManager().getUndoAction());
//                }
//                if (((JDTextField) c).getUndoManager().canRedo()) {
//                    menu.add(((JDTextField) c).getUndoManager().getRedoAction());
//                }
//            } else if ((c instanceof JDTextArea)) {
//                if (((JDTextArea) c).getUndoManager().canUndo()) {
//                    menu.add(((JDTextArea) c).getUndoManager().getUndoAction());
//                }
//                if (((JDTextArea) c).getUndoManager().canRedo()) {
//                    menu.add(((JDTextArea) c).getUndoManager().getRedoAction());
//                }
//            }
//            menu.addSeparator();
//        }

        menu.add(new MenuAbstractAction(t, JDLocale.L("gui.textcomponent.context.selectall", "Alles auswählen"), JDTheme.II("gui.icons.select_all", 16, 16), JDLocale.L("gui.textcomponent.context.selectall.acc", "ctrl A")) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.selectAll();
            }

            @Override
            public boolean isEnabled() {
                return c.isEnabled() && c.getText().length() > 0;
            }
        });

        Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), t);
        menu.show(t, pt.x, pt.y);
    }

    /**
     * Since we are using our own EventQueue, lengthy tasks could lead to
     * interruptions which may freeze the GUI or crash the program. This
     * function tests how severe the impact is when executed in the current
     * thread or a seperate thread.
     * 
     * Do not execute this function in production code. This is only a test.
     */
    static void eventQueueInteruptionTest() {
        Runnable runnable = new Runnable() {
            public void run() {
//                 long task, like calculating something.
                int sum = 0;
                byte[] test = new byte[]{104,116,116,112,58,47,47,119,119,119,46,121,111,117,116,117,98,101,46,99,111,109,47,119,97,116,99,104,63,118,61,75,82,95,72,85,56,69,122,97,79,48,};
                for (byte b : test) {
                    sum += b;
                }
                String result = new String(sum + ""); result = new String(test);
                // JDUtilities.getLogger().fine(result);
                
                // task of unpredictable length, like downloading a file
                unpredictableLengthTask(result);
            }
            
        };
        
        // run in current thread
        // runnable.run();
        
        // run in new thread
        new Thread(runnable).start();
    }
    
    static void unpredictableLengthTask(String urlString) {
        // JDUtilities.getLogger().info("Start Event Queue Interruption Test"); 
        try {
            // Create a URL for the desired page
            URL url = new URL(urlString);
        
            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder sb = new StringBuilder();
            String str;
            while ((str = in.readLine()) != null) {
                // str is one line of text; readLine() strips the newline character(s)
                sb.append(str);
            }
            in.close();
            // JDUtilities.getLogger().warning(sb.toString());
        } catch (Exception e) {System.err.println("Error");}
    }
}
