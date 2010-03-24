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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adf.ly", "download.su", "wowebook.com", "link.songs.pk + songspk.info", "imageto.net", "clubteam.eu", "jforum.uni.cc", "linksole.com" }, urls = { "http://[\\w\\.]*?adf\\.ly/[A-Za-z0-9]+", "http://[\\w\\.]*?download\\.su/go/\\?id=.*?=/files/\\d+/.+", "http://[\\w\\.]*?wowebook\\.com/(e-|non-e-)book/.*?/.*?\\.html", "http://[\\w\\.]*?(link\\.songs\\.pk/(popsong|song1|bhangra)\\.php\\?songid=|songspk\\.info/ghazals/download/ghazals\\.php\\?id=)[0-9]+", "http://[\\w\\.]*?imageto\\.net/(\\?v=|images/)[0-9a-z]+\\..{2,4}", "http://[\\w\\.]*?clubteam\\.eu/dl\\.php\\?id=\\d+\\&c=[a-zA-z0-9=]+", "http://[\\w\\.]*?jforum\\.uni\\.cc/protect/\\?r=[a-z0-9]+", "http://[\\w\\.]*?linksole\\.com/[0-9a-z]+" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0 })
public class DecrypterForRedirectServicesWithoutDirectRedirects extends PluginForDecrypt {

    public DecrypterForRedirectServicesWithoutDirectRedirects(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        String finallink = null;
        if (!parameter.contains("imageto.net/")) br.getPage(parameter);
        if (parameter.contains("adf.ly"))
            finallink = br.getRegex("var target_url = '(http.*?)'").getMatch(0);
        else if (parameter.contains("download.su")) {
            String rspart = new Regex(parameter + "\"", "(/files.*?)\"").getMatch(0);
            finallink = "http://rapidshare.com" + rspart;
        } else if (parameter.contains("link.songs.pk/") || parameter.contains("songspk.info/ghazals/download/ghazals.php?id=")) {
            finallink = br.getRedirectLocation();
            if (finallink != null) finallink = "directhttp://" + finallink;
        } else if (parameter.contains("imageto")) {
            if (parameter.contains("imageto.net/images/")) {
                finallink = "directhttp://" + parameter;
            } else {
                String fileid = new Regex(parameter, "imageto\\.net/\\?v=(.+)").getMatch(0);
                finallink = "directhttp://http://imageto.net/images/" + fileid;
            }
        } else if (parameter.contains("clubteam.eu/dl")) {
            finallink = br.getRegex("content='0; url=(.*?)'>").getMatch(0);
        } else if (parameter.contains("wowebook.com")) {
            String redirectLink = br.getRegex("\"(http://www\\.wowebook\\.com/download.*?)\"").getMatch(0);
            if (redirectLink == null) return null;
            br.getPage(redirectLink);
            finallink = br.getRedirectLocation();
        } else if (parameter.contains("jforum.uni.cc/")) {
            finallink = br.getRegex("<frame name=\"page\" src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("linksole.com/")) {
            finallink = br.getRegex("linkRefererUrl = '(.*?)';").getMatch(0);
            if (finallink == null) finallink = br.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
        }
        if (finallink == null) {
            logger.info("DecrypterForRedirectServicesWithoutDirectRedirects says \"Out of date\" for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
