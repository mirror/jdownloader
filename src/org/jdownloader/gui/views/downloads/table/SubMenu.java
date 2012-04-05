package org.jdownloader.gui.views.downloads.table;

import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class SubMenu extends JMenuItem {
    private SubMenuEditor editor;

    public SubMenu(final String title, final Icon icon, final SubMenuEditor comp) {
        super(new AppAction() {
            {

                setName(title);
                setSmallIcon(icon);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Dialog.getInstance().showDialog(new AbstractDialog<Object>(Dialog.STYLE_HIDE_ICON, title, null, _GUI._.literally_save(), null) {
                        {

                        }

                        @Override
                        protected Object createReturnValue() {

                            return null;
                        }

                        @Override
                        protected Point getDesiredLocation() {
                            // we want to be the mouse pointer exactly over the
                            // desired location givven by SubMenuEditor
                            Point mouse = MouseInfo.getPointerInfo().getLocation();
                            Point offset = comp.getDesiredLocation();
                            mouse.x -= offset.x;
                            mouse.y -= offset.y;
                            // try to find offsets by frame window
                            Insets insets = JDGui.getInstance().getMainFrame().getInsets();
                            mouse.x -= insets.left;
                            mouse.y -= insets.top;
                            return mouse;
                        }

                        protected void setReturnmask(final boolean b) {
                            super.setReturnmask(b);
                            if (b) {

                                comp.save();
                            }
                        }

                        @Override
                        public JComponent layoutDialogContent() {
                            return comp;
                        }
                    });
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        });

    }

}
