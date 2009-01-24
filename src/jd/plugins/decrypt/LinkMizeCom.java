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

package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class LinkMizeCom extends PluginForDecrypt {

    public LinkMizeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setDebug(true); 
        //jedesmal wenn ich spechere compiliert eclipse gleich neu.. und spring an den afnag der funktion... muss also nicht immer neu starten
        br.setFollowRedirects(false);
br.getPage(param.getCryptedUrl());
String link = br.getRedirectLocation();
// dann scahuen wir mal was die header sagen----
decryptedLinks.add(createDownloadlink(link));

return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 4297 $");
    }

}