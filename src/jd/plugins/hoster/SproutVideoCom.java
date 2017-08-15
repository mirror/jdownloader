package jd.plugins.hoster;

import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 *
 * note: backend cloudfront
 *
 * @author raztoki
 *
 */

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sproutvideo.com" }, urls = { "https?://(?:www\\.)?(?:videos\\.sproutvideo\\.com/embed/[a-f0-9]{18}/[a-f0-9]{16}|\\w+\\.vids\\.io/videos/[a-f0-9]{18})" })
public class SproutVideoCom extends PluginForHost {

    public SproutVideoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    private String dllink;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        br = new Browser();
        dllink = null;
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (!br.getURL().contains("/embed/")) {
            if (containsPassword()) {
                Form f = br.getFormByInputFieldPropertyKeyValue("id", "password");
                String password = link.getDownloadPassword();
                if (password != null) {
                    f.put("password", Encoding.urlEncode(password));
                    br.submitForm(f);
                }
                if (containsPassword()) {
                    // invalid cache
                    link.setDownloadPassword(null);
                    f = br.getFormByInputFieldPropertyKeyValue("id", "password");
                    password = Plugin.getUserInput("Password?", link);
                    f.put("password", Encoding.urlEncode(password));
                    br.submitForm(f);
                    if (containsPassword()) {
                        // since we dont have a password linkstatus
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Incorrect Password");
                    }
                }
                link.setDownloadPassword(password);
            }
            // embed required, you can get it from omebed json/xml or iframe or 'meta content twitter'
            // prefer iframe as it can have ?type=hd parameter
            String embed = br.getRegex("<iframe src=('|\"|)(.*?)\\1 ").getMatch(1);
            if (StringUtils.isEmpty(embed)) {
                embed = br.getRegex("<meta content=('|\"|)(.*?)\\1 name=\"twitter:player\"").getMatch(1);
            }
            if (embed == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(embed);
        }
        // json
        String json = br.getRegex("var dat\\s*=\\s*'([a-zA-Z0-9-=_/+]+)'").getMatch(0);
        if (json == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        json = Encoding.Base64Decode(json);
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        final Boolean hls = (Boolean) entries.get("hls");
        final String title = (String) entries.get("title");
        String m3u;
        if (Boolean.TRUE.equals(hls)) {
            final String userHash = (String) entries.get("s3_user_hash");
            final String videoHash = (String) entries.get("s3_video_hash");
            /// cant use cdSig, its wrong
            entries = (LinkedHashMap<String, Object>) entries.get("signature");
            final String cfPolicy = (String) entries.get("CloudFront-Policy");
            final String cfSignature = (String) entries.get("CloudFront-Signature");
            final String cfKeyPairId = (String) entries.get("CloudFront-Key-Pair-Id");
            m3u = "https://hls2.videos.sproutvideo.com/" + userHash + "/" + videoHash + "/video/index.m3u8?Policy=" + cfPolicy + "&Signature=" + cfSignature + "&Key-Pair-Id=" + cfKeyPairId;
            dllink = m3u;
            link.setName(title + ".mp4");
            link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        return AvailableStatus.TRUE;
    }

    private boolean containsPassword() {
        final boolean result = br.getHttpConnection().getResponseCode() == 403 && br.containsHTML("<title>Password Protected Video");
        return result;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllink.contains(".m3u8")) {
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(br, dllink));
            String dllink = hlsbest.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink, this.dllink.substring(this.dllink.indexOf("?")));
            dl.startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
