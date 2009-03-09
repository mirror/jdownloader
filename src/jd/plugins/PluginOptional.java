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

package jd.plugins;

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.event.ControlEvent;
import jd.event.ControlListener;

public abstract class PluginOptional extends Plugin implements ControlListener {

    public PluginOptional(PluginWrapper wrapper) {
        super(wrapper);

    }

    public static final int ADDON_INTERFACE_VERSION = 2;

    public void controlEvent(ControlEvent event) {

        // Deaktiviert das PLugin beim beenden
        if (event.getID() == ControlEvent.CONTROL_SYSTEM_EXIT) {
            try {
                onExit();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public String getLinkName() {
        return null;
    }

    public abstract String getRequirements();

    @Override
    public Pattern getSupportedLinks() {
        return null;
    }

    public abstract boolean initAddon();

    public abstract void onExit();

    public Object interact(String command, Object parameter) {
        return null;
    }
}
