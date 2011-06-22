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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sound.to" }, urls = { "http://(www\\.)?sound\\.to/(en|de|fr)/music/download/files/.+" }, flags = { 0 })
public class SndTo extends PluginForDecrypt {

    public SndTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replaceAll("/(fr|de)/", "/en/");
        br.setFollowRedirects(false);
        br.setCookie("http://www.sound.to/", "language", "en");
        br.getPage(parameter);
        String fpName = br.getRegex("<title>Download: (.*?) \\- sound\\.to \\- Free album downloads,").getMatch(0);
        if (fpName == null) fpName = br.getRegex("class=\"openside\">Download: (.*?)</div>").getMatch(0);
        String[] links = br.getRegex("target=\\'_blank\\' href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String aLink : links) {
            DownloadLink dl = createDownloadlink(aLink);
            dl.addSourcePluginPassword("sound.to");
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
