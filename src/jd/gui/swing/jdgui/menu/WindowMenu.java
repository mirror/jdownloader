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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import jd.gui.swing.jdgui.actions.ToolBarAction;

import org.appwork.utils.Application;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.translate._JDT;

public class WindowMenu extends JMenu {

    private static final long serialVersionUID = 1019851981865519325L;
    private static WindowMenu INSTANCE         = null;

    private WindowMenu() {
        super(_JDT._.gui_menu_windows());
        updateMenu();
    }

    public void update() {
        this.removeAll();
        updateMenu();

    }

    public static WindowMenu getInstance() {
        if (INSTANCE == null) INSTANCE = new WindowMenu();
        return INSTANCE;
    }

    private void updateMenu() {

        ArrayList<AbstractExtension<?>> pluginsOptional = ExtensionController.getInstance().getEnabledExtensions();
        Collections.sort(pluginsOptional, new Comparator<AbstractExtension<?>>() {

            public int compare(AbstractExtension<?> o1, AbstractExtension<?> o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        for (final AbstractExtension<?> plg : pluginsOptional) {
            if (!plg.isEnabled()) continue;

            if (plg.getShowGuiAction() != null) {

                if (Application.getJavaVersion() >= 16000000) {
                    // Togglebuttons for 1.6
                    add(new JCheckBoxMenuItem(plg.getShowGuiAction()));

                } else {
                    // 1.5 togle buttons need a changelistener in the menuitem
                    final JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(plg.getShowGuiAction());
                    this.addPropertyChangeListener(new PropertyChangeListener() {
                        public void propertyChange(final PropertyChangeEvent evt) {
                            m2.setSelected(((ToolBarAction) evt.getSource()).isSelected());
                        }
                    });
                    add(m2);
                }
            }
        }

    }
}
