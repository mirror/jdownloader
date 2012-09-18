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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "homemade-voyeur.com" }, urls = { "http://(www\\.)?homemade\\-voyeur\\.com/(tube/video/|\\d+/)[A-Za-z0-9\\-]+\\.html" }, flags = { 0 })
public class HomemadeVoyeurCom extends PluginForDecrypt {

    public HomemadeVoyeurCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String tempID = br.getRedirectLocation();
        if ("http://www.homemade-voyeur.com/".equals(br.getRedirectLocation()) || br.containsHTML(">404 Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink(tempID));
            return decryptedLinks;
        }
        String filename = br.getRegex("class=\"he2\"><span>(.*?)</span>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Daily Free Homemade and Voyeur Videos - Beach Sex \\- Hidden Sex \\- Public Sex \\- Voyeur Videos  \\- (.*?)</title>").getMatch(0);
        tempID = br.getRegex("var playlist = \\[ \\{ url: \\'(http://.*?\\.flv)\\'").getMatch(0);
        if (tempID == null) tempID = br.getRegex("(\\'|\")(http://(hosted\\.yourvoyeurvideos\\.com/videos/\\d+\\.flv|[a-z0-9]+\\.yourvoyeurvideos\\.com/mp4/\\d+\\.mp4))(\\'|\")").getMatch(1);
        if (tempID == null || filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
        dl.setFinalFileName(filename.trim() + tempID.substring(tempID.lastIndexOf(".")));
        decryptedLinks.add(dl);

        return decryptedLinks;
    }

}
