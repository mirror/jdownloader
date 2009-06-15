//    jDownloader - Downloadmanager
//    Copyright (C) 2009 JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import java.util.ArrayList;

public class ThreeArabForestCom extends PluginForDecrypt {

    public ThreeArabForestCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String linkid = new Regex(parameter, ".*?short\\.3arabforest\\.com/([^/]*)").getMatch(0);
        br.getPage("http://short.3arabforest.com/m1.php?id=" + linkid);
        String declink = br.getRegex("onclick=\"NewWindow\\('(.*?)','name'").getMatch(0);
        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(declink)));
        
        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}