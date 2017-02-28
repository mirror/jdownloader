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
import java.util.List;
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
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "digitalplayground.com" }, urls = { "https?://ma\\.digitalplayground\\.com/(?!model/)[a-z]+(?:/[a-z]+)?/\\d+(?:/[a-z0-9\\-_]+/?)?|https?://ma\\.digitalplayground\\.com/[^/]+/galleries/\\d+(?:/[a-z0-9\\-_]+/?)?|https?://www\\.digitalplayground\\.com/[a-z]+(?:/[a-z]+)?/trailer/\\d+/[a-z0-9\\-_]+/?" })
public class DigitalplaygroundCom extends PluginForDecrypt {

    public DigitalplaygroundCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static String DOMAIN_BASE           = "digitalplayground.com";
    public static String DOMAIN_PREFIX_PREMIUM = "ma.";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        /*
         * ID - usually bound to the content type. If we have a movie, we can also use it to grab the corresponding gallery but we cannot
         * use the gallery ID to grab movies!
         */
        final String fid_of_current_contenttype = getFID(parameter);
        String fid_movie = null;
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        final boolean fastlinkcheck = cfg.getBooleanProperty("ENABLE_FAST_LINKCHECK", true);
        final String host_decrypted = "http://digitalplaygrounddecrypted";
        String title = null;
        String ext = ".mp4";
        final boolean isPhotoUrl = parameter.matches(".+/galleries/\\d+(?:/[a-z0-9\\-_]+/?)?");
        final boolean isTrailerUrl = isTrailerUrl(parameter);

        if (!isPhotoUrl && !isTrailerUrl) {
            /* Current contenttype is a movie --> We have the required ID already. */
            fid_movie = fid_of_current_contenttype;
        }

        final PluginForHost hostPlugin = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        final List<Account> moch_accounts = AccountController.getInstance().getMultiHostAccounts(this.getHost());

