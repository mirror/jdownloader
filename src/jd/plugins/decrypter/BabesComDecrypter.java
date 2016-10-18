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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "babes.com" }, urls = { "https?://members\\.babes\\.com/(?:video/view/id/\\d+(?:/[a-z0-9\\-_]+/?)?|pictures/hqpics/id/\\d+(?:/[a-z0-9\\-_]+/?)?)" })
public class BabesComDecrypter extends PluginForDecrypt {

    public BabesComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_VIDEO = "https?://members\\.babes\\.com/video/view/id/\\d+(?:/[a-z0-9\\-_]+/)?";
    private static final String TYPE_PHOTO = "https?://members\\.babes\\.com/pictures/hqpics/id/\\d+(?:/[a-z0-9\\-_]+/?)?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "/id/(\\d+)").getMatch(0);
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
        String title = null;
        if (parameter.matches(TYPE_VIDEO)) {
            title = br.getRegex("<title>([^<>\"]*?) \\- BABES</title>").getMatch(0);
            if (title == null) {
                /* Fallback to id from inside url */
                title = fid;
            }
            final String base_url = new Regex(this.br.getURL(), "(https?://[^/]+)/").getMatch(0);
            final String htmldownload = this.br.getRegex("<ul id=\"video\\-download\">(.*?)</ul>").getMatch(0);
            final String[] dlinfo = htmldownload.split("</li>");
            for (final String video : dlinfo) {
                final String dlurl = new Regex(video, "(/download/[^<>\"]+/)\"").getMatch(0);
                final String quality = new Regex(video, "<span>([^<>\"]+)</span>").getMatch(0);
                final String filesize = new Regex(video, "<var>([^<>\"]+)</var>").getMatch(0);
                if (dlurl == null || quality == null || filesize == null) {
                    continue;
                }
                final DownloadLink dl = this.createDownloadlink(base_url + dlurl);
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                dl.setName(title + "_" + quality + ".mp4");
                dl.setAvailable(true);
                dl.setProperty("fid", fid);
                dl.setProperty("quality", quality);
                decryptedLinks.add(dl);
            }
        } else {
            /* Nothing good available in html code --> Get title from url if possible */
            title = new Regex(parameter, "/id/\\d+/(.+)").getMatch(0);
            if (title == null) {
                /* Fallback to id from inside url */
                title = fid;
            }
            final String pictures[] = getPictureArray(this.br);
            for (String finallink : pictures) {
                final String number_formatted = new Regex(finallink, "/pics/img/(\\d+)\\.jpg").getMatch(0);
                if (!finallink.startsWith("http://photos.bb.contentdef.com/")) {
                    /* WTF */
                    continue;
                }
                finallink = finallink.replace("http://photos.bb.contentdef.com/", "http://babesdecrypted.photos.bb.contentdef.com/");
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
            ((jd.plugins.hoster.BabesCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {

            aa.setValid(false);
            return false;
        }
        return true;
    }

    public static String[] getPictureArray(final Browser br) {
        String picsource = br.getRegex("data\\-galleryui\\-images=\\'\\[(.*?)\\]").getMatch(0);
        if (picsource != null) {
            picsource = picsource.replace("\\", "");
            picsource = picsource.replace("\"", "");
        }
        final String[] picarray = picsource.split(",");
        return picarray;
    }

    public static String getVideoUrlFree(final String fid) {
        return "http://www.babes.com/tour/videos/view/id/" + fid + "/";
    }

    public static String getVideoUrlPremium(final String fid) {
        return "http://members.babes.com/video/view/id/" + fid + "/";
    }

    public static String getPicUrl(final String fid) {
        return "http://members.babes.com/pictures/hqpics/id/" + fid + "/";
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