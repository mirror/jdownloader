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
import java.util.Arrays;
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

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "realitykings.com" }, urls = { "https?://(?:new\\.)?members\\.realitykings\\.com/video/(?:(?:full|watch)/\\d+(?:/[a-z0-9\\-_]+/?)?|pics/\\d+(?:/[a-z0-9\\-_]+/?)?)|https?://(?:new\\.)?members\\.realitykings\\.com/(?:videos/\\?models=\\d+|model/view/\\d+/[a-z0-9\\-_]+/?)|https?://(?:www\\.)?realitykings\\.com/tour/video/watch/\\d+/(?:[a-z0-9\\-_]+/?)?" })
public class RealityKingsCom extends PluginForDecrypt {

    public RealityKingsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_VIDEO            = "https?://(?:new\\.)?members\\.realitykings\\.com/video/(?:full|watch)/\\d+(?:/[a-z0-9\\-_]+/?)?";
    private static final String TYPE_PHOTO            = "https?://(?:new\\.)?members\\.realitykings\\.com/video/pics/\\d+(?:/[a-z0-9\\-_]+/?)?";
    private static final String TYPE_MEMBER           = "https?://(?:new\\.)?members\\.realitykings\\.com/(?:videos/\\?models=\\d+|model/view/\\d+/[a-z0-9\\-_]+/?)";

    private static final String TYPE_VIDEO_FREE       = ".+/tour/video/watch/.+";

    public static String        DOMAIN_BASE           = "realitykings.com";
    public static String        DOMAIN_PREFIX_PREMIUM = "new.members.";

