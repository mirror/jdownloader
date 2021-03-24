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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.OneFichierConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class MyStreamTo extends PluginForHost {
    public MyStreamTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://mystream.la/terms-of-service";
    }

    public static final String getDefaultAnnotationPatternPart() {
        return "/(?:external/|embed-)?([A-Za-z0-9]{12})";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.|embed\\.)?" + buildHostsPatternPart(domains) + MyStreamTo.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mystream.to", "mystream.la", "mstream.xyz", "mstream.cloud", "mstream.fun", "mstream.press" });
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
        return MyStreamTo.buildAnnotationUrls(getPluginDomains());
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(getFID(link) + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 500 });
        br.getPage("https://embed." + this.getHost() + "/" + this.getFID(link));
        if (br.containsHTML(">\\s*File Not Found<|The video has been blocked|The file you were looking for could not be found|>The file was deleted by administration because|File was deleted|>We are unable to find the video you're looking for") || br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = PluginJSonUtils.getJsonValue(br, "title");
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("title\\s*:\\s*'([^<>\"\\']+)'").getMatch(0);
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            filename = encodeUnicode(filename);
            final String ext = getFileNameExtensionFromString(filename, ".mp4");
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
            link.setFinalFileName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        final boolean resume = true;
        final int maxChunks = -2;
        if (!attemptStoredDownloadurlDownload(link, "directlink", resume, maxChunks)) {
            requestFileInformation(link);
            final String dllink = getDllink();
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("directlink", br.getURL());
        }
        dl.startDownload();
    }

    private String getDllink() {
        final String encodedjs = new Regex(br.toString(), Pattern.compile("(\\$=.+?;)\\s*<", Pattern.DOTALL)).getMatch(0);
        final int beginning = encodedjs.indexOf("\"\\\"\"+") + 5;
        final int end = encodedjs.indexOf("\"\\\"\")())()");
        String group1 = encodedjs.substring(beginning, end);
        final String pos = new Regex(group1, "(\\(\\!\\[\\]\\+\\\"\\\"\\)\\[.+?\\]\\+)").getMatch(0);
        group1 = group1.replace(pos, "l").replace("$.__+", "t").replace("$._+", "u").replace("$._$+", "o");
        final String js = new Regex(encodedjs, "(\\$=\\{.+?\\});").getMatch(0);
        final String js1 = js.substring(3, js.length() - 1);
        final String[] jsgroups = js1.split(",");
        final HashMap<String, String> tmpMap = new HashMap<String, String>();
        int i = -1;
        for (final String jsgroup : jsgroups) {
            final String[] groupmembers = jsgroup.split(":");
            final String a = groupmembers[0];
            final String b = groupmembers[1];
            if (b.equals("++$")) {
                i += 1;
                tmpMap.put(String.format("$.%s+", a), Integer.toString(i));
            } else if (b.equals("(![]+\"\")[$]")) {
                tmpMap.put(String.format("$.%s+", a), Character.toString("false".charAt(i)));
            } else if (b.equals("({}+\"\")[$]")) {
                tmpMap.put(String.format("$.%s+", a), Character.toString("[object Object]".charAt(i)));
            } else if (b.equals("($[$]+\"\")[$]")) {
                tmpMap.put(String.format("$.%s+", a), Character.toString("undefined".charAt(i)));
            } else if (b.equals("(!\"\"+\"\")[$]")) {
                tmpMap.put(String.format("$.%s+", a), Character.toString("true".charAt(i)));
            }
        }
        final TreeMap<String, String> sortedMap = new TreeMap<String, String>();
        for (Map.Entry<String, String> entry : tmpMap.entrySet()) {
            sortedMap.put(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
            group1 = group1.replace(entry.getValue(), entry.getKey());
        }
        group1 = group1.replace("\\\"", "\\");
        group1 = group1.replace("\"\\\\\\\\\"", "\\\\");
        group1 = group1.replace("\\\"", "\\");
        group1 = group1.replace("\"", "");
        group1 = group1.replace("+", "");
        for (int r = 255; r >= 0; r--) {
            if (group1.contains("\\\\" + r)) {
                final char replacement = (char) Long.parseLong(String.valueOf(r), 8);
                group1 = group1.replace("\\\\" + r, String.valueOf(replacement));
            }
        }
        return new Regex(group1, "'src'\\s*,\\s*'(https?://.*?)'").getMatch(0);
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String property, final boolean resume, final int maxchunks) throws Exception {
        String url = link.getStringProperty(property);
        if (StringUtils.isEmpty(url)) {
            return false;
        } else {
            final boolean preferSSL = PluginJsonConfig.get(OneFichierConfigInterface.class).isPreferSSLEnabled();
            if (preferSSL && url.startsWith("http://")) {
                url = url.replace("http://", "https://");
            }
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e2) {
            }
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
