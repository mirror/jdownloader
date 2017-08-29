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
import java.util.regex.Pattern;

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
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "naughtyamerica.com" }, urls = { "https?://(?:beta\\.)?(?:members|tour)\\.naughtyamerica\\.com/scene/[a-z0-9\\-]+\\-\\d+" })
public class NaughtyamericaCom extends PluginForDecrypt {

    public NaughtyamericaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static String DOMAIN_BASE           = "naughtyamerica.com";
    public static String DOMAIN_PREFIX_PREMIUM = "members.";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* 2016-12-12: Prefer current website instead of beta */
        final String parameter = param.toString().replace("beta.", "");
        final String filename_url = new Regex(parameter, "/([a-z0-9\\-]+)$").getMatch(0);
        final String fid = new Regex(parameter, "(\\d+)$").getMatch(0);
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        final boolean fast_linkcheck = cfg.getBooleanProperty("ENABLE_FAST_LINKCHECK", true);
        final boolean is_logged_in = getUserLogin(false);
        if (is_logged_in) {
            br.getPage(getVideoUrlPremium(filename_url));
        } else {
            br.getPage(getVideoUrlFree(filename_url));
        }
        String redirect = br.getRedirectLocation();
        if (redirect == null) {
            redirect = br.getRegex("Redirecting to <a href=\"(https?://[^<>\"]+)\">").getMatch(0);
        }
        if (isOffline(br)) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String title = null;
        if (!is_logged_in && redirect != null && redirect.contains("/access/login")) {
            title = filename_url;
        } else {
            if (redirect != null) {
                br.getPage(redirect);
            }
            if (title == null) {
                title = filename_url;
            }
        }

        if (is_logged_in) {
            final String[] videoInfo = getVideoInfoArray(br);
            for (final String singleVideoInfo : videoInfo) {
                final String directlink = getDirecturlFromVideoInfo(singleVideoInfo);
                final String[] videroInfoArray = getVideoInfoDetailed(singleVideoInfo);
                final String type = videroInfoArray[videroInfoArray.length - 2];
                final String quality = videroInfoArray[videroInfoArray.length - 1];
                final String quality_url = directlink != null ? new Regex(directlink, "([a-z0-9]+)\\.(?:wmv|mp4)").getMatch(0) : null;
                if (directlink == null || quality_url == null) {
                    continue;
                }
                final String type_for_property = getTypeForProperty(type);
                if (type_for_property != null && type_for_property.equalsIgnoreCase("clip") && !cfg.getBooleanProperty("GRAB_CLIPS", true)) {
                    /* Skip clips if user does not want them. */
                    continue;
                } else if (!cfg.getBooleanProperty("GRAB_" + quality, true)) {
                    /* Skip qualities which the user does not want to have */
                    continue;
                }
                final String linkid = title + type + quality;
                String ext = getFileNameExtensionFromURL(directlink);
                if (ext == null) {
                    ext = ".mp4";
                }
                String filename = title + "_";
                if (type_for_property != null && type_for_property.equalsIgnoreCase("clip")) {
                    /* E.g. clip 1, clip 2, ... */
                    filename += type + "_";
                }
                filename += quality_url + ext;

                final DownloadLink dl = this.createDownloadlink(directlink.replaceAll("https?://", "http://naughtyamericadecrypted"));
                dl.setLinkID(linkid);
                dl.setName(filename);
                if (fast_linkcheck) {
                    dl.setAvailable(true);
                }
                dl.setProperty("fid", fid);
                dl.setProperty("quality", quality);
                dl.setProperty("filename_url", filename_url);
                if (type_for_property != null) {
                    dl.setProperty("type", type_for_property);
                }
                decryptedLinks.add(dl);
            }
            if (cfg.getBooleanProperty("GRAB_PICTURES", true)) {
                /* Crawl picture gallery if user wants that. */
                final String pictures[] = getPictureArray(br);
                for (String finallink : pictures) {
                    final String number_formatted = new Regex(finallink, "(\\d+)\\.jpg").getMatch(0);
                    finallink = finallink.replaceAll("https?://", "http://naughtyamericadecrypted");
                    final String linkid = title + number_formatted;
                    final DownloadLink dl = this.createDownloadlink(finallink);
                    dl.setLinkID(linkid);
                    dl.setFinalFileName(title + "_" + number_formatted + ".jpg");
                    if (fast_linkcheck) {
                        dl.setAvailable(true);
                    }
                    dl.setProperty("fid", fid);
                    dl.setProperty("picnumber_formatted", number_formatted);
                    dl.setProperty("filename_url", filename_url);
                    decryptedLinks.add(dl);
                }
            }
        } else {
            /* We're not logged in but maybe the user has an account to download later or an MOCH account --> Add one dummy url. */
            final String quality_dummy = "1080";
            final String type_dummy = "full";
            final String linkid = title + type_dummy + quality_dummy;
            final DownloadLink dl = this.createDownloadlink("http://naughtyamericadecryptedlvl3.secure.naughtycdn.com/mfhg/members/chanelvan/" + filename_url + "_" + quality_dummy + ".mp4");
            dl.setLinkID(linkid);
            dl.setName(title + "_" + quality_dummy + ".mp4");
            dl.setProperty("fid", fid);
            dl.setProperty("quality", quality_dummy);
            dl.setProperty("filename_url", filename_url);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public static String[] getVideoInfoArray(final Browser br) {
        return br.getRegex("(<a onclick=\"trackVideo\\(.*?</a>)").getColumn(0);
    }

    public static String[] getVideoInfoDetailed(final String source) {
        String videoInfoString = new Regex(source, "trackVideo\\(([^<>]+)\\)").getMatch(0);
        videoInfoString = videoInfoString.replace("'", "");
        final String[] videroInfoArray = videoInfoString.split(", ");
        return videroInfoArray;
    }

    public static String getDirecturlFromVideoInfo(final String source) {
        String directurl = new Regex(source, "(https?://[^<>\"]+\\.(?:mp4|wmv)[^<>\"]*?)\"").getMatch(0);
        if (directurl != null) {
            directurl = Encoding.htmlDecode(directurl);
        }
        return directurl;
    }

    public static String[] getPictureArray(final Browser br) {
        return br.getRegex("<a class=\"fancybox gallery_pic\"[^>]+rel=\"nozoom\"[^>]+href=\"(http[^<>\"]+\\.jpg[^<>\"]*?)\">").getColumn(0);
    }

    public static String getVideoUrlFree(final String filename_url) {
        return "http://tour." + DOMAIN_BASE + "/scene/" + filename_url;
    }

    public static String getVideoUrlPremium(final String filename_url) {
        return "http://" + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + "/scene/" + filename_url;
    }

    public static String getTypeForProperty(final String type) {
        return new Regex(type, Pattern.compile(".*?clip.*?", Pattern.CASE_INSENSITIVE)).matches() ? "clip" : null;
    }

    public static String getPicUrl(final String filename_url) {
        return getVideoUrlPremium(filename_url);
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
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
            ((jd.plugins.hoster.NaughtyamericaCom) hostPlugin).login(br, aa, force);
        } catch (final PluginException e) {

            aa.setValid(false);
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}