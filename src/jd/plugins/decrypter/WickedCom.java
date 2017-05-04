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

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wicked.com" }, urls = { "https?://ma\\.wicked\\.com/(?:watch/\\d+(?:/[a-z0-9\\-_]+/?)?|galleries/\\d+(?:/[a-z0-9\\-_]+/?)?)|https?://(?:www\\.)?wicked\\.com/tour/movie/\\d+(?:/[a-z0-9\\-_]+/?)?" })
public class WickedCom extends PluginForDecrypt {
    public WickedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static String DOMAIN_BASE           = "wicked.com";
    public static String DOMAIN_PREFIX_PREMIUM = "ma.";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "/(\\d+)/?").getMatch(0);
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        final boolean is_logged_in = getUserLogin(false);
        // Login if possible
        if (is_logged_in) {
            br.getPage(parameter);
        } else {
            /* 2016-11-03: Free users can download galleries without account. */
            br.getPage(getVideoUrlFree(fid));
        }
        final String redirect = this.br.getRedirectLocation();
        if (isOffline(this.br)) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String title = null;
        if (!this.br.containsHTML("class=\"galleryui\\-container\"")) {
            /* 2016-11-03: Videos are not officially downloadable --> Download http streams */
            if (!is_logged_in && redirect != null && redirect.contains("/access/login")) {
                /* No title available */
            } else {
                if (title == null) {
                    if (is_logged_in) {
                        title = br.getRegex("class=\"icon icon_watching\"></span>\\s*?<h4>You are watching: ([^<>]+)<").getMatch(0);
                    } else {
                        title = br.getRegex("class=\"showcase\\-video\\-player__title\">([^<>]+)<").getMatch(0);
                    }
                }
            }
            if (title == null) {
                /* Fallback to id from inside url */
                title = fid;
            }
            LinkedHashMap<String, Object> entries;
            if (is_logged_in) {
                final String json = jd.plugins.decrypter.BrazzersCom.getVideoJson(this.br);
                if (json == null) {
                    return null;
                }
                entries = getVideoMapHttpStream(json);
            } else {
                /* We're not logged in but maybe the user has an account to download later. */
                entries = getDummyQualityMap(fid);
            }
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
                final DownloadLink dl = this.createDownloadlink(quality_url.replaceAll("https?://", "http://wickeddecrypted"));
                dl.setName(title + "_" + quality_key + ext);
                dl.setProperty("fid", fid);
                dl.setProperty("quality", quality_key);
                decryptedLinks.add(dl);
            }
        } else {
            if (title == null) {
                title = br.getRegex("<h4>Picture Gallery: ([^<>\"]+)<").getMatch(0);
            }
            if (title == null) {
                /* Fallback to id from inside url */
                title = fid;
            }
            final String pictures[] = getPictureArray(this.br);
            for (String finallink : pictures) {
                final String number_formatted = new Regex(finallink, "(\\d+)\\.jpg").getMatch(0);
                finallink = finallink.replaceAll("https?://", "http://wickeddecrypted");
                final DownloadLink dl = this.createDownloadlink(finallink);
                dl.setFinalFileName(title + "_" + number_formatted + ".jpg");
                dl.setAvailable(true);
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

    private LinkedHashMap<String, Object> getDummyQualityMap(final String fid) {
        final List<Account> moch_accounts = AccountController.getInstance().getMultiHostAccounts(this.getHost());
        final boolean moch_account_available = moch_accounts != null && moch_accounts.size() > 0;
        final String[] possibleQualityKeys = { "-480x272_H264", "-640x368_H264", "-176x144_H263", "-128x96_H263", "_256p_600", "_400p_1300", "_720p_2500", "_1080p_6000" };
        int counter = 0;
        final int place_of_last_item = possibleQualityKeys.length - 1;
        LinkedHashMap<String, Object> dummymap = new LinkedHashMap<String, Object>();
        for (final String possibleQualityKey : possibleQualityKeys) {
            /*
             * E.g.
             * http://http.movies.wickedcdn.com/44177/vids/wkd_teen_yoga_scene_1-480x272_H264.mp4?nvb=20161102203730&nva=20161103023730&
             * hash=077f0507af518ba62a11d
             */
            if (moch_account_available && counter == place_of_last_item) {
                /* MOCH account available --> Only add one dummy quality. */
                dummymap.clear();
            }
            dummymap.put(possibleQualityKey, String.format("http://http.movies.wickedcdn.com/%s/vids/%s.mp4", fid, possibleQualityKey));
            counter++;
        }
        return dummymap;
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
            ((jd.plugins.hoster.WickedCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    public static LinkedHashMap<String, Object> getVideoMapHttpStream(final String json_source) {
        LinkedHashMap<String, Object> entries = null;
        try {
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
            entries = (LinkedHashMap<String, Object>) entries.get("stream_info");
        } catch (final Throwable e) {
        }
        return jd.plugins.decrypter.BrazzersCom.getVideoMapHttpStreams(entries);
    }

    public static String[] getPictureArray(final Browser br) {
        return jd.plugins.decrypter.BabesComDecrypter.getPictureArray(br);
    }

    public static String getVideoUrlFree(final String fid) {
        return "http://www." + DOMAIN_BASE + "/tour/movie/" + fid + "/";
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