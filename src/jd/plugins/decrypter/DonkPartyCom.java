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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "donkparty.com" }, urls = { "https?://(?:www\\.)?donkparty\\.com/videos/([a-z0-9\\-_]+)_(\\d+)" })
public class DonkPartyCom extends PornEmbedParser {
    public DonkPartyCom(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    /* Porn_plugin */

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        final String url = param.toString() + "/";
        br.getPage(url);
        String tempID = br.getRedirectLocation();
        if (tempID != null) {
            final DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML("Media not found\\!<") || br.containsHTML("<title> free sex video \\- DonkParty</title>") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = new Regex(url, this.getSupportedLinks()).getMatch(0).replace("-", " ");
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = Encoding.htmlDecode(title).trim();
        String sources = br.getRegex("sources\":\\[\\{\"src\":\"(.*?)\"").getMatch(0);
        if (sources != null) {
            String finallink = sources;
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(title + ".mp4");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("settings=(http://secret\\.shooshtime\\.com/playerConfig\\.php?.*?)\"").getMatch(0);
        if (tempID != null) {
            br.getPage(tempID);
            String finallink = br.getRegex("defaultVideo:(http://.*?);").getMatch(0);
            if (finallink == null) {
                throw new DecrypterException("Decrypter broken for link: " + url);
            }
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(title + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        String iframe = br.getRegex("<iframe src=\"([^<>\"]+)\"[^<>]*allowfullscreen[^<>]*").getMatch(0);
        if (iframe != null) {
            decryptedLinks.add(createDownloadlink(iframe));
            return decryptedLinks;
        }
        decryptedLinks.addAll(findEmbedUrls(title));
        if (decryptedLinks.isEmpty()) {
            throw new DecrypterException("Decrypter broken for link: " + url);
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}