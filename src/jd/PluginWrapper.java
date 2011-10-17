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

package jd;

import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public abstract class PluginWrapper {

    public static final int         LOAD_ON_INIT = 1 << 1;

    /**
     * See http://wiki.jdownloader.org/knowledge/wiki/glossary/cnl2 for cnl2
     * details. If a Decrypter uses CNL2, we can think about activating this
     * feature here. JDownloader then will only decrypt indriect or
     * deepencrypted links. Direct links will be opened in th systems
     * defaultbrowser to use CNL
     */
    public static final int         CNL_2        = 1 << 4;

    /**
     * Load only if debug flag is set. For internal developer plugins
     */
    public static final int         DEBUG_ONLY   = 1 << 5;

    private transient LazyPlugin<?> lazy;

    public PluginWrapper(final LazyPlugin<?> lazy) {
        this.lazy = lazy;
    }

    public LazyPlugin<?> getLazy() {
        return lazy;
    }

    @Deprecated
    public static PluginWrapper getWrapper(String name) {
        for (LazyHostPlugin p : HostPluginController.getInstance().list()) {
            if ("DirectHTTP".equals(p.getDisplayName())) { return new PluginWrapper((LazyPlugin<?>) p) {

            }; }
        }
        return null;
    }

}
