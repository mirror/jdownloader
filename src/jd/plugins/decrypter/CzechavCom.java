//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "czechav.com" }, urls = { "https?://(?:www\\.)?czechav\\.com/(?:de|en)/video/[a-z0-9\\-]+\\-\\d+/?" })
public class CzechavCom extends PluginForDecrypt {

    public CzechavCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected DownloadLink createDownloadlink(String url, final String fid, final String urlpart, final String quality) {
        url = url.replaceAll("https?://", "http://czechavdecrypted");
        final DownloadLink dl = super.createDownloadlink(url, true);
        dl.setProperty("fid", fid);
        dl.setProperty("quality", quality);
        return dl;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String urlpart = new Regex(parameter, "/video/([a-z0-9\\-]+)").getMatch(0);
        final String fid = new Regex(parameter, "(\\d+)/?$").getMatch(0);
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        final boolean fastlinkcheck = cfg.getBooleanProperty("FAST_LINKCHECK", true);
        final boolean is_logged_in = getUserLogin(false);
        if (!is_logged_in) {
            logger.info("Account required");
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (isOffline(this.br)) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String title = this.br.getRegex("<h1>([^<>\"]+)</h1>").getMatch(0);
        if (title == null) {
            /* Fallback to id from inside url */
            title = fid;
        }
        final String[] videourls = getVideourls(this.br);
        for (String videourl : videourls) {
            videourl = "https://" + this.getHost() + videourl;
            final String quality_url = new Regex(videourl, "(\\d+x\\d+)").getMatch(0);
            if (quality_url == null || !cfg.getBooleanProperty("GRAB_" + quality_url, true)) {
                continue;
            }
            final String ext = ".mp4";
            final DownloadLink dl = this.createDownloadlink(videourl, fid, urlpart, quality_url);
            if (fastlinkcheck) {
                dl.setAvailable(true);
            }
            dl.setName(title + "_" + quality_url + ext);
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public static String[] getVideourls(final Browser br) {
        return br.getRegex("\"(/[^/]+/video/[^/]+/download/video\\-\\d+x\\d+\\.mp4)\"").getColumn(0);
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.CzechavCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}