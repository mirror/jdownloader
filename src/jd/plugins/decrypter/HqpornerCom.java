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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hqporner.com", "hqpornerpro.com" }, urls = { "https?://(?:www\\.)?hqporner\\.com/hdporn/\\d+\\-([^/]+)\\.html", "https?://(?:www\\.)?hqpornerpro\\.com/([^/]+)" })
public class HqpornerCom extends PornEmbedParser {
    public HqpornerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    protected Browser prepareBrowser(final Browser br) {
        /* 2019-01-24: Important! some URLs will just display 404 if Referer is not set! */
        br.getHeaders().put("Referer", "https://" + this.getHost() + "/");
        return br;
    }

    /* DEV NOTES */
    /* Porn_plugin */
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = super.decryptIt(param, progress);
        final String titleFromURL = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0).replace("-", " ").trim();
        final String[][] parsedNames = br.getRegex("/actress/[^\"]+\"[^>]+>([^<>\"]+)</a>").getMatches();
        final StringBuilder names = new StringBuilder();
        if (parsedNames != null) {
            for (final String parsedName[] : parsedNames) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(parsedName[0]);
            }
        }
        final String accressName = names.toString();
        String title = br.getRegex("<h1 class=\"main\\-h1\" style=\"line\\-height: 1em;\">\\s*?([^<>\"]+)</h1>").getMatch(0);
        if (title == null) {
            /* Fallback */
            title = titleFromURL;
        }
        title = Encoding.htmlDecode(title).trim();
        if (decryptedLinks.size() > 0) {
            /* Most of all times we grab mydaddy.cc URLs which need some special properties. */
            for (final DownloadLink link : decryptedLinks) {
                /* Set special properties for madaddy.cc URLs */
                final String host = Browser.getHost(link.getPluginPatternMatcher());
                if (host.equals(jd.plugins.hoster.MydaddyCc.getPluginDomains().get(0)[0])) {
                    if (!StringUtils.isEmpty(accressName)) {
                        link.setProperty(jd.plugins.hoster.MydaddyCc.PROPERTY_ACTRESS_NAME, accressName);
                    }
                    link.setProperty(jd.plugins.hoster.MydaddyCc.PROPERTY_CRAWLER_TITLE, title);
                }
            }
        } else {
            /* E.g. hqpornerpro.com */
            final String selfhostedEmbedURL = br.getRegex("\"(/player/video\\.php\\?id=[a-f0-9]+)\"").getMatch(0);
            if (selfhostedEmbedURL == null) {
                return null;
            }
            br.getPage(selfhostedEmbedURL);
            String bestQualityDownloadurl = null;
            int maxWidth = -1;
            final String[] directurls = br.getRegex("data-fluid-hd[^>]*src='(https?://[^<>\\'\"]+\\d+\\.mp4)'").getColumn(0);
            if (directurls.length == 0) {
                return null;
            }
            for (final String directurl : directurls) {
                final String widthStr = new Regex(directurl, "(\\d+)\\.mp4").getMatch(0);
                final int width = Integer.parseInt(widthStr);
                if (width > maxWidth) {
                    maxWidth = width;
                    bestQualityDownloadurl = directurl;
                }
            }
            final DownloadLink direct = this.createDownloadlink(bestQualityDownloadurl);
            if (!StringUtils.isEmpty(accressName) && !title.contains(accressName)) {
                direct.setFinalFileName(accressName + " - " + title + ".mp4");
            } else {
                direct.setFinalFileName(title + ".mp4");
            }
            direct.setAvailable(true);
            decryptedLinks.add(direct);
        }
        return decryptedLinks;
    }

    @Override
    protected boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else {
            return false;
        }
    }
}
