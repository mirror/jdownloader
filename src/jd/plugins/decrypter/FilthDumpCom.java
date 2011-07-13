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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filthdump.com" }, urls = { "http://(www\\.)?filthdump\\.com/\\d+/.*?\\.html" }, flags = { 0 })
public class FilthDumpCom extends PluginForDecrypt {

    public FilthDumpCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<title>(.*?) :: Amateur Porn </title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            logger.warning("Couldn't decrypt link: " + parameter);
            return null;
        }
        filename = filename.trim();
        String tempID = br.getRegex("\\?settings=(http://(www\\.)?(tube\\.)?watchgfporn\\.com/playerConfig\\.php\\?.*?)\"").getMatch(0);
        if (tempID != null) {
            br.setFollowRedirects(true);
            br.getPage(tempID);
            String finallink = br.getRegex("defaultVideo:(http://.*?);").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("dump1\\.com/flv_player/data/playerConfig(Embed)?/(\\d+)").getMatch(1);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("http://www.dump1.com/media/" + tempID + "/");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("addParam\\(\\'flashvars\\',\\'\\&file=(http://video\\.teensexmovs\\.com/.*?)\\&").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("flvURL=(.*?)\\&destinationURL").getMatch(0);
        if (tempID != null) {
            tempID = Encoding.Base64Decode(tempID);
            if (tempID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (tempID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}
