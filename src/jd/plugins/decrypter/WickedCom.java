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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
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
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wicked.com" }, urls = { "https?://members\\.wicked\\.com/[a-z]{2}/(video/[^/]+/[^/]+/\\d+|picture/[^/]+/\\d+)" })
public class WickedCom extends PluginForDecrypt {
    public WickedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static String        DOMAIN_BASE           = "wicked.com";
    public static String        DOMAIN_PREFIX_PREMIUM = "members.";
    private static final String TYPE_VIDEO            = "https?://members\\.wicked\\.com/[a-z]{2}/video/([^/]+)/([^/]+)/(\\d+)";
    private static final String TYPE_PHOTO            = "https?://members\\.wicked\\.com/[a-z]{2}/picture/([^/]+)/(\\d+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        final boolean is_logged_in = getUserLogin(false);
        // Login if possible
        if (is_logged_in) {
            br.getPage(param.getCryptedUrl());
        } else {
            /* 2021-06-08: Free users can sometimes watch trailers but in general they cannot do anything */
            // br.getPage(getVideoUrlFree(fid));
            throw new AccountRequiredException();
        }
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /*
         * Users can also perform official downloads but they're not unlocked for all premium users (costs extra lol) so let's just download
         * the streams right away.
         */
        if (param.getCryptedUrl().matches(TYPE_VIDEO)) {
            final String playerJson = br.getRegex("window\\.ScenePlayerOptions = (\\{.+\\});window\\.ScenePlayerName").getMatch(0);
            Map<String, Object> entries = JSonStorage.restoreFromString(playerJson, TypeRef.HASHMAP);
            entries = (Map<String, Object>) entries.get("playerOptions");
            final Map<String, Object> sceneInfos = (Map<String, Object>) entries.get("sceneInfos");
            final String dateFormatted = (String) sceneInfos.get("sceneReleaseDate");
            final String title = (String) sceneInfos.get("sceneTitle");
            final List<Map<String, Object>> streamingSources = (List<Map<String, Object>>) entries.get("streamingSources");
            final ArrayList<DownloadLink> selectedAndFoundQualities = new ArrayList<DownloadLink>();
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(dateFormatted + "_" + title);
            final String contentID = new Regex(param.getCryptedUrl(), TYPE_VIDEO).getMatch(2);
            for (final Map<String, Object> streamingSource : streamingSources) {
                final String label = (String) streamingSource.get("label");
                final String url = (String) streamingSource.get("src");
                if (!label.matches("\\d+p") || StringUtils.isEmpty(url)) {
                    /* E.g. "auto" */
                    continue;
                }
                final DownloadLink dl = this.createDownloadlink(url.replace("https://", "m3u8s://"));
                dl.setFinalFileName(dateFormatted + "_" + title + "_" + label + ".mp4");
                dl.setAvailable(true);
                dl.setLinkID(this.getHost() + "_" + contentID + "_" + label);
                dl._setFilePackage(fp);
                if (cfg.getBooleanProperty(label, true)) {
                    selectedAndFoundQualities.add(dl);
                }
            }
            if (!selectedAndFoundQualities.isEmpty()) {
                logger.info("Returning selected qualities");
                return selectedAndFoundQualities;
            } else {
                logger.info("Returning all qualities");
                return decryptedLinks;
            }
        } else if (param.getCryptedUrl().matches(TYPE_PHOTO)) {
            final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_PHOTO);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(urlinfo.getMatch(0));
            final String contentID = urlinfo.getMatch(1);
            final String[] pics = br.getRegex("class=\"imgLink start-image-viewer\" href=\"(https?://[^/]+/[^\"]+)").getColumn(0);
            int index = 0;
            for (final String pic : pics) {
                final DownloadLink dl = this.createDownloadlink("directhttp://" + pic);
                final String fname = Plugin.getFileNameFromURL(new URL(pic));
                if (fname != null) {
                    dl.setName(fname);
                }
                dl.setAvailable(true);
                dl.setLinkID(this.getHost() + "_" + contentID + "_" + index);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                index += 1;
            }
            return decryptedLinks;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    public static String getVideoJson(final Browser br) {
        return br.getRegex("\\.addVideoInfo\\((.*?)\\)").getMatch(0);
    }

    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.WickedCom) hostPlugin).login(aa, force);
        } catch (final PluginException e) {
            return false;
        }
        return true;
    }

    public static String getVideoUrlFree(final String url) throws MalformedURLException {
        return "https://www." + DOMAIN_BASE + new URL(url).getPath();
    }

    public static String getVideoUrlPremium(final String url) throws MalformedURLException {
        return "https://" + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + new URL(url).getPath();
    }

    public static String getPicUrl(final String url) throws MalformedURLException {
        return "https://" + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + new URL(url).getPath();
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}