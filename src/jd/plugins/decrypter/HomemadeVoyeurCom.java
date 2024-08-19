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

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "homemade-voyeur.com" }, urls = { "https?://(?:www\\.)?(homemade\\-voyeur|yourvoyeurvideos)\\.com/(?:(?:tube/)?video/|tube/gallery/|\\d+/)[^/]+\\.html" })
public class HomemadeVoyeurCom extends PluginForDecrypt {
    public HomemadeVoyeurCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    /* Porn_plugin */
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(false);
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)^http://", "https://");
        br.getPage(contenturl);
        String tempID = br.getRedirectLocation();
        // Invalid link
        if ("http://www.homemade-voyeur.com/".equals(tempID) || br.containsHTML(">\\s*404 Not Found\\s*<") || br.containsHTML("<title>\\s*Homemade Voyeur - Hosted Voyeur Videos - Biggest Voyeur Vids Archive on the Net</title>") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("This video does not exist\\!< | >\\s+Video Not Found\\s+<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (tempID != null) {
            ret.add(createDownloadlink(tempID));
            return ret;
        }
        String title = br.getRegex("<meta name=\"title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (title == null) {
            title = br.getRegex("<title>Your Voyeur (Videos|Pics) \\-\\s*(.*?)\\s*</title>").getMatch(1);
            if (title == null) {
                title = br.getRegex("<title>(.+) \\- Voyeur (Videos|Pics) \\- .+</title>").getMatch(0);
            }
            if (title == null) {
                title = br.getRegex("<div class=\"titlerr\"[^>]+>([^\r\n]+)</div>").getMatch(0);
            }
        }
        if (title == null) {
            /* Fallback to url-filename */
            title = new Regex(param.getCryptedUrl(), "([A-Za-z0-9\\-]+)\\.html$").getMatch(0);
        }
        if (param.getCryptedUrl().contains("/tube/gallery/")) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title.trim());
            final DecimalFormat df = new DecimalFormat("0000");
            String[] images = br.getRegex("(https?://(www\\.)?homemade\\-voyeur\\.com/tube/images/galleries/\\d+/\\d+/[a-z0-9]{32}\\.jpg)").getColumn(0);
            for (String image : images) {
                final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(image));
                dl.setFinalFileName(title + " - " + df.format(ret.size() + 1) + image.substring(image.lastIndexOf(".")));
                fp.add(dl);
                ret.add(dl);
            }
            return ret;
        }
        tempID = br.getRegex("\"(http://api\\.slutdrive\\.com/homemadevoyeur\\.php\\?id=\\d+\\&type=v)\"").getMatch(0);
        if (tempID != null) {
            br.getPage(tempID);
            if (br.containsHTML(">404 Not Found<")) {
                logger.info("Link offline: " + param.getCryptedUrl());
                return ret;
            }
            logger.warning("Cannot handle link: " + tempID);
            logger.warning("Mainlink: " + param.getCryptedUrl());
            return null;
        }
        tempID = br.getRegex("var playlist = \\[ \\{ url: escape\\(\\'(http://[^<>\"]*?)\\'\\) \\} \\]").getMatch(0);
        if (tempID == null) {
            tempID = br.getRegex("var playlist = \\[ [^\\]]+(http://[^<>\"\\]\\}]+)").getMatch(0);
        }
        if (tempID == null) {
            tempID = br.getRegex("(\\'|\")(http://(hosted\\.yourvoyeurvideos\\.com/videos/\\d+\\.flv|[a-z0-9]+\\.yourvoyeurvideos\\.com/mp4/\\d+\\.mp4))(\\'|\")").getMatch(1);
        }
        if (tempID == null) {
            tempID = br.getRegex("file=(http[^&\"]+)").getMatch(0);
        }
        if (tempID != null && tempID.contains(".jpg")) {
            logger.info("This url is only advertising --> Offline");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Last chance - video directurl */
        if (tempID == null) {
            tempID = br.getRegex("<source type=\"video/mp4\" src=\"([^\"]+)\"").getMatch(0);
        }
        if (tempID == null) {
            tempID = br.getRegex("<source src=(?:\"|')([^\"]+)(?:\"|') type=(?:\"|')video/mp4(?:\"|')").getMatch(0);
        }
        if (tempID == null || title == null) {
            logger.info("filename: " + title + ", tempID: " + tempID);
            logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(tempID));
        dl.setReferrerUrl(br.getURL()); // Important
        dl.setFinalFileName(title.trim() + tempID.substring(tempID.lastIndexOf(".")));
        /*
         * Direct-URL may not be valid without correct referer thus let's set the upper URL as contentURL so this is used when user uses
         * "open in browser".
         */
        dl.setContentUrl(br.getURL());
        ret.add(dl);
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}