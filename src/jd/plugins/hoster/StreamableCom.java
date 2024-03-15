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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class StreamableCom extends PluginForHost {
    public StreamableCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public String getAGBLink() {
        return "https://terms.streamable.com/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "streamable.com" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9\\-_]+)");
        }
        return ret.toArray(new String[0]);
    }

    private static final String PROPERTY_DIRECTLINK = "directlink";

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
        final String extDefault = ".mp4";
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFID(link) + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> videomap1 = null;
        Map<String, Object> videomap2 = null;
        final String[] jsons = br.getRegex("<script>self\\.__next_f\\.push\\((.*?)\\)</script>").getColumn(0);
        if (jsons != null && jsons.length > 0) {
            for (final String json : jsons) {
                final Object object = JSonStorage.restoreFromString(json, TypeRef.OBJECT);
                if (object instanceof LinkedList) {
                    final LinkedList<Object> list = (LinkedList<Object>) object;
                    for (final Object item : list) {
                        if (!(item instanceof String)) {
                            continue;
                        }
                        String itemstr = item.toString();
                        itemstr = itemstr.replaceFirst("^f:", "");
                        if (itemstr.startsWith("{") || itemstr.startsWith("[")) {
                            final Object obj = restoreFromString(itemstr, TypeRef.OBJECT);
                            if (videomap1 == null) {
                                videomap1 = (Map<String, Object>) this.findVideomap1(obj);
                            }
                            if (videomap2 == null) {
                                videomap2 = (Map<String, Object>) this.findVideomap2(obj, "NOT_NEEDED");
                            }
                        }
                    }
                } else if (object instanceof List) {
                    if (true) {
                        // Unfinished/unneeded code
                        continue;
                    }
                    final ArrayList<Object> list = (ArrayList<Object>) object;
                    for (final Object item : list) {
                        // System.out.print(item);
                    }
                }
                if (videomap1 != null && videomap2 != null) {
                    break;
                }
            }
        }
        if (videomap1 == null && videomap2 == null) {
            /* E.g. if user adds non-video URLs such as: https://streamable.com/login */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = null;
        String directurl = null;
        boolean looksLikeOffline = false;
        if (videomap2 != null) {
            /* This map contains more information */
            title = (String) videomap2.get("title");
            final Map<String, Object> files = (Map<String, Object>) videomap2.get("files");
            if (files != null) {
                final Map<String, Object> files_mp4 = (Map<String, Object>) files.get("mp4");
                final long filesize = ((Number) files_mp4.get("size")).longValue();
                if (filesize > 0) {
                    link.setDownloadSize(filesize);
                    /* Do NOT set verifiedFilesize here - this one isn't safe! */
                }
                directurl = files_mp4.get("url").toString();
            } else {
                looksLikeOffline = true;
            }
        } else {
            title = videomap1.get("name").toString();
            directurl = videomap1.get("contentUrl").toString();
        }
        if (!StringUtils.isEmpty(title)) {
            title = title.replaceFirst("(?i) \\| Streamable$", "");
            link.setFinalFileName(title + extDefault);
        }
        if (directurl != null) {
            directurl = br.getURL(directurl).toExternalForm();
            link.setProperty(PROPERTY_DIRECTLINK, directurl);
        }
        /* Check here because title of offline videos might still be available. */
        if (directurl == null && looksLikeOffline) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    private Object findVideomap1(final Object o) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            if (entrymap.containsKey("@context")) {
                return entrymap;
            }
            for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                // final String key = entry.getKey();
                final Object value = entry.getValue();
                if (value instanceof List || value instanceof Map) {
                    final Object ret = findVideomap1(value);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object res = findVideomap1(arrayo);
                    if (res != null) {
                        return res;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private Object findVideomap2(final Object o, final String videoid) {
        if (videoid == null) {
            return null;
        }
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            if (entrymap.containsKey("shortcode") && entrymap.containsKey("width")) {
                return entrymap;
            }
            for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                // final String key = entry.getKey();
                final Object value = entry.getValue();
                if (value instanceof List || value instanceof Map) {
                    final Object ret = findVideomap2(value, videoid);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object res = findVideomap2(arrayo, videoid);
                    if (res != null) {
                        return res;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link);
    }

    private void handleDownload(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String dllink = link.getStringProperty(PROPERTY_DIRECTLINK);
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find final downloadurl");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}