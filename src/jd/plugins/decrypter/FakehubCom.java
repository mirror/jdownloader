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
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fakehub.com" }, urls = { "https?://site-ma\\.fakehub\\.com/(?:trailer|scene)/(\\d+)(/[a-z0-9\\-]+)?|https?://(www\\.)?fakehub\\.com/scene/(\\d+)(/[a-z0-9\\-]+)?" })
public class FakehubCom extends PluginForDecrypt {
    public FakehubCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_VIDEO            = "https?://(?:new\\.|site-)?ma\\.fakehub\\.com/(watch|scene)/(\\d+)(?:/[a-z0-9\\-_]+/?)?";
    private static final String TYPE_PHOTO            = "https?://(?:new\\.|site-)?ma\\.fakehub\\.com/pics/(\\d+)(?:/[a-z0-9\\-_]+/?)?";
    private static final String TYPE_MEMBER           = "https?://(?:new\\.|site-)?ma\\.fakehub\\.com/model/(\\d+)(?:/[a-z0-9\\-_]+/?)?";
    public static String        DOMAIN_BASE           = "fakehub.com";
    public static String        DOMAIN_PREFIX_PREMIUM = "site-ma.";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        /* Login if possible */
        getUserLogin(false);
        final String videoID = new Regex(parameter, "(?:trailer|scene)/(\\d+)").getMatch(0);
        if (videoID == null) {
            return null;
        }
        final LinkedHashMap<String, DownloadLink> qualities = crawlVideoAPI(videoID);
        final Iterator<Entry<String, DownloadLink>> iteratorQualities = qualities.entrySet().iterator();
        while (iteratorQualities.hasNext()) {
            decryptedLinks.add(iteratorQualities.next().getValue());
        }
        return decryptedLinks;
    }

    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private void getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa != null) {
            try {
                ((jd.plugins.hoster.FakehubCom) hostPlugin).login(this.br, aa, force);
                return;
            } catch (final PluginException e) {
                handleAccountException(aa, e);
            }
        }
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private LinkedHashMap<String, DownloadLink> crawlVideoAPI(final String videoID) throws Exception {
        final LinkedHashMap<String, DownloadLink> foundQualities = new LinkedHashMap<String, DownloadLink>();
        br.getPage("https://site-api.project1service.com/v2/releases/" + videoID);
        /* TODO: Check offline errorhandling */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("result");
        String title = (String) entries.get("title");
        String description = (String) entries.get("description");
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            title = videoID;
        } else if (title.equalsIgnoreCase("trailer")) {
            title = videoID + "_trailer";
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        final String format_filename = "%s_%s.mp4";
        entries = (LinkedHashMap<String, Object>) entries.get("videos");
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "full/files");
        LinkedHashMap<String, Object> videoInfo = null;
        final Iterator<Entry<String, Object>> qualities = entries.entrySet().iterator();
        while (qualities.hasNext()) {
            final Entry<String, Object> entry = qualities.next();
            videoInfo = (LinkedHashMap<String, Object>) entry.getValue();
            String format = (String) videoInfo.get("format");
            final long filesize = JavaScriptEngineFactory.toLong(videoInfo.get("sizeBytes"), 0);
            videoInfo = (LinkedHashMap<String, Object>) videoInfo.get("urls");
            String downloadurl = (String) videoInfo.get("download");
            if (StringUtils.isEmpty(downloadurl)) {
                /* Fallback to stream-URL */
                downloadurl = (String) videoInfo.get("view");
            }
            if (StringUtils.isEmpty(downloadurl)) {
                continue;
            } else if (StringUtils.isEmpty(format) || !format.matches("\\d+p")) {
                /* Skip invalid entries and hls and dash streams */
                continue;
            }
            /* E.g. '1080p' --> '1080' */
            format = format.replace("p", "");
            final DownloadLink dl = this.createDownloadlink("directhttp://" + downloadurl);
            dl.setFinalFileName(String.format(format_filename, title, format));
            dl.setProperty("fid", videoID);
            dl.setProperty("quality", format);
            if (filesize > 0) {
                dl.setDownloadSize(filesize);
            }
            dl.setAvailable(true);
            dl._setFilePackage(fp);
            // if (!loggedin) {
            // dl.setProperty("free_downloadable", true);
            // }
            foundQualities.put(format, dl);
        }
        return foundQualities;
    }

    public static String[] getPictureArray(final Browser br) {
        final String[] picarray = br.getRegex("data\\-flickity\\-lazyload=\"(http://[^<>\"]+\\d+\\.jpg[^<>\"]+nvb=[^<>\"]+)\"").getColumn(0);
        return picarray;
    }

    public static String getVideoUrlFree(final String fid) {
        return getProtocol() + "www." + DOMAIN_BASE + "/tour/video/" + fid + "/";
    }

    public static String getVideoUrlPremium(final String fid) {
        return getProtocol() + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + "/watch/" + fid + "/";
    }

    public static String getPicUrl(final String fid) {
        return getProtocol() + DOMAIN_PREFIX_PREMIUM + DOMAIN_BASE + "/pics/" + fid + "/";
    }

    public static String getProtocol() {
        return "https://";
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PornPortal;
    }
}