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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "javmon.com" }, urls = { "https?://(www\\.)?(javmon|jamo)\\.(com|tv)/(online\\-\\d+/)?(video|movie|clip)\\-\\d+(/|\\-)[a-z0-9\\-_]+\\.html" }, flags = { 0 })
public class JavMonCom extends PluginForDecrypt {

    public JavMonCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("javmon.com", "jamo.tv");
        br.getPage(parameter);
        if (br.containsHTML("fastpic.ru/big/2014/0907/ae")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String filename = br.getRegex("top-title\">(JAV Movie Uncensored )?(.*?)<").getMatch(1);
        if (parameter.contains("/movie")) {
            String video = br.getRegex("(https?://jamo.tv/online.*?)\"").getMatch(0);
            br.getPage(video);
        }
        if (br.containsHTML("jamo.tv/embed/")) {
            String embed = br.getRegex("(https?://jamo.tv/embed/.*?)(\"|\')").getMatch(0);
            br.getPage(embed);
        }
        String externID = br.getRegex("<iframe.*? src=(\"|\')(https?.*?)(\"|\')").getMatch(1);
        if (externID == null) {
            if (!br.containsHTML("s1\\.addParam\\(\\'flashvars\\'")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (externID.contains("cloudtime.to/embed/")) {
            String vid = br.getRegex("https?://(www.)?cloudtime.to/embed/\\?v=(.*)").getMatch(1);
            externID = "http://www.cloudtime.to/video/" + vid;
        }
        logger.info("externID: " + externID);
        externID = Encoding.htmlDecode(externID);
        DownloadLink dl = createDownloadlink(externID);
        dl.setFinalFileName(filename + ".mp4");
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

}