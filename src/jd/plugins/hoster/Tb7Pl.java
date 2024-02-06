//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.plugins.HostPlugin;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tb7.pl" }, urls = { "" })
public class Tb7Pl extends Tb7AndXt7PlCORE {
    public Tb7Pl(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected static MultiHosterManagement mhm = new MultiHosterManagement("tb7.pl");

    @Override
    protected MultiHosterManagement getMultiHosterManagement() {
        return mhm;
    }
}