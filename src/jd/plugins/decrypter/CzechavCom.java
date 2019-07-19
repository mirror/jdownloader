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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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
import jd.utils.JDUtilities;

import org.jdownloader.plugins.components.config.CzechavComConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "czechav.com" }, urls = { "https?://(?:www\\.)?czechav\\.com/(?:de|en)/video/[a-z0-9\\-]+\\-\\d+[a-z0-9\\-]*/?" })
public class CzechavCom extends PluginForDecrypt {

    public CzechavCom(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    protected DownloadLink createDownloadlink(String url, final String fid, final String urlpart, final String quality) {
        url = url.replaceAll("https?://", "http://czechavdecrypted");
        final DownloadLink dl = super.createDownloadlink(url, true);
        dl.setProperty("fid", fid);
        dl.setProperty("quality", quality);
        return dl;
    }

    /* 2016-12-29: Prevent serverside IP ban. */
    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String urlpart = new Regex(parameter, "/video/([a-z0-9\\-]+)-\\d+").getMatch(0);
        final String fid = new Regex(parameter, "(\\d+)/?$").getMatch(0);
        final boolean is_logged_in = getUserLogin(false);
        final CzechavComConfigInterface cfg = PluginJsonConfig.get(org.jdownloader.plugins.components.config.CzechavComConfigInterface.class);
        final boolean fastLinkcheck = cfg.isFastLinkcheckEnabled();
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
        final HashMap<String, List<DownloadLink>> qualities = new HashMap<String, List<DownloadLink>>();
        for (String videourl : videourls) {
            videourl = "https://" + this.getHost() + videourl;
            final String quality = new Regex(videourl, "x(\\d+)").getMatch(0);
            if (quality != null) {
                final String quality_url = new Regex(videourl, "(\\d+x\\d+)").getMatch(0);
                final String ext = getFileNameExtensionFromURL(videourl, ".mp4");
                final DownloadLink dl = this.createDownloadlink(videourl, fid, urlpart, quality_url);
                if (fastLinkcheck) {
                    dl.setAvailable(true);
                }
                dl.setName(title + "_" + quality_url + ext);
                List<DownloadLink> list = qualities.get(quality);
                if (list == null) {
                    list = new ArrayList<DownloadLink>();
                    qualities.put(quality, list);
                }
                list.add(dl);
            }
        }

        final boolean allQualities = !(cfg.isGrab1080pVideoEnabled() || cfg.isGrab2160pVideoEnabled() || cfg.isGrab360pVideoEnabled() || cfg.isGrab540pVideoEnabled() || cfg.isGrab720pVideoEnabled() || cfg.isGrabOtherResolutionsVideoEnabled());
        final boolean bestOnly = cfg.isGrabBestVideoVersionEnabled();

        if ((allQualities || cfg.isGrab2160pVideoEnabled()) && qualities.containsKey("2160")) {
            decryptedLinks.addAll(qualities.get("2160"));
        }
        if ((!bestOnly || decryptedLinks.isEmpty()) && (allQualities || cfg.isGrab1080pVideoEnabled()) && qualities.containsKey("1080")) {
            decryptedLinks.addAll(qualities.get("1080"));
        }
        if ((!bestOnly || decryptedLinks.isEmpty()) && (allQualities || cfg.isGrab720pVideoEnabled()) && qualities.containsKey("720")) {
            decryptedLinks.addAll(qualities.get("720"));
        }
        if ((!bestOnly || decryptedLinks.isEmpty()) && (allQualities || cfg.isGrab540pVideoEnabled()) && qualities.containsKey("540")) {
            decryptedLinks.addAll(qualities.get("540"));
        }
        if ((!bestOnly || decryptedLinks.isEmpty()) && (allQualities || cfg.isGrab360pVideoEnabled()) && qualities.containsKey("360")) {
            decryptedLinks.addAll(qualities.get("360"));
        }
        if ((!bestOnly || decryptedLinks.isEmpty()) && (allQualities || cfg.isGrabOtherResolutionsVideoEnabled())) {
            final Iterator<Entry<String, List<DownloadLink>>> it = qualities.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, List<DownloadLink>> next = it.next();
                final int q = Integer.valueOf(next.getKey());
                switch (q) {
                case 2160:
                case 1080:
                case 720:
                case 540:
                case 360:
                    continue;
                default:
                    decryptedLinks.addAll(next.getValue());
                    break;
                }
            }

        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public static String[] getVideourls(final Browser br) {
        return br.getRegex("\"(/[^/]+/video/[^/]+/download/video\\-\\d+x\\d+(-\\d+kbps)?\\.(mp4|wmv))\"").getColumn(0);
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