        if (aa == null && (moch_accounts == null || moch_accounts.size() == 0)) {
            logger.info("Account needed to use this crawler for this linktype (videos)");
            return decryptedLinks;
        }
        if (aa == null && moch_accounts != null && moch_accounts.size() > 0) {
            /* Only MOCH download possible --> Add (dummy) link for hostplugin */
            this.br.getPage(getVideoUrlFree(parameter));
            if (isOffline(this.br)) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            title = getTitleVideo(this.br, fid_of_current_contenttype);
            final String quality_key = "720p_4000";
            final DownloadLink dl = this.createDownloadlink(host_decrypted + "ma.digitalplayground.com/download/" + fid_of_current_contenttype + "/" + quality_key + "/");
            final String filename_temp = title + "_" + quality_key + ext;
            dl.setProperty("quality", quality_key);
            dl.setProperty("fid", fid_of_current_contenttype);
            dl.setProperty("mainlink", getVideoUrlFree(parameter));
            dl.setAvailable(true);
            dl.setName(filename_temp);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else {
            /* Premium users shall not add free urls - that would make no sense at all. */
            if (isTrailerUrl) {
                logger.info("Unsupported linktype (trailer) - premium users should add premium urls only.");
                return decryptedLinks;
            }
            /* Normal host account is available - perform login. */
            ((jd.plugins.hoster.DigitalplaygroundCom) hostPlugin).login(this.br, aa, false);
        }
        final boolean grabPictures = isPhotoUrl || cfg.getBooleanProperty("AUTO_PICTURES", false);
        final boolean grabMovies = !grabPictures || cfg.getBooleanProperty("AUTO_MOVIES", false);
        String linkid = null;
        if (grabPictures) {
            br.getPage(getPicUrl(fid_of_current_contenttype));
            if (isOffline(this.br)) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            if (fid_movie == null) {
                fid_movie = PluginJSonUtils.getJsonValue(this.br, "objectId");
                if (fid_movie == null) {
                    fid_movie = this.br.getRegex("class=\"\\s*?first\\s*?\">\\s*?<a  href=\"/[^\"\\']+/(\\d+)/[^\"\\']+\"").getMatch(0);
                }
            }
            String finallink;
            title = br.getRegex("class=\"icon icon\\-gallery\"></span>[\t\n\r ]*?<h1><span>([^<>\"]+)</span>").getMatch(0);
            if (title == null) {
                /* Fallback to id from inside url */
                title = fid_of_current_contenttype;
            }
            title = Encoding.htmlDecode(title).trim();
            if (cfg.getLongProperty("PREFERRED_PICTURE_FILE_TYPE", 0) == 0) {
                ext = ".zip";
                finallink = getPicArchiveDownloadlink(this.br);
                final String number_formatted = new Regex(finallink, "(\\d+)\\.zip").getMatch(0);
                finallink = finallink.replaceAll("https?://", host_decrypted);
                final String filename_final = title + "_" + number_formatted + ext;
                linkid = fid_of_current_contenttype + "_" + number_formatted + ext;
                final DownloadLink dl = this.createDownloadlink(finallink);
                dl.setLinkID(linkid);
                dl.setFinalFileName(filename_final);
                if (fastlinkcheck) {
                    dl.setAvailable(true);
                }
                dl.setProperty("fid", fid_of_current_contenttype);
                dl.setProperty("picnumber_formatted", number_formatted);
                decryptedLinks.add(dl);
            } else {
                ext = ".jpg";
                final String pictures[] = getPictureArray(this.br);
                for (final String final_link : pictures) {
                    final String number_formatted = new Regex(final_link, "(\\d+)\\.jpg").getMatch(0);
                    finallink = final_link.replaceAll("https?://", host_decrypted);
                    final String filename_final = title + "_" + number_formatted + ext;
                    linkid = fid_of_current_contenttype + "_" + number_formatted + ext;
                    final DownloadLink dl = this.createDownloadlink(finallink);
                    dl.setLinkID(linkid);
                    dl.setFinalFileName(filename_final);
                    if (fastlinkcheck) {
                        dl.setAvailable(true);
                    }
                    dl.setProperty("fid", fid_of_current_contenttype);
                    dl.setProperty("picnumber_formatted", number_formatted);
                    dl.setProperty("mainlink", parameter);
                    decryptedLinks.add(dl);
                }
            }
        }
        if (grabMovies && fid_movie != null) {
            br.getPage(getVideoUrlPremium(fid_movie));
            if (isOffline(this.br)) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            /* 2016-11-03: Videos are not officially downloadable --> Download http streams */
            title = getTitleVideo(this.br, fid_of_current_contenttype);
            final String json = jd.plugins.decrypter.BrazzersCom.getVideoJson(this.br);
            if (json == null) {
                return null;
            }

            final LinkedHashMap<String, Object> entries = jd.plugins.decrypter.WickedCom.getVideoMapHttpStream(json);

            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> entry = it.next();
                final String quality_key = entry.getKey();
                final String quality_url = (String) entry.getValue();
                if (!cfg.getBooleanProperty("GRAB_" + quality_key, true) || quality_url == null || !quality_url.startsWith("http")) {
                    /* Skip unwanted content */
                    continue;
                }
                final String filename_temp = title + "_" + quality_key + ext;
                linkid = fid_of_current_contenttype + "_" + quality_key + ext;
                final DownloadLink dl = this.createDownloadlink(quality_url.replaceAll("https?://", host_decrypted));
                dl.setLinkID(linkid);
                dl.setName(filename_temp);
                if (fastlinkcheck) {
                    dl.setAvailable(true);
                }
                dl.setProperty("fid", fid_movie);
                dl.setProperty("quality", quality_key);
                dl.setProperty("mainlink", parameter);
                decryptedLinks.add(dl);
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public static String getFID(final String url) {
        return new Regex(url, "/(\\d+)/?").getMatch(0);
    }

    public static boolean isTrailerUrl(final String url) {
        return url != null && url.matches("https?://(?:www\\.)?[^/]+/.+/trailer/\\d+(?:/[a-z0-9\\-_]+/?)?");
    }

    public static String getTitleVideo(final Browser br, final String fallback) {
        String title_video = br.getRegex("<div class=\"player\\-title\">[\t\n\r ]*?<h1>([^<>\"]+)<").getMatch(0);
        if (title_video == null) {
            title_video = fallback;
        }
        title_video = Encoding.htmlDecode(title_video).trim();
        return title_video;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    public static String[] getPictureArray(final Browser br) {
        return jd.plugins.decrypter.BabesComDecrypter.getPictureArray(br);
    }

    public static String getVideoUrlFree(final String url_original) {
        String videourl_free;
        if (isTrailerUrl(url_original)) {
            /* Nothing to do */
            videourl_free = url_original;
        } else {
            /* Change premium url to free --> This might not always result in a valid url! */
            final String fid = getFID(url_original);
            final Regex urlinfo = new Regex(url_original, "https?://[^/]+/(.*?)/\\d+(/.*?)?");
            final String urlpart = urlinfo.getMatch(0);
            final String url_ending = urlinfo.getMatch(1);
            if (urlpart != null && fid != null) {
                videourl_free = "http://www." + DOMAIN_BASE + "/" + urlpart + "/trailer/" + fid;
                if (url_ending != null) {
                    videourl_free += url_ending;
                }
            } else {
                /* Fallback */
                videourl_free = url_original;
            }
        }
        return videourl_free;
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

    public static String getPicArchiveDownloadlink(final Browser br) {
        return br.getRegex("\"(https?://[^<>\"]+\\d+\\.zip[^<>\"]*?)\"").getMatch(0);
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