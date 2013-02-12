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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "muchacarne.com" }, urls = { "http://(www\\.)?muchacarne\\.com/hosted\\-id\\d+\\-.*?\\.html" }, flags = { 0 })
public class MuchaCarneCom extends PluginForDecrypt {

    public MuchaCarneCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<title>([^<>\"]*?) at MuchaCarne\\.com</title>").getMatch(0);
        String tempID = br.getRedirectLocation();
        if (tempID != null) {
            DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("\\&id=(\\d+)\\&").getMatch(0);
        if (tempID == null) tempID = br.getRegex("\\&url=/videos/(\\d+)/").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("http://www.isharemybitch.com/videos/" + tempID + "/oh-lol" + new Random().nextInt(10000) + ".html");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("xvideos\\.com/(embedframe|embedcode)/(\\d+)").getMatch(1);
        if (tempID == null) tempID = br.getRegex("id_video=(\\d+)\"").getMatch(0);
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + tempID));
            return decryptedLinks;
        }
        tempID = br.getRegex("isharemybitch\\-gallery\\-(\\d+)\"").getMatch(0);
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink("http://www.isharemybitch.com/galleries/" + tempID + "/" + System.currentTimeMillis() + ".html"));
            return decryptedLinks;
        }
        // For all following ids, a filename is needed
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename.trim());
        tempID = br.getRegex("<iframe id=\"preview\" src=\"(http://gallys\\.nastydollars\\.com/[^<>\"]+)").getMatch(0);
        if (tempID != null) {
            br.getPage(tempID);
            tempID = br.getRegex("<iframe src=\"(http://[^<>\"]+)").getMatch(0);
            if (tempID != null) {
                br.getPage(tempID);
                tempID = br.getRegex("<a href=\"(http://[^<>]+\\.flv)\" id=\"media\"").getMatch(0);
                if (tempID != null) {
                    final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
                    dl.setFinalFileName(filename + ".flv");
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                }
            }
        }
        tempID = br.getRegex("url: \\'(http://static\\.crakmembers\\.com/[^<>\"]*?)\\'").getMatch(0);
        if (tempID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("xhamster\\.com/xembed\\.php\\?video=(\\d+)\"").getMatch(0);
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink("http://xhamster.com/movies/" + tempID + "/" + System.currentTimeMillis() + ".html"));
            return decryptedLinks;
        }
        if (tempID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}
