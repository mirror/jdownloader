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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "homemade-voyeur.com" }, urls = { "http://(www\\.)?homemade\\-voyeur\\.com/tube/video/.*?\\.html" }, flags = { 0 })
public class HomemadeVoyeurCom extends PluginForDecrypt {

    public HomemadeVoyeurCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("class=\"he2\"><span>(.*?)</span>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Daily Free Homemade and Voyeur Videos - Beach Sex \\- Hidden Sex \\- Public Sex \\- Voyeur Videos  \\- (.*?)</title>").getMatch(0);
        String tempID = br.getRegex("var playlist = \\[ \\{ url: \\'(http://.*?\\.flv)\\'").getMatch(0);
        if (tempID == null) tempID = br.getRegex("\\'(http://hosted\\.yourvoyeurvideos\\.com/videos/\\d+\\.flv)\\'").getMatch(0);
        if (tempID == null || filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        DownloadLink dl = createDownloadlink("directhttp://" + tempID);
        dl.setFinalFileName(filename.trim() + ".flv");
        decryptedLinks.add(dl);

        return decryptedLinks;
    }

}
