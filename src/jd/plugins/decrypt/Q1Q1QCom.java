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

package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class Q1Q1QCom extends PluginForDecrypt {

    public Q1Q1QCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String id;
        String parameter = param.toString();
        id = new Regex(parameter,"q1q1q\\.com/(\\d+)").getMatch(0);
        br.getPage("http://www.q1q1q.com/show2.php?id="+id);
        String link = br.getRegex("name=\"ifram\" src=\"(.*?)\"").getMatch(0);
        decryptedLinks.add(createDownloadlink(link));
        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
