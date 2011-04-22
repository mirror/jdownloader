//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixcloud.com" }, urls = { "http://(www\\.)?mixcloud\\.com/.*?/[A-Za-z0-9_\\-]+/" }, flags = { 0 })
public class MxCloudCom extends PluginForDecrypt {

    public MxCloudCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String theName = br.getRegex("class=\"cloudcast\\-title\">(.*?)</h1>").getMatch(0);
        br.getPage("http://www.mixcloud.com/api/1/cloudcast/" + new Regex(parameter, "mixcloud\\.com/(.*?/[A-Za-z0-9_-]+)/").getMatch(0) + ".json");
        if (theName == null) theName = br.getRegex("color:#02a0c7; font-weight:bold;\\\\\\\">(.*?)</a>").getMatch(0);
        if (theName == null) return null;
        String sets[] = br.getRegex("\\[[\t\n\r ]+(\".*?\")[\t\n\r ]+\\]").getColumn(0);
        if (sets == null || sets.length == 0) return null;
        for (String set : sets) {
            String[] links = new Regex(set, "\"(.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String dl : links) {
                DownloadLink dlink = createDownloadlink("directhttp://" + dl);
                dlink.setFinalFileName(theName + new Regex(dl, "(\\..{3}$)").getMatch(0));
                decryptedLinks.add(dlink);
            }
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(theName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
