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

package jd.gui.swing.jdgui.menu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.AbstractExtensionWrapper;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class AddonsMenu extends JMenu {

    private static final long serialVersionUID = 1019851981865519325L;
    private static AddonsMenu INSTANCE         = null;

    private AddonsMenu() {
        super(_JDT._.gui_menu_extensions());
        updateMenu();
    }

    public void update() {
        this.removeAll();
        updateMenu();

    }

    public static AddonsMenu getInstance() {
        if (INSTANCE == null) INSTANCE = new AddonsMenu();
        return INSTANCE;
    }

    private void updateMenu() {
        this.add(ActionController.getToolBarAction("addonsMenu.configuration"));

        ArrayList<JMenuItem> itemsWithSubmenu = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsToggle = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsPress = new ArrayList<JMenuItem>();
        ArrayList<AbstractExtensionWrapper> pluginsOptional = ExtensionController.getInstance().getExtensions();
        Collections.sort(pluginsOptional, new Comparator<AbstractExtensionWrapper>() {

            public int compare(AbstractExtensionWrapper o1, AbstractExtensionWrapper o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        for (final AbstractExtensionWrapper wrapper : pluginsOptional) {

            if (wrapper._isEnabled()) {
                final AbstractExtension<?> plg = wrapper._getExtension();
                ArrayList<MenuAction> mis = plg.getMenuAction();
                if (mis != null && !mis.isEmpty()) {
                    if (mis.size() == 1) {
                        JMenuItem c = mis.get(0).toJMenuItem();
                        c.setIcon(NewTheme.I().getIcon(plg.getIconKey(), 16));
                        if (mis.get(0).getType() == Types.TOGGLE) {
                            itemsToggle.add(c);
                        } else {
                            itemsPress.add(c);
                        }
                    } else {
                        MenuAction m = new MenuAction(plg.getConfigID(), plg.getName(), plg.getIconKey()) {

                            @Override
                            protected String createMnemonic() {
                                return plg.getName();
                            }

                            @Override
                            protected String createAccelerator() {
                                return "ctrl+shift" + plg.getName().charAt(0);
                            }

                            @Override
                            protected String createTooltip() {
                                return plg.getName();
                            }

                        };
                        m.setItems(mis);

                        JMenuItem mi = m.toJMenuItem();
                        itemsWithSubmenu.add(mi);
                    }
                }
            } else if (wrapper.isQuickToggleEnabled()) {

                JMenuItem jmi = new JMenuItem(new AbstractAction(wrapper.getName(), wrapper._getIcon(16)) {

                    public void actionPerformed(ActionEvent e) {
                        if (!wrapper._isEnabled()) {
                            try {
                                wrapper._setEnabled(true);

                                if (wrapper._getExtension().getGUI() != null) {
                                    int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, wrapper.getName(), _JDT._.gui_settings_extensions_show_now(wrapper.getName()));

                                    if (UserIO.isOK(ret)) {
                                        // activate panel
                                        wrapper._getExtension().getGUI().setActive(true);
                                        // bring panel to front
                                        wrapper._getExtension().getGUI().toFront();

                                    }
                                }
                            } catch (StartException e1) {
                                Dialog.getInstance().showExceptionDialog(_JDT._.dialog_title_exception(), e1.getMessage(), e1);
                            } catch (StopException e1) {
                                e1.printStackTrace();
                            }
                        } else {
                            try {

                                wrapper._setEnabled(false);
                            } catch (StartException e1) {
                                e1.printStackTrace();
                            } catch (StopException e1) {
                                Dialog.getInstance().showExceptionDialog(_JDT._.dialog_title_exception(), e1.getMessage(), e1);
                            }
                        }

                    }
                });
                itemsToggle.add(jmi);

            }
        }

        boolean pre = false;
        for (JMenuItem jmi : itemsWithSubmenu) {
            if (!pre) {
                addSeparator();
                pre = true;
            }
            add(jmi);
        }

        pre = false;
        for (JMenuItem jmi : itemsPress) {
            if (!pre) {
                addSeparator();
                pre = true;
            }
            add(jmi);
        }

        pre = false;
        for (JMenuItem jmi : itemsToggle) {
            if (!pre) {
                addSeparator();
                pre = true;
            }
            add(jmi);
        }
    }
}
