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

package jd.gui.swing.jdgui.views.settings.sidebar;

import java.util.HashMap;

import javax.swing.JComponent;

import jd.config.ConfigContainer;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;

public class AddonConfig extends ConfigPanel {

    private static final long serialVersionUID = 5561326475681668634L;

    private static HashMap<String, AddonConfig> MAP;

    private AddonConfig(ConfigContainer container) {
        super(container);
    }

    public JComponent getPanel() {
        return this.getRootPane();
    }

    /**
     * Caches {@link AddonConfig} panels...
     * 
     * @param container
     * @param ext
     * @return
     */
    public synchronized static AddonConfig getInstance(ConfigContainer container, String ext) {
        if (MAP == null) MAP = new HashMap<String, AddonConfig>();

        AddonConfig p = MAP.get(container + "_" + ext);
        if (p != null) return p;

        MAP.put(container + "_" + ext, p = new AddonConfig(container));
        return p;
    }

}
