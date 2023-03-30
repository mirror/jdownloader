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
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

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
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ZoomUs extends PluginForHost {
    public ZoomUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Connection stuff */
    private static final boolean free_resume        = true;
    private static final int     free_maxchunks     = 0;
    private static final int     free_maxdownloads  = -1;
    private final String         PROPERTY_DIRECTURL = "directurl";

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
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String meetingID = br.getRegex("meeting_id\\s*:\\s*\"([^\"]+)\"").getMatch(0);
        if (meetingID == null) {
            meetingID = br.getRegex("meetingID\\s*:\\s*(?:\"|\\')([^\"\\']+)").getMatch(0);
        }
        String fileId = br.getRegex("fileId\\s*:\\s*(?:\"|\\')([^\"\\']+)").getMatch(0);
        final String error = br.getRegex("class=\"error-message\"[^>]*>(.*?)<").getMatch(0);
        String topic = null;
        String displayFileName = null;
        if (error != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (fileId == null) {
            if (meetingID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            Map<String, Object> entries = accessPlayShareOnfo(br, meetingID);
            Map<String, Object> result = (Map<String, Object>) entries.get("result");
            if (!Boolean.TRUE.equals(result.get("canPlayFromShare"))) {
                /* Usually password protected item */
                final String componentName = result.get("componentName").toString();
                if (!componentName.equals("need-password")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String action = result.get("action").toString();
                final String useWhichPasswd = result.get("useWhichPasswd").toString();
                final String sharelevel = result.get("sharelevel").toString();
                final UrlQuery query = new UrlQuery();
                query.add("action", action);
                query.add("sharelevel", sharelevel);
                query.add("useWhichPasswd", useWhichPasswd);
                query.add("clusterId", result.get("clusterId").toString());
                query.add("componentName", componentName);
                query.add("meetingId", Encoding.urlEncode(meetingID));
                query.add("originRequestUrl", Encoding.urlEncode(link.getPluginPatternMatcher()));
                br.getPage("/rec/component-page?" + query.toString());
                if (!passwordRequired(br)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* Do not ask user for password during availablecheck. */
                link.setPasswordProtected(true);
                if (!handleDownloadPassword) {
                    /* Do not ask user for password here but at least find file title if possible. */
                    logger.info("Trying to find title of password protected item");
                    final Form prepwform = new Form();
                    prepwform.setAction("/nws/recording/1.0/validate-context");
                    prepwform.setMethod(MethodType.POST);
                    prepwform.put("meetingId", Encoding.urlEncode(meetingID));
                    prepwform.put("fileId", "");
                    prepwform.put("useWhichPasswd", Encoding.urlEncode(useWhichPasswd));
                    prepwform.put("sharelevel", Encoding.urlEncode(sharelevel));
                    final Browser br2 = br.cloneBrowser();
                    br2.submitForm(prepwform);
                    final Map<String, Object> entries3 = restoreFromString(br2.getRequest().getHtmlCode(), TypeRef.MAP);
                    final Map<String, Object> result3 = (Map<String, Object>) entries3.get("result");
                    if (result3 != null) {
                        topic = (String) result3.get("topic");
                        if (!StringUtils.isEmpty(topic)) {
                            link.setName(topic + ext);
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
                pwform.put("action", action);
                String recaptchaV2Response = "";
                if (this.requiresCaptcha(br)) {
                    final String reCaptchaSiteKey = br.getRegex("var gRecaptchaVisible\\s*=\\s*\"([^\"]+)").getMatch(0);
                    if (reCaptchaSiteKey == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaSiteKey).getToken();
                }
                pwform.put("recaptcha", Encoding.urlEncode(recaptchaV2Response));
                br.submitForm(pwform);
                final Map<String, Object> entries5 = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                /* E.g. invalid password: {"status":false,"errorCode":3302,"errorMessage":"Falscher Kenncode","result":null} */
                if (Boolean.FALSE.equals(entries5.get("status"))) {
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else {
                    /* Correct password! Item should now be downloadable. */
                    link.setDownloadPassword(passCode);
                    // br.getPage(link.getPluginPatternMatcher());
                    entries = accessPlayShareOnfo(br, meetingID);
                    result = (Map<String, Object>) entries.get("result");
                }
            }
            final String redirectUrl = result.get("redirectUrl").toString();
            if (StringUtils.isEmpty(redirectUrl)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(redirectUrl);
            fileId = br.getRegex("fileId\\s*:\\s*(?:\"|\\')([^\"\\']+)").getMatch(0);
            if (fileId == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.getPage("/nws/recording/1.0/play/info/" + fileId + "?canPlayFromShare=true&from=share_recording_detail&continueMode=true&componentName=rec-play&originRequestUrl=" + Encoding.urlEncode(link.getPluginPatternMatcher()));
        final Map<String, Object> entries2 = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> result2 = (Map<String, Object>) entries2.get("result");
        link.setProperty(PROPERTY_DIRECTURL, result2.get("viewMp4Url"));
        final Map<String, Object> meet = (Map<String, Object>) result2.get("meet");
        topic = (String) meet.get("topic");
        final Map<String, Object> recording = (Map<String, Object>) result2.get("recording");
        displayFileName = (String) recording.get("displayFileName");
        final String fileSizeInMB = (String) recording.get("fileSizeInMB");
        if (fileSizeInMB != null) {
            link.setDownloadSize(SizeFormatter.getSize(fileSizeInMB));
        }
        if (topic != null && displayFileName != null) {
            link.setFinalFileName(topic + " - " + displayFileName + ext);
        } else if (displayFileName != null) {
            link.setFinalFileName(displayFileName + ext);
        } else if (topic != null) {
            link.setFinalFileName(topic + ext);
        }
        return AvailableStatus.TRUE;
    }

    private Map<String, Object> accessPlayShareOnfo(final Browser br, final String meetingID) throws IOException {
        br.getPage("/nws/recording/1.0/play/share-info/" + Encoding.urlEncode(meetingID));
        return restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(dllink)) {
            throwExceptionOnCaptcha(br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            }
        }
        dl.startDownload();
    }

    private void throwExceptionOnCaptcha(final Browser br) throws PluginException {
        if (requiresCaptcha(br)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Captcha required: Try again later");
        }
    }

    private boolean requiresCaptcha(final Browser br) {
        return br.containsHTML("(?i)needRecaptcha\\s*:\\s*true");
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