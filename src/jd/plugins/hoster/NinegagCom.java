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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "9gag.com" }, urls = { "https?://(?:www\\.)?9gag\\.com/[^/]+/([a-zA-Z0-9]+)" })
public class NinegagCom extends PluginForHost {
    public NinegagCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_HOST, LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = false;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://9gag.com/tos";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("?post_removed=1")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String id = this.getFID(link);
        String title = null;
        String description = null;
        String jsonParse = br.getRegex("window\\._config\\s*=\\s*JSON\\.parse\\((.*?)\\)\\s*;\\s*</script").getMatch(0);
        Map<String, Object> root = null;
        boolean video = false;
        if (jsonParse != null) {
            jsonParse = restoreFromString(jsonParse, TypeRef.STRING);
            root = restoreFromString(jsonParse, TypeRef.MAP);
            final Object postsO = JavaScriptEngineFactory.walkJson(root, "data/posts");
            Map<String, Object> post = null;
            if (postsO != null) {
                /* Variant1: We need to find the object containing the data of the post we want. */
                final List<Map<String, Object>> posts = (List<Map<String, Object>>) postsO;
                for (final Map<String, Object> thispost : posts) {
                    final String thisID = thispost.get("id").toString();
                    if (thisID.equals(id)) {
                        post = thispost;
                        break;
                    }
                }
            }
            if (post == null) {
                /* Variant2: There is only one post object which is the one we want. */
                post = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "data/post");
            }
            if (post != null) {
                final Map<String, Object> images = (Map<String, Object>) post.get("images");
                final Map<String, Object> image460sv = (Map<String, Object>) images.get("image460sv");
                if (image460sv != null && image460sv.get("url") != null) {
                    video = true;
                    dllink = (String) image460sv.get("url");
                }
                title = post.get("title").toString();
                description = post.get("description").toString();
            }
        }
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            title = id;
        }
        if (StringUtils.isEmpty(dllink)) {
            dllink = br.getRegex("rel\\s*=\\s*\"image_src\"\\s*href\\s*=\\s*\"(https?[^<>\"]*?)\"").getMatch(0);
        }
        title = Encoding.htmlDecode(title);
        title = title.trim();
        final String ext = getFileNameExtensionFromString(dllink, video ? ".mp4" : ".jpg");
        link.setFinalFileName(this.applyFilenameExtension(title, ext));
        if (!StringUtils.isEmpty(description) && link.getComment() == null) {
            link.setComment(description);
        }
        if (!StringUtils.isEmpty(dllink)) {
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, link.getFinalFileName(), ext);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
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