    private final List<String>  all_known_qualities   = Arrays.asList("GRAB_1080", "GRAB_3000", "GRAB_1500", "GRAB_800", "GRAB_mp4v_480", "GRAB_mp4v_320", "GRAB_3gp");

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String host_decrypted = "realitykingsdecrypted://";
        String fid = null;
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        // Login if possible
        final boolean loggedin = getUserLogin(false);
        if (!loggedin && !parameter.matches(TYPE_VIDEO_FREE)) {
            logger.info("No account present but url requires login --> Cannot decrypt anything!");
            return decryptedLinks;
        }
        if (parameter.matches(TYPE_MEMBER)) {
            fid = new Regex(parameter, ".+realitykings\\.com/(?:videos/\\?models=|model/view/)(\\d+)").getMatch(0);
            br.getPage("http://" + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + "/videos/?models=" + fid);
            if (isOffline(this.br)) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            /* Grab all videos of a model/member */
            final String[] videourls = this.br.getRegex("/video/full[^<>\"]+").getColumn(-1);
            for (final String videourl : videourls) {
                decryptedLinks.add(this.createDownloadlink(getProtocol() + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + videourl));
            }
        } else {
            fid = new Regex(parameter, "/(\\d+)/").getMatch(0);
            String title;
            if (isVideoURL(parameter)) {
                if (loggedin) {
                    br.getPage(getVideoUrlPremium(fid));
                } else {
                    br.getPage(getVideoUrlFree(fid));
                }
                if (isOffline(this.br)) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                title = getTitle();
                if (title == null) {
                    /* Fallback to id from inside url */
                    title = fid;
                }
                final String format_filename = "%s_%s%s";
                final String htmldownload = this.br.getRegex("class=\"fa fa\\-download\"></i>[^<>]*?</div>(.*?)</div>").getMatch(0);
                final String[] dlinfo = htmldownload != null ? htmldownload.split("</a>") : null;

                if (dlinfo != null) {
                    List<String> all_selected_qualities = new ArrayList<String>();
                    for (final String possibleQuality : all_known_qualities) {
                        if (cfg.getBooleanProperty(possibleQuality, true)) {
                            all_selected_qualities.add(possibleQuality);
                        }
                    }
                    if (all_selected_qualities.size() == 0) {
                        logger.info("User selected nothing --> Grabbing ALL qualities");
                        all_selected_qualities = all_known_qualities;
                    }

                    for (final String video : dlinfo) {
                        final String dlurl = new Regex(video, "\"(/[^<>\"]*?download/[^<>\"]+/)\"").getMatch(0);
                        final String quality = new Regex(video, "<span>([^<>\"]+)</span>").getMatch(0);
                        final String filesize = new Regex(video, "<var>([^<>\"]+)</var>").getMatch(0);
                        final String quality_url = dlurl != null ? new Regex(dlurl, "/\\d+/([^/]+)/?$").getMatch(0) : null;
                        if (dlurl == null || quality == null || quality_url == null) {
                            continue;
                        }
                        if (!all_selected_qualities.contains("GRAB_" + quality_url)) {
                            /* Skip unwanted content */
                            continue;
                        }
                        final String ext;
                        if ("3gp".equalsIgnoreCase(quality_url)) {
                            /* Special case */
                            ext = ".3gp";
                        } else {
                            ext = ".mp4";
                        }
                        final DownloadLink dl = this.createDownloadlink(br.getURL(dlurl).toString());
                        if (filesize != null) {
                            dl.setDownloadSize(SizeFormatter.getSize(filesize));
                            dl.setAvailable(true);
                        }
                        dl.setName(String.format(format_filename, title, quality, ext));
                        dl.setProperty("fid", fid);
                        dl.setProperty("quality", quality);
                        decryptedLinks.add(dl);
                    }
                }

                if (decryptedLinks.size() == 0) {
                    /* This should only happen with free account mode or no account at all (trailer download). */
                    logger.info("No official downloads available --> Downloading streams (adding all regardless of quality selection)");
                    final String json = this.br.getRegex("streams\\s*?:\\s*?(\\{.*?\\})\\s+").getMatch(0);
                    if (json == null) {
                        return null;
                    }

                    final LinkedHashMap<String, Object> entries = jd.plugins.decrypter.BrazzersCom.getVideoMapHttpStreams(json);

                    int bitrate_max = 0;
                    int bitrate_temp = 0;
                    DownloadLink downloadlink_quality_BEST = null;

                    final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
                    final String ext = ".mp4";
                    while (it.hasNext()) {
                        final Entry<String, Object> entry = it.next();
                        final String quality_key = entry.getKey();
                        final String quality_url = (String) entry.getValue();

                        final DownloadLink dl = this.createDownloadlink(quality_url.replaceAll("https?://", host_decrypted));
                        dl.setName(String.format(format_filename, title, quality_key, ext));
                        dl.setProperty("fid", fid);
                        dl.setProperty("quality", quality_key);
                        if (!loggedin) {
                            dl.setProperty("free_downloadable", true);
                        }
                        decryptedLinks.add(dl);

                        if (quality_key.matches("\\d+")) {
                            bitrate_temp = Integer.parseInt(quality_key);
                            if (bitrate_temp > bitrate_max) {
                                bitrate_max = bitrate_temp;
                                downloadlink_quality_BEST = dl;
                            }
                        }

                    }

                    /* Not logged in --> Usually only trailer / MOCH download --> Add BEST only */
                    if (!loggedin && downloadlink_quality_BEST != null) {
                        logger.info("Trailer download: Adding BEST quality only");
                        decryptedLinks.clear();
                        decryptedLinks.add(downloadlink_quality_BEST);
                    }

                }

            } else if (parameter.matches(TYPE_PHOTO)) {
                br.getPage(parameter);
                if (isOffline(this.br)) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                title = getTitle();
                if (title == null) {
                    /* Fallback to id from inside url */
                    title = fid;
                }
                final String pictures[] = getPictureArray(this.br);
                for (String finallink : pictures) {
                    final String number_formatted = new Regex(finallink, "(\\d+)\\.jpg").getMatch(0);
                    finallink = finallink.replaceAll("https?://", host_decrypted);
                    final DownloadLink dl = this.createDownloadlink(finallink);
                    dl.setFinalFileName(title + "_" + number_formatted + ".jpg");
                    dl.setAvailable(true);
                    dl.setProperty("fid", fid);
                    dl.setProperty("picnumber_formatted", number_formatted);
                    decryptedLinks.add(dl);
                }
            } else {
                /* WTF - this should never happen! */
                logger.warning("Unsupported linktype");
                return decryptedLinks;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private String getTitle() {
        return br.getRegex("<title>([^<>\"]*?) / Reality Kings</title>").getMatch(0);
    }

    public static boolean isVideoURL(final String url) {
        return url.matches(TYPE_VIDEO) || url.matches(TYPE_VIDEO_FREE) || url.contains(".mp4");
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
            ((jd.plugins.hoster.RealityKingsCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {

            aa.setValid(false);
            return false;
        }
        return true;
    }

    public static String[] getPictureArray(final Browser br) {
        final String[] picarray = br.getRegex("data\\-flickity\\-lazyload=\"(http://[^<>\"]+\\d+\\.jpg[^<>\"]+nvb=[^<>\"]+)\"").getColumn(0);
        return picarray;
    }

    public static String getVideoUrlFree(final String fid) {
        return getProtocol() + "www." + DOMAIN_BASE + "/tour/video/watch/" + fid + "/";
    }

    public static String getVideoUrlPremium(final String fid) {
        return getProtocol() + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + "/video/full/" + fid + "/";
    }

    public static String getPicUrl(final String fid) {
        return getProtocol() + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + "/video/pics/" + fid + "/";
    }

    public static String getProtocol() {
        return "http://";
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