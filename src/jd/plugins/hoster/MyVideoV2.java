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

package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.plugins.HostPlugin;

// Altes Decrypterplugin bis Revision 14394 
@HostPlugin(revision = "$Revision: 26268 $", interfaceVersion = 3, names = { "myvideo.de" }, urls = { "fromDecrypter://(www\\.)?myvideo\\.(de|at)/watch/\\d+(/\\w+)?" }, flags = { 32 })
public class MyVideoV2 extends MyVideo {

    public MyVideoV2(PluginWrapper wrapper) {
        super(wrapper);
    }

}