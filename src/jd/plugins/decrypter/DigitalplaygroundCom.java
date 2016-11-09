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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "digitalplayground.com" }, urls = { "https?://ma\\.digitalplayground\\.com/movie/(?:(?:scenes|fullmovie)/\\d+(?:/[a-z0-9\\-_]+/?)?|galleries/\\d+(?:/[a-z0-9\\-_]+/?)?)|https?://ma\\.digitalplayground\\.com/series/episodes/\\d+(?:/[a-z0-9\\-_]+/?)?" })
public class DigitalplaygroundCom extends PluginForDecrypt {

    public DigitalplaygroundCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_VIDEO            = ".+/(?:scenes|fullmovie|episodes)/\\d+(?:/[a-z0-9\\-_]+/?)?";
    private static final String TYPE_PHOTO            = ".+/galleries/\\d+(?:/[a-z0-9\\-_]+/?)?";

    public static String        DOMAIN_BASE           = "digitalplayground.com";
    public static String        DOMAIN_PREFIX_PREMIUM = "ma.";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "/(\\d+)/?").getMatch(0);
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        final boolean fastlinkcheck = cfg.getBooleanProperty("ENABLE_FAST_LINKCHECK", true);
        // Login if possible
        if (!getUserLogin(false)) {
            logger.info("No account present --> Cannot decrypt anything!");
            return decryptedLinks;
        }
        final boolean grabMovies = parameter.matches(TYPE_VIDEO) || cfg.getBooleanProperty("AUTO_MOVIES", false);
        final boolean grabPictures = parameter.matches(TYPE_PHOTO) || cfg.getBooleanProperty("AUTO_PICTURES", false);
        String title = null;
        if (grabMovies) {
            br.getPage(getVideoUrlPremium(fid));
            if (isOffline(this.br)) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            /* 2016-11-03: Videos are not officially downloadable --> Download http streams */
            title = br.getRegex("<div class=\"player\\-title\">[\t\n\r ]*?<h1>([^<>\"]+)<").getMatch(0);
            if (title == null) {
                /* Fallback to id from inside url */
                title = fid;
            }
            title = Encoding.htmlDecode(title).trim();
            final String json = jd.plugins.decrypter.BrazzersCom.getVideoJson(this.br);
            if (json == null) {
                return null;
            }

            final LinkedHashMap<String, Object> entries = jd.plugins.decrypter.BrazzersCom.getVideoMap(json);

            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> entry = it.next();
                final String quality_key = entry.getKey();
                final String quality_url = (String) entry.getValue();
                if (!cfg.getBooleanProperty("GRAB_" + quality_key, true) || quality_url == null || !quality_url.startsWith("http")) {
                    /* Skip unwanted content */
                    continue;
                }
                final String ext = ".mp4";
                final String filename_temp = title + "_" + quality_key + ext;
                final DownloadLink dl = this.createDownloadlink(quality_url.replaceAll("https?://", "http://digitalplaygrounddecrypted"));
                dl.setLinkID(filename_temp);
                dl.setName(filename_temp);
                if (fastlinkcheck) {
                    dl.setAvailable(true);
                }
                dl.setProperty("fid", fid);
                dl.setProperty("quality", quality_key);
                decryptedLinks.add(dl);
            }
        }
        if (grabPictures) {
            br.getPage(getPicUrl(fid));
            if (isOffline(this.br)) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }

            title = br.getRegex("class=\"icon icon\\-gallery\"></span>[\t\n\r ]*?<h1><span>([^<>\"]+)</span>").getMatch(0);
            if (title == null) {
                /* Fallback to id from inside url */
                title = fid;
            }
            title = Encoding.htmlDecode(title).trim();
            final String pictures[] = getPictureArray(this.br);
            for (String finallink : pictures) {
                final String number_formatted = new Regex(finallink, "(\\d+)\\.jpg").getMatch(0);
                finallink = finallink.replaceAll("https?://", "http://digitalplaygrounddecrypted");
                final String filename_final = title + "_" + number_formatted + ".jpg";
                final DownloadLink dl = this.createDownloadlink(finallink);
                dl.setLinkID(filename_final);
                dl.setFinalFileName(filename_final);
                if (fastlinkcheck) {
                    dl.setAvailable(true);
                }
                dl.setProperty("fid", fid);
                dl.setProperty("picnumber_formatted", number_formatted);
                decryptedLinks.add(dl);
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
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
            ((jd.plugins.hoster.DigitalplaygroundCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {

            aa.setValid(false);
            return false;
        }
        return true;
    }

    public static String[] getPictureArray(final Browser br) {
        return jd.plugins.decrypter.BabesComDecrypter.getPictureArray(br);
    }

    public static String getVideoUrlPremium(final String fid) {
        return "http://" + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + "/watch/" + fid + "/";
    }

    public static String getPicUrl(final String fid) {
        return "http://" + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + "/gallery/" + fid + "/";
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PornPortal;
    }

}