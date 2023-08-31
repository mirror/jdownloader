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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.BangbrosCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bangbrosold.com", "mygf.com" }, urls = { "https?://members\\.bangbros\\.com/product/\\d+/movie/\\d+|https?://(?:bangbrothers\\.(?:com|net)|bangbros\\.com)/video\\d+/[a-z0-9\\-]+", "https?://members\\.mygf\\.com/product/\\d+/movie/\\d+" })
public class BangbrosComCrawler extends PluginForDecrypt {
    public BangbrosComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static final String type_userinput_video_couldbe_trailer = ".+/video\\d+/[a-z0-9\\-]+";
    public static final String type_decrypted_zip                   = ".+\\.zip.*?";

    /**
     * 2023-06-26: old.bangbros.com was added as they've moved to a new system ("pornportal") which is incompatible with the old stuff thus
     * the "old." domain is used JDownloader-plugin-internally.
     */
    protected DownloadLink createDownloadlink(String url, final String fid, final String productid, final String quality) {
        final String hostpart = BangbrosCom.getHostInternal(this.getHost()).split("\\.")[0];
        url = url.replaceAll("https?://", hostpart + "decrypted://");
        final DownloadLink dl = super.createDownloadlink(url, true);
        dl.setProperty(BangbrosCom.PROPERTY_FID, fid);
        if (quality != null) {
            dl.setProperty(BangbrosCom.PROPERTY_QUALITY, quality);
        }
        if (productid != null) {
            dl.setProperty(BangbrosCom.PROPERTY_PRODUCT_ID, productid);
        }
        dl.setProperty(BangbrosCom.PROPERTY_MAINLINK, br.getURL());
        return dl;
    }

