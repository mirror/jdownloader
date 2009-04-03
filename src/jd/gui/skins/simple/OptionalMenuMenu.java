package jd.gui.skins.simple;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jd.OptionalPluginWrapper;
import jd.config.MenuItem;
import jd.utils.JDUtilities;

public class OptionalMenuMenu extends JDMenu {

    private static final long serialVersionUID = 3811108697902787341L;
    private static OptionalMenuMenu menu;
    private MenuAction actionOptionalConfig;

    public OptionalMenuMenu(String l, MenuAction actionOptionalConfig) {
        super(l);
        this.actionOptionalConfig = actionOptionalConfig;
    }

    public static OptionalMenuMenu getMenu(String l, MenuAction actionOptionalConfig) {
        if (menu != null)return menu;
        menu = new OptionalMenuMenu(l, actionOptionalConfig);
        menu.createOptionalPluginsMenuEntries();
        return menu;
    }

    public void createOptionalPluginsMenuEntries() {
        Component temp = (getComponentCount() != 0) ? getComponent(0) : createMenuItem(this.actionOptionalConfig);
        removeAll();
        add(temp);
        addSeparator();
        ArrayList<JMenuItem> itemsWithSubmenu = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsToggle = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsPress = new ArrayList<JMenuItem>();

        for (final OptionalPluginWrapper plg : OptionalPluginWrapper.getOptionalWrapper()) {
            if (!plg.isLoaded()) continue;
            if (plg.getPlugin().createMenuitems() != null && JDUtilities.getConfiguration().getBooleanProperty(plg.getConfigParamKey(), false)) {
                if (plg.getPlugin().createMenuitems().size() > 1) {

                    MenuItem m = new MenuItem(MenuItem.CONTAINER, plg.getPlugin().getHost(), 0);
                    m.setItems(plg.getPlugin().createMenuitems());
                    JMenuItem mi = getJMenuItem(m);

                    if (mi != null) {
                        itemsWithSubmenu.add(mi);

                        ((JMenu) mi).removeMenuListener(((JMenu) mi).getMenuListeners()[0]);
                        ((JMenu) mi).addMenuListener(new MenuListener() {
                            public void menuCanceled(MenuEvent e) {
                            }

                            public void menuDeselected(MenuEvent e) {
                            }

                            public void menuSelected(MenuEvent e) {
                                JMenu m = (JMenu) e.getSource();

                                m.removeAll();
                                for (MenuItem menuItem : plg.getPlugin().createMenuitems()) {
                                    JMenuItem c = getJMenuItem(menuItem);

                                    if (c == null) {
                                        m.addSeparator();
                                    } else {
                                        m.add(c);
                                    }
                                }
                            }

                        });
                    } else {
                        this.addSeparator();
                    }
                } else {
                    for (MenuItem mi : plg.getPlugin().createMenuitems()) {
                        JMenuItem c = getJMenuItem(mi);
                        c.setDisabledIcon(null);
                        c.setIcon(null);
                        c.setSelectedIcon(null);
                        c.setDisabledSelectedIcon(null);
                        if (mi.getID() == MenuItem.TOGGLE) {
                            itemsToggle.add(c);
                        } else {
                            itemsPress.add(c);
                        }
                        break;
                    }
                }
                for (JMenuItem jmi : itemsWithSubmenu) {
                    this.add(jmi);
                }
                for (JMenuItem jmi : itemsPress) {
                    this.add(jmi);
                }
                for (JMenuItem jmi : itemsToggle) {
                    this.add(jmi);
                }
            }
        }
        if (this.getItem(this.getItemCount() - 1) == null) {
            this.remove(this.getItemCount() - 1);
        }
    }

}
