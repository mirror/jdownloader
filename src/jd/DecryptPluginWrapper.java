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

import java.util.ArrayList;

import jd.plugins.PluginForDecrypt;

public class DecryptPluginWrapper extends PluginWrapper {
    private static final ArrayList<DecryptPluginWrapper> DECRYPT_WRAPPER = new ArrayList<DecryptPluginWrapper>();

    public static ArrayList<DecryptPluginWrapper> getDecryptWrapper() {
        return DECRYPT_WRAPPER;
    }

    public DecryptPluginWrapper(String host, String className, String patternSupported, int flags) {
        super(host, "jd.plugins.decrypt." + className, patternSupported, flags);
        DECRYPT_WRAPPER.add(this);
    }

    public DecryptPluginWrapper(String host, String className, String patternSupported) {
        this(host, className, patternSupported, 0);
    }

    @Override
    public PluginForDecrypt getPlugin() {
        return (PluginForDecrypt) super.getPlugin();
    }

}