    protected DownloadLink createDownloadlink(String url, final String fid) {
        return createDownloadlink(url, fid, null, null);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return decryptIt(param, account, SubConfiguration.getConfig(this.getHost()));
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final Account account, final SubConfiguration cfg) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl().replaceFirst("bangbrothers\\.com/", "bangbros.com/");
        final boolean loginRequired = requiresAccount(parameter);
        if (loginRequired && account == null) {
            throw new AccountRequiredException();
        }
        final BangbrosCom hostPlugin = (BangbrosCom) this.getNewPluginForHostInstance(this.getHost());
        if (account != null) {
            hostPlugin.login(this.br, account, false);
        }
        final boolean preferServersideOriginalFilenames = hostPlugin.getPluginConfig().getBooleanProperty("PREFER_ORIGINAL_FILENAMES", false);
        br.getPage(parameter);
        if (isOffline(this.br, parameter)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = null;
        String cast_comma_separated = null;
        String description = null;
        final String cast_html = br.getRegex("<span class=\"tag\">Cast:(.*?)</span>").getMatch(0);
        if (cast_html != null) {
            final String[] castMembers = new Regex(cast_html, "class=\"tagB\">([^<]*)<").getColumn(0);
            if (castMembers.length > 0) {
                cast_comma_separated = "";
                for (String castMember : castMembers) {
                    castMember = castMember.trim();
                    if (cast_comma_separated.length() > 0) {
                        cast_comma_separated += ",";
                    }
                    cast_comma_separated += castMember;
                }
            }
        }
        final Regex finfo = new Regex(parameter, "product/(\\d+)/movie/(\\d+)");
        final String productid = finfo.getMatch(0);
        String fid = finfo.getMatch(1);
        final String directurl_photos = regexZipUrl(this.br, "pictures");
        String directurl_screencaps = regexZipUrl(this.br, "screencaps");
        if (directurl_screencaps != null) {
            /* Protocol might sometimes be missing! */
            directurl_screencaps = br.getURL(directurl_screencaps).toString();
        }
        title = this.br.getRegex("class=\"vdo\\-hdd1\">([^<>\"]+)<").getMatch(0);
        if (title == null) {
            title = this.br.getRegex("class=\"desTxt\\-hed\">([^<>\"]+)<").getMatch(0);
        }
        String titleForPlugin = null;
        final String trailerJsonEncoded = br.getRegex("class=\"pBtn overlay-trailer-opener\" data-shoot=\"(\\{[^\"]+)").getMatch(0);
        DownloadLink trailer = null;
        if (trailerJsonEncoded != null) {
            final Map<String, Object> entries = restoreFromString(Encoding.htmlDecode(trailerJsonEncoded), TypeRef.MAP);
            trailer = this.createDownloadlink(entries.get("trailer").toString());
            if (title == null) {
                title = entries.get("title").toString();
            }
            description = (String) entries.get("description");
        }
        if (title != null) {
            title = Encoding.htmlDecode(title);
            titleForPlugin = title;
            if (cast_comma_separated != null) {
                titleForPlugin = cast_comma_separated + "_" + titleForPlugin;
            }
        } else {
            /* Fallback to id from inside url */
            title = fid;
            titleForPlugin = title;
        }
        /* 2019-01-29: Content-Servers are very slow */
        final boolean fast_linkcheck = true;
        final String[] videourls = br.getRegex("(/product/\\d+/movie/\\d+/\\d+p)").getColumn(0);
        int numberOfAddedVideos = 0;
        if (videourls != null && videourls.length > 0) {
            int heightMax = -1;
            DownloadLink best = null;
            for (String videourl : videourls) {
                videourl = br.getURL(videourl).toString();
                final String qualityHeightStr = new Regex(videourl, "(\\d+)p").getMatch(0);
                final int qualityHeight = Integer.parseInt(qualityHeightStr);
                final String qualityIdentifier = qualityHeightStr + "p";
                final String streamingURL = regexStreamingURL(br, qualityIdentifier);
                final String ext = ".mp4";
                final DownloadLink video = this.createDownloadlink(videourl, fid, productid, qualityIdentifier);
                if (streamingURL != null) {
                    video.setProperty(BangbrosCom.PROPERTY_STREAMING_DIRECTURL, streamingURL);
                }
                final String pluginFilename = titleForPlugin + "_" + qualityIdentifier + ext;
                if (preferServersideOriginalFilenames) {
                    String originalFilename = null;
                    if (streamingURL != null) {
                        originalFilename = Plugin.getFileNameFromURL(streamingURL);
                    }
                    if (originalFilename != null) {
                        video.setFinalFileName(originalFilename);
                    } else {
                        /* Fallback */
                        video.setName(pluginFilename);
                    }
                } else {
                    video.setFinalFileName(pluginFilename);
                }
                if (fast_linkcheck) {
                    video.setAvailable(true);
                }
                /* Collect best */
                if (best == null || qualityHeight > heightMax) {
                    heightMax = qualityHeight;
                    best = video;
                }
                if (cfg != null && !cfg.getBooleanProperty("GRAB_" + qualityIdentifier, true)) {
                    continue;
                }
                ret.add(video);
            }
            if (cfg != null && cfg.getBooleanProperty(BangbrosCom.SETTING_BEST_ONLY, BangbrosCom.default_SETTING_BEST_ONLY)) {
                ret.clear();
                ret.add(best);
            }
            numberOfAddedVideos = ret.size();
        }
        if ((cfg == null || cfg.getBooleanProperty("GRAB_photos", false)) && directurl_photos != null) {
            final String quality = "pictures";
            final DownloadLink dl = this.createDownloadlink(directurl_photos, fid, productid, quality);
            final String pluginFilename = titleForPlugin + "_" + quality + ".zip";
            if (preferServersideOriginalFilenames) {
                String originalFilename = UrlQuery.parse(directurl_photos).get("filename");
                if (originalFilename == null) {
                    originalFilename = Plugin.getFileNameFromURL(directurl_photos);
                }
                if (originalFilename != null) {
                    dl.setFinalFileName(originalFilename);
                } else {
                    /* Fallback */
                    dl.setName(pluginFilename);
                }
            } else {
                dl.setFinalFileName(pluginFilename);
            }
            if (fast_linkcheck) {
                dl.setAvailable(true);
            }
            ret.add(dl);
        }
        if ((cfg == null || cfg.getBooleanProperty("GRAB_screencaps", false)) && directurl_screencaps != null) {
            final String quality = "screencaps";
            final DownloadLink dl = this.createDownloadlink(directurl_screencaps, fid, productid, quality);
            final String pluginFilename = titleForPlugin + "_" + quality + ".zip";
            if (preferServersideOriginalFilenames) {
                String originalFilename = UrlQuery.parse(directurl_screencaps).get("filename");
                if (originalFilename == null) {
                    originalFilename = Plugin.getFileNameFromURL(directurl_screencaps);
                }
                if (originalFilename != null) {
                    dl.setFinalFileName(originalFilename);
                } else {
                    /* Fallback */
                    dl.setName(pluginFilename);
                }
            } else {
                dl.setFinalFileName(pluginFilename);
            }
            if (fast_linkcheck) {
                dl.setAvailable(true);
            }
            ret.add(dl);
        }
        if (ret.isEmpty()) {
            /* Nothing found -> Check why or return trailer. */
            if (trailer == null) {
                /* Dead end */
                if (br.containsHTML("(?i)<div>\\s*Unlock Video Now")) {
                    /* This can even happen with premium account. Some items need to be bought separately. */
                    throw new AccountRequiredException();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                if (account == null) {
                    logger.info("Returning trailer because: No account available");
                } else {
                    logger.info("Returning trailer because: Failed to find any other items");
                }
                ret.add(trailer);
            }
        } else if (numberOfAddedVideos == 0 && trailer != null) {
            /* No video items found so far -> Return trailer. */
            logger.info("Returning trailer because: Failed to find any other video items");
            ret.add(trailer);
        }
        /* Set additional properties */
        for (final DownloadLink result : ret) {
            result.setProperty(BangbrosCom.PROPERTY_TITLE, title);
            result.setProperty(BangbrosCom.PROPERTY_CAST_COMMA_SEPARATED, cast_comma_separated);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(titleForPlugin);
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        fp.addLinks(ret);
        return ret;
    }

    public static String regexStreamingURL(final Browser br, final String qualityIdentifier) {
        String url = br.getRegex("<source src=\"(https?://[^\"]+" + qualityIdentifier + "\\.mp4[^\"]*)\"[^>]*type=.video/mp4").getMatch(0);
        if (url == null) {
            /* 2023-08-31: Wider RegEx */
            url = br.getRegex("href=\"(https?://[^\"]+" + qualityIdentifier + "\\.mp4[^\"]*)\"[^>]*>" + qualityIdentifier).getMatch(0);
        }
        if (url != null) {
            return Encoding.htmlOnlyDecode(url);
        } else {
            return null;
        }
    }

    public static String regexZipUrl(final Browser br, final String key) {
        String zipurl = br.getRegex("([^<>\"\\']+/" + key + "/[^<>\"\\']+\\.zip[^<>\"\\']+)").getMatch(0);
        if (zipurl != null) {
            zipurl = Encoding.htmlDecode(zipurl);
        }
        return zipurl;
    }

    @Override
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    public static boolean requiresAccount(final String url) {
        return url != null && !(url.matches(type_userinput_video_couldbe_trailer) || url.matches(type_decrypted_zip));
    }

    public static boolean isOffline(final Browser br, final String url_source) {
        final boolean isOffline;
        if (url_source != null && url_source.matches(type_userinput_video_couldbe_trailer)) {
            isOffline = br.getHttpConnection().getResponseCode() == 503 || !br.getURL().contains("/video");
        } else {
            isOffline = !br.getURL().contains("/movie/") || br.getURL().contains("/library");
        }
        return isOffline;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}