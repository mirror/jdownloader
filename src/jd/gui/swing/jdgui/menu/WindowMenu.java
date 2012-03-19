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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import jd.controlling.IOEQ;
import jd.gui.swing.jdgui.menu.actions.LogAction;
import jd.gui.swing.jdgui.menu.actions.SettingsAction;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.ExtensionControllerListener;
import org.jdownloader.translate._JDT;

public class WindowMenu extends JMenu implements ExtensionControllerListener {

    private static final long serialVersionUID = 1019851981865519325L;
    private static WindowMenu INSTANCE         = null;

    private WindowMenu() {
        super(_JDT._.gui_menu_windows());
        ExtensionController.getInstance().getEventSender().addListener(this);
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

        add(new SettingsAction());
        add(new LogAction());
        for (final AbstractExtension<?> plg : pluginsOptional) {
            if (!plg.isEnabled()) continue;

            if (plg.getShowGuiAction() != null) {

                add(new JCheckBoxMenuItem(plg.getShowGuiAction()));

            }
        }

    }

    public void onUpdated() {
        IOEQ.add(new Runnable() {

            public void run() {
                removeAll();
                updateMenu();
            }

        }, true);
    }
}
