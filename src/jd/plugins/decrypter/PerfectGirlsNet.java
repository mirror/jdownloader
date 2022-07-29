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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "perfectgirls.net", "perfektdamen.co" }, urls = { "https?://([a-z]+\\.)?(perfectgirls\\.net/\\d{2,}/|(www|wwr|ipad|m)\\.perfectgirls\\.net/gal/\\d+/[A-Za-z0-9\\-_]+)", "https?://(?:www\\.)?perfektdamen\\.co/gal/\\d+/[A-Za-z0-9\\-_]+" })
public class PerfectGirlsNet extends PornEmbedParser {
    public PerfectGirlsNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    protected void correctCryptedLink(final CryptedLink param) {
        final String newurl = correctURL(param.getCryptedUrl());
        param.setCryptedUrl(newurl);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        correctCryptedLink(param);
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!br.containsHTML("<source src=[^<>]+cdn\\.perfectgirls\\.net")) {
            decryptedLinks.addAll(findEmbedUrls());
            if (!decryptedLinks.isEmpty()) {
                return decryptedLinks;
            }
        }
        final String title = br.getRegex("<title>([^<>\"]*?) ::: PERFECT GIRLS</title>").getMatch(0);
        final DownloadLink main = createDownloadlink(param.getCryptedUrl());
        if (br.containsHTML("src=\"http://(www\\.)?dachix\\.com/flashplayer/flvplayer\\.swf\"|\"http://(www\\.)?deviantclip\\.com/flashplayer/flvplayer\\.swf\"|thumbs/misc/not_available\\.gif")) {
            main.setAvailable(false);
            main.setProperty("offline", true);
        } else {
            main.setAvailable(true);
            if (title != null) {
                main.setName(Encoding.htmlDecode(title).trim() + ".mp4");
            }
        }
        decryptedLinks.add(main);
        return decryptedLinks;
    }

    public static String correctURL(final String input) {
        return input.replaceAll("(ipad|m)\\.perfectgirls\\.net/", "perfectgirls.net/").replace("perfektdamen.co/", "perfectgirls.net/");
    }

    protected boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.getRedirectLocation() != null && br.toString().length() <= 100) {
            return true;
        } else {
            return false;
        }
    }
}