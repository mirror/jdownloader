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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class InfidriveNetCrawler extends PluginForDecrypt {
    public InfidriveNetCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "infidrive.net" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/file/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final String contentID = new Regex(contenturl, this.getSupportedLinks()).getMatch(0);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("File Not Found\\s*</h1>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"text-xl font-semibold\"[^>]*>([^<]+)").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Download ([^<]+) \\| InfiDrive</title>").getMatch(0);
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
        }
        final String filesizeStr = br.getRegex("Size:\\s*<b>([^<]+)</b>").getMatch(0);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String unescapedhtml = PluginJSonUtils.unescape(br.getRequest().getHtmlCode());
        final String[] fileids = new Regex(unescapedhtml, "file_id\":\"([^\"]+)").getColumn(0);
        if (fileids != null && fileids.length > 0) {
            /* Old way which worked when they were still working with public Google Drive fileIDs. */
            for (final String fileid : fileids) {
                ret.add(createDownloadlink(GoogleDriveCrawler.generateFileURL(fileid, null)));
            }
        }
        final String htmlWithUnescapedJson = PluginJSonUtils.unescape(br.getRequest().getHtmlCode());
        final Pattern directurlpattern = Pattern.compile("\"url\":\"(https?://[^\"]+)", Pattern.CASE_INSENSITIVE);
        String directurl = new Regex(htmlWithUnescapedJson, directurlpattern).getMatch(0);
        /* Maybe captcha required */
        if (directurl == null) {
            final boolean allowCaptchaHandling = true;
            if (!allowCaptchaHandling) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String userEmail = PluginJSonUtils.getJson(htmlWithUnescapedJson, "userEmail");
            if (StringUtils.isEmpty(userEmail)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String rcKey = "6Ld5isIbAAAAAK2KVX86ymkA8NDDWSGtyGIfyz8H"; // 2024-08-22
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, rcKey).getToken();
            final PostRequest request = br.createJSonPostRequest(br.getURL(), "[\"" + PluginJSonUtils.escape(recaptchaV2Response) + "\"]");
            br.getHeaders().put("Accept", "text/x-component");
            br.getHeaders().put("Origin", "https://" + br.getHost(true));
            request.setContentType("text/plain;charset=UTF-8");
            br.getPage(request);
            final String htmlWithUnescapedJson2 = PluginJSonUtils.unescape(br.getRequest().getHtmlCode());
            String googleDriveFileID = br.getRegex("0:\\[\"[^\"]+\",\\[\"([^\"]+)").getMatch(0);
            if (googleDriveFileID == null) {
                googleDriveFileID = PluginJSonUtils.getJson(htmlWithUnescapedJson2, "file_id");
            }
            directurl = new Regex(htmlWithUnescapedJson2, directurlpattern).getMatch(0);
            if (StringUtils.isEmpty(googleDriveFileID)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (this.isAbort()) {
                throw new InterruptedException();
            }
            final boolean additionalRequestNeeded = true;
            if (additionalRequestNeeded) {
                final DownloadLink gdrivelink = createDownloadlink(GoogleDriveCrawler.generateFileURL(googleDriveFileID, null));
                ret.add(gdrivelink);
                distribute(gdrivelink);
                final Map<String, Object> postdata = new HashMap<String, Object>();
                postdata.put("filename", filename);
                postdata.put("id", googleDriveFileID);
                postdata.put("owner_email", userEmail);
                postdata.put("referer", "https://www.google.com/");
                postdata.put("slug", contentID);
                postdata.put("type", "direct");
                /* Can be null */
                postdata.put("url", directurl);
                final PostRequest request2 = br.createJSonPostRequest("/api/download", postdata);
                br.getPage(request2);
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                directurl = entries.get("dl").toString();
            }
        }
        final DownloadLink direct = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl));
        if (filename != null) {
            direct.setName(filename);
        }
        if (filesizeStr != null) {
            direct.setDownloadSize(SizeFormatter.getSize(filesizeStr));
        }
        direct.setAvailable(true);
        ret.add(direct);
        return ret;
    }
}
