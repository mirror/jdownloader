//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ZoomUs extends antiDDoSForHost {
    public ZoomUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "zoom.us" });
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
            ret.add("https?://(?:[A-Za-z0-9-]+\\.)?" + buildHostsPatternPart(domains) + "/rec/(?:play|share)/([A-Za-z0-9\\-_\\.]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://zoom.us/de-de/terms.html";
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

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    /** See also: https://github.com/Battleman/zoomdl */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean handleDownloadPassword) throws Exception {
        final String ext = ".mp4";
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ext);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String error = br.getRegex("class=\"error-message\"[^>]*>(.*?)<").getMatch(0);
        if (error != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (passwordRequired(br)) {
            /* Do not ask user for password during availablecheck. */
            link.setPasswordProtected(true);
            final String meetingID = br.getRegex("meeting_id\\s*:\\s*\"([^\"]+)\"").getMatch(0);
            final String useWhichPasswd = br.getRegex("useWhichPasswd\\s*:\\s*\"([^\"]+)\"").getMatch(0);
            final String sharelevel = br.getRegex("sharelevel\\s*:\\s*\"([^\"]+)\"").getMatch(0);
            if (!handleDownloadPassword) {
                /* Do not ask user for password here but at least find file title if possible. */
                if (meetingID != null && useWhichPasswd != null && sharelevel != null) {
                    logger.info("Trying to find title of password protected item");
                    final Form prepwform = new Form();
                    prepwform.setAction("/nws/recording/1.0/validate-context");
                    prepwform.setMethod(MethodType.POST);
                    prepwform.put("meetingId", Encoding.urlEncode(meetingID));
                    prepwform.put("fileId", "");
                    prepwform.put("useWhichPasswd", Encoding.urlEncode(useWhichPasswd));
                    prepwform.put("sharelevel", Encoding.urlEncode(sharelevel));
                    this.submitForm(prepwform);
                    final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    final Map<String, Object> result = (Map<String, Object>) entries.get("result");
                    if (result != null) {
                        final String topic = (String) result.get("topic");
                        if (!StringUtils.isEmpty(topic)) {
                            link.setName(topic + ext);
                        }
                    }
                }
                return AvailableStatus.TRUE;
            }
            if (meetingID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String passCode = link.getDownloadPassword();
            if (passCode == null) {
                passCode = getUserInput("Password?", link);
            }
            final Form pwform = new Form();
            pwform.setAction("/nws/recording/1.0/validate-meeting-passwd");
            pwform.setMethod(MethodType.POST);
            pwform.put("id", Encoding.urlEncode(meetingID));
            pwform.put("passwd", Encoding.urlEncode(passCode));
            pwform.put("action", "viewdetailpage");
            pwform.put("recaptcha", "");
            this.submitForm(pwform);
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            /* E.g. invalid password: {"status":false,"errorCode":3302,"errorMessage":"Falscher Kenncode","result":null} */
            if (Boolean.FALSE.equals(entries.get("status"))) {
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            } else {
                /* Correct password! Item should now be downloadable. */
                link.setDownloadPassword(passCode);
                getPage(link.getPluginPatternMatcher());
            }
        }
        /* TODO: Add support for password protected items */
        String filename = br.getRegex("topic\\s*:\\s*\"([^<>\"]+)\"").getMatch(0);
        final String filesize = br.getRegex("fileSize\\:\\s*(?:\"|')(\\d+(\\.\\d{1,2})? [^\"\\']+)(?:\"|')").getMatch(0);
        if (!StringUtils.isEmpty(filename)) {
            filename = Encoding.htmlDecode(filename).trim();
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
            link.setFinalFileName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        final String dllink = br.getRegex("viewMp4Url\\s*:\\s*(?:\"|')(https://[^<>\"\\']+)(?:\"|')").getMatch(0);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        }
        dl.startDownload();
    }

    private boolean passwordRequired(final Browser br) {
        if (br.containsHTML("componentName\\s*:\\s*\"need-password\"")) {
            return true;
        } else {
            return false;
        }
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