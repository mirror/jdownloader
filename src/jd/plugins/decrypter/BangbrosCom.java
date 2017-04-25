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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bangbros.com" }, urls = { "https?://members\\.bangbros\\.com/product/\\d+/movie/\\d+" })
public class BangbrosCom extends PluginForDecrypt {

    public BangbrosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected DownloadLink createDownloadlink(String url, final String fid, final String productid, final String quality) {
        url = url.replaceAll("https?://", "bangbrosdecrypted://");
        final DownloadLink dl = super.createDownloadlink(url, true);
        dl.setProperty("fid", fid);
        dl.setProperty("quality", quality);
        return dl;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final Regex finfo = new Regex(parameter, "product/(\\d+)/movie/(\\d+)");
        final String productid = finfo.getMatch(0);
        final String fid = finfo.getMatch(1);
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
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
        final String directurl_photos = regexZipUrl(this.br, "pictures");
        final String directurl_screencaps = regexZipUrl(this.br, "screencaps");
        String title = this.br.getRegex("class=\"vdo\\-hdd1\">([^<>\"]+)<").getMatch(0);
        if (title == null) {
            title = this.br.getRegex("class=\"desTxt\\-hed\">([^<>\"]+)<").getMatch(0);
        }
        if (title == null) {
            /* Fallback to id from inside url */
            title = fid;
        }
        final String[] htmls_videourls = getVideourls(this.br);
        for (final String html_videourl : htmls_videourls) {
            final String videourl = getVideourlFromHtml(html_videourl);
            if (videourl == null) {
                continue;
            }
            final String quality_url = new Regex(videourl, "(\\d+p)").getMatch(0);
            if (quality_url == null || !cfg.getBooleanProperty("GRAB_" + quality_url, true)) {
                continue;
            }
            final String ext = ".mp4";
            final DownloadLink dl = this.createDownloadlink(videourl, fid, productid, quality_url);
            dl.setName(title + "_" + quality_url + ext);
            decryptedLinks.add(dl);
        }
        if (cfg.getBooleanProperty("GRAB_photos", false)) {
            final String quality = "pictures";
            final DownloadLink dl = this.createDownloadlink(directurl_photos, fid, productid, quality);
            dl.setName(title + "_" + quality + ".zip");
            decryptedLinks.add(dl);
        }
        if (cfg.getBooleanProperty("GRAB_screencaps", false)) {
            final String quality = "screencaps";
            final DownloadLink dl = this.createDownloadlink(directurl_screencaps, fid, productid, quality);
            dl.setName(title + "_" + quality + ".zip");
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public static String[] getVideourls(final Browser br) {
        final String html_urltable = br.getRegex("class=\"dropM\">\\s*?<ul>(.*?)</ul>").getMatch(0);
        return html_urltable.split("<li>");
    }

    public static String getVideourlFromHtml(final String html) {
        String videourl = new Regex(html, "\"(http[^<>\"]+)\"").getMatch(0);
        if (videourl != null) {
            videourl = Encoding.htmlDecode(videourl);
        }
        return videourl;
    }

    public static String regexZipUrl(final Browser br, final String key) {
        String zipurl = br.getRegex("(https?://[^<>\"\\']+/" + key + "/[^<>\"\\']+\\.zip[^<>\"\\']+)").getMatch(0);
        if (zipurl != null) {
            zipurl = Encoding.htmlDecode(zipurl);
        }
        return zipurl;
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
            ((jd.plugins.hoster.BangbrosCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    // public static String getPicUrl(final String original_url) {
    // return original_url + "/photos";
    // }

    public static boolean isOffline(final Browser br) {
        return !br.getURL().contains("/movie/") || br.getURL().contains("/library");
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}