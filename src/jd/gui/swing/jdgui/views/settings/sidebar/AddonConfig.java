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

import javax.swing.Icon;
import javax.swing.JPanel;

import jd.config.ConfigContainer;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;

import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.storage.config.MinTimeWeakReferenceCleanup;

public class AddonConfig extends ConfigPanel {

    private static final long                                         serialVersionUID = 5561326475681668634L;

    private static HashMap<String, MinTimeWeakReference<AddonConfig>> MAP              = new HashMap<String, MinTimeWeakReference<AddonConfig>>();
    private static MinTimeWeakReferenceCleanup                        cleanup          = new MinTimeWeakReferenceCleanup() {

                                                                                           @Override
                                                                                           public void onMinTimeWeakReferenceCleanup(MinTimeWeakReference<?> minTimeWeakReference) {
                                                                                               synchronized (MAP) {
                                                                                                   MAP.values().remove(minTimeWeakReference);
                                                                                               }
                                                                                           }

                                                                                       };

    private final ConfigContainer                                     container;

    private final boolean                                             showGroups;

    private AddonConfig(ConfigContainer container, boolean showGroups) {
        super();
        this.container = container;
        this.showGroups = showGroups;
        init(true);
    }

    @Override
    protected boolean showGroups() {
        return showGroups;
    }

    @Override
    protected ConfigContainer setupContainer() {
        return container;
    }

    public JPanel getPanel() {
        return panel;
    }

    @Override
    public String getTitle() {
        return container.getEntries().get(0).getGroup().getName();
    }

    @Override
    public Icon getIcon() {
        return container.getEntries().get(0).getGroup().getIcon();
    }

    /**
     * Caches {@link AddonConfig} panels...
     * 
     * @param container
     * @param ext
     * @return
     */
    public synchronized static AddonConfig getInstance(ConfigContainer container, String ext, boolean showGroups) {
        String id = container + "_" + ext;
        MinTimeWeakReference<AddonConfig> weak = MAP.get(id);
        AddonConfig config = null;
        if (weak != null && (config = weak.get()) != null) return config;
        config = new AddonConfig(container, showGroups);
        MAP.put(container + "_" + ext, new MinTimeWeakReference<AddonConfig>(config, 30 * 1000l, id, cleanup));
        return config;
    }
}
