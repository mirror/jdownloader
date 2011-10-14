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

import org.appwork.exceptions.WTFException;

@Deprecated
public class DecryptPluginWrapper extends PluginWrapper {

    public static ArrayList<DecryptPluginWrapper> getDecryptWrapper() {
        throw new WTFException();
    }

    public static boolean hasPlugin(final String data) {
        throw new WTFException();
    }

    public DecryptPluginWrapper(final String host, final String classNamePrefix, final String className, final String patternSupported, final int flags, final String revision) {
        super(host, classNamePrefix, className, patternSupported, flags, revision);
        throw new WTFException();
    }

    public DecryptPluginWrapper(final String host, final String className, final String patternSupported, final int flags, final String revision) {
        this(host, "jd.plugins.decrypter.", className, patternSupported, flags, revision);
        throw new WTFException();
    }

    @Override
    public PluginForDecrypt getPlugin() {
        throw new WTFException();
    }

    @Override
    public PluginForDecrypt getNewPluginInstance() {
        throw new WTFException();
    }

}
