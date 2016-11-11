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
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fantasymassage.com" }, urls = { "https?://members\\.fantasymassage\\.com/[a-z]{2}/(?:video|picture)//[A-Za-z0-9\\-_]+/\\d+|https?://members\\.fantasymassage\\.com/[a-z]{2}/pornstar//\\d+/[A-Za-z0-9\\-_]+" })
public class FantasymassageCom extends PluginForDecrypt {

    public FantasymassageCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_VIDEO            = ".+/video//[A-Za-z0-9\\-_]+/\\d+";
    private static final String TYPE_PHOTO            = ".+/picture//[A-Za-z0-9\\-_]+/\\d+";
    private static final String TYPE_MEMBER           = ".+/pornstar//\\d+/[A-Za-z0-9\\-_]+";

    public static String        DOMAIN_BASE           = "fantasymassage.com";
    public static String        DOMAIN_PREFIX_PREMIUM = "members.";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String url_title = new Regex(parameter, "//([A-Za-z0-9\\-_]+)/\\d+$").getMatch(0);
        final String fid = new Regex(parameter, "(\\d+)$").getMatch(0);
        final String url_filename = url_title + "_" + fid;
        final String urlpart = url_title + "/" + fid;
        final String url_decrypted = "http://fantasymassagedecrypted";
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        // Login if possible
        if (!getUserLogin(false)) {
            logger.info("No account present --> Cannot decrypt anything!");
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String title = br.getRegex("<h1 class=\"title\">([^<>\"]+)</h1>").getMatch(0);
        if (title == null) {
            title = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        }
        if (title == null) {
            /* Fallback to filename from inside url */
            title = url_filename;
        }
        if (parameter.matches(TYPE_VIDEO)) {
            final String htmldownload = this.br.getRegex("<div class=\"optionList\">(.*?)</div>\\s*?</div>").getMatch(0);
            final String[] dlinfo = htmldownload.split("</p>");
            for (final String video : dlinfo) {
                final String dlurl = new Regex(video, "\"(/[^<>\"]*?download/[^<>\"]+mp4)\"").getMatch(0);
                final String quality = new Regex(video, "class=\"movieQuality\">([^<>\"]+)<").getMatch(0);
                final String filesize = new Regex(video, "class=\"movieSize\">([^<>\"]+)<").getMatch(0);
                final String quality_url = dlurl != null ? new Regex(dlurl, "/(\\d+p)/mp4/?$").getMatch(0) : null;
                if (dlurl == null || quality == null || quality_url == null) {
                    continue;
                }
                if (!cfg.getBooleanProperty("GRAB_" + quality_url, true)) {
                    /* Skip unwanted content. */
                    continue;
                }
                final String ext = ".mp4";
                final DownloadLink dl = this.createDownloadlink(url_decrypted + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + dlurl);
                if (filesize != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                dl.setAvailable(true);
                dl.setName(title + "_" + quality + ext);
                dl.setProperty("fid", fid);
                dl.setProperty("urlpart", urlpart);
                dl.setProperty("quality", quality);
                decryptedLinks.add(dl);
            }
        } else if (parameter.matches(TYPE_MEMBER)) {
            /* Grab all videos of a model/member */
            /* 2016-11-11: TODO - maybe add pagination support in the future if wished by a user. */
            final String[] video_or_photo_url = this.br.getRegex("(/[a-z]{2}/(?:video|picture)//[^<>\"]+)\"").getColumn(0);
            for (final String videourl : video_or_photo_url) {
                decryptedLinks.add(this.createDownloadlink(getProtocol() + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + videourl));
            }
        } else if (parameter.matches(TYPE_PHOTO)) {
            final String finallink_zip = getPictureZipUrl(this.br);
            if (finallink_zip == null) {
                return null;
            }
            final String number_formatted = new Regex(finallink_zip, "(\\d+)\\.zip").getMatch(0);
            final DownloadLink dl = this.createDownloadlink(finallink_zip.replaceAll("https?://", url_decrypted));
            dl.setFinalFileName(title + "_" + number_formatted + ".jpg");
            /*
             * Combined with the fact that for all other downloadlinks, filesize is given via html, we should be able to do this one extra
             * request without issues.
             */
            // dl.setAvailable(true);
            dl.setProperty("fid", fid);
            dl.setProperty("urlpart", urlpart);
            dl.setProperty("picnumber_formatted", number_formatted);
            decryptedLinks.add(dl);
        } else {
            /* WTF - this should never happen! */
            logger.warning("Unsupported linktype");
            return decryptedLinks;
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
            jd.plugins.hoster.FantasymassageCom.login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    public static String getPictureZipUrl(final Browser br) {
        return br.getRegex("(https?://[^<>\"]+\\d+\\.zip[^<>\"]+)").getMatch(0);
    }

    public static String getVideoUrlFree(final String urlpart) {
        return getProtocol() + "www." + DOMAIN_BASE + "/en/video//" + urlpart;
    }

    public static String getVideoUrlPremium(final String urlpart) {
        return getProtocol() + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + "/en/video//" + urlpart;
    }

    public static String getPicUrlFree(final String urlpart) {
        return getProtocol() + "www." + DOMAIN_BASE + "/en/picture//" + urlpart;
    }

    public static String getPicUrlPremium(final String urlpart) {
        return getProtocol() + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + "/en/picture//" + urlpart;
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
        return SiteTemplate.EvilAngelNetwork;
    }

}