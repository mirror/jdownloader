//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.decrypter.DefinebabeComDecrypter;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class DefineBabeCom extends PluginForHost {
    public DefineBabeCom(PluginWrapper wrapper) {
        super(wrapper);
        /* Don't overload the server. */
        this.setStartIntervall(5 * 1000l);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "definebabe.com", "definebabes.com" });
        ret.add(new String[] { "definefetish.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/video/([a-z0-9]+)/([a-z0-9\\-]+)/");
        }
        return ret.toArray(new String[0]);
    }

    /* Tags: TubeContext@Player */
    /* Sites using the same player: definebabes.com, definebabe.com, definefetish.com */
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://www.definebabe.com/about/privacy/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String getMirrorID(DownloadLink link) {
        final String fid = getFID(link);
        if (link != null && StringUtils.equals(getHost(), link.getHost()) && fid != null) {
            return getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* Set fallback-filename */
        final Regex urlfinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
        final String weakTitle;
        final String urlSlug = urlfinfo.getMatch(1);
        if (urlSlug != null) {
            weakTitle = urlSlug.replace("-", " ").trim();
        } else {
            /* Fallback to File-ID */
            weakTitle = urlfinfo.getMatch(0);
        }
        final String extDefault = ".mp4";
        if (!link.isNameSet()) {
            link.setName(weakTitle + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("Please, call later\\.")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is busy", 5 * 60 * 1000l);
        }
        String title = DefinebabeComDecrypter.getFileTitle(br);
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            link.setFinalFileName(title + extDefault);
        }
        final String videoID = getVideoID(br);
        if (videoID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final boolean useNewAPI = true;
        if (useNewAPI) {
            br.getPage("/player/config.php?id=" + videoID);
            try {
                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                this.dllink = (String) entries.get("video_alt_url");
                if (StringUtils.isEmpty(this.dllink)) {
                    this.dllink = (String) entries.get("video_url");
                }
            } catch (final Throwable e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Broken video?", e);
            }
        } else {
            br.getPage("http://www." + link.getHost() + "/playlist/playlist.php?type=regular&video_id=" + videoID);
            final String decrypted = decryptRC4HexString("TubeContext@Player", br.getRequest().getHtmlCode().trim());
            final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(decrypted);
            final Map<String, Object> videos = (Map<String, Object>) entries.get("videos");
            /* Usually only 360 is available */
            final String[] qualities = { "1080p", "720p", "480p", "360p", "320p", "240p", "180p" };
            for (final String currentqual : qualities) {
                final Map<String, Object> quality_info = (Map<String, Object>) videos.get("_" + currentqual);
                if (quality_info != null) {
                    dllink = (String) quality_info.get("fileUrl");
                    break;
                }
            }
        }
        if (!StringUtils.isEmpty(dllink)) {
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, title, extDefault);
        }
        return AvailableStatus.TRUE;
    }

    public static final String getVideoID(final Browser br) {
        String videoID = br.getRegex("video_id=(\\d+)").getMatch(0);
        if (videoID == null) {
            videoID = br.getRegex("id=\\'comment_object_id\\' value=\"(\\d+)\"").getMatch(0);
        }
        if (videoID == null) {
            /* 2021-07-26 */
            videoID = br.getRegex("'video_id'\\s*:\\s*(\\d+)").getMatch(0);
        }
        return videoID;
    }

    public static boolean isOffline(final Browser br) {
        return (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Error with fetching video"));
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    /**
     * @author makaveli
     * @throws Exception
     */
    private static String decryptRC4HexString(final String plainTextKey, final String hexStringCiphertext) throws Exception {
        String ret = "";
        try {
            Cipher rc4 = Cipher.getInstance("RC4");
            rc4.init(Cipher.DECRYPT_MODE, new SecretKeySpec(plainTextKey.getBytes(), "RC4"));
            ret = new String(rc4.doFinal(HexFormatter.hexToByteArray(hexStringCiphertext)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unlimited Strength JCE Policy Files needed!", e);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KernelVideoSharing;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
