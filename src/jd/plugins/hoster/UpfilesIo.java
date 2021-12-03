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

import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "upfiles.io" }, urls = { "https?://(?:www\\.)?upfiles\\.io/([A-Za-z0-9]+)" })
public class UpfilesIo extends PluginForHost {
    public UpfilesIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://upfiles.io/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(DownloadLink link) throws Exception {
        // requestFileInformation(link);
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String downloadUrl = getDownloadFileUrl(link, MethodName.handleFree);
        URLConnectionAdapter con = null;
        try {
            br.setFollowRedirects(true);
            con = br.openGetConnection(downloadUrl);
            if (!con.getContentType().contains("html")) {
                logger.info("This url is a directurl");
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con).trim()));
            } else {
                br.followConnection();
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadUrl, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    enum MethodName {
        requestFileInformation,
        handleFree
    }

    String getDownloadFileUrl(final DownloadLink link, MethodName type) throws Exception {
        String csrfToken = br.getRegex("csrf-token\" content=\"([^\"]+)\"").getMatch(0);
        UrlQuery query = new UrlQuery();
        query.add("_token", csrfToken);
        query.add("ccp", "1");
        query.add("action", "continue");
        br.postPage(link.getPluginPatternMatcher(), query);
        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LcsK9kaAAAAABe3I5PTS2zqmeKl3XueBrKNk3-Z").getToken();
        String view_form_data = br.getRegex("view_form_data\" value=\"([^\"]+)\"").getMatch(0);
        UrlQuery query2 = new UrlQuery();
        query2.add("_token", csrfToken);
        query2.add("view_form_data", view_form_data);
        query2.add("action", "captcha");
        query2.add("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        br.postPage(link.getPluginPatternMatcher(), query2);
        UrlQuery query3 = new UrlQuery();
        query3.add("_token", csrfToken);
        query3.add("view_form_data", view_form_data);
        if (type == MethodName.handleFree) {
            sleep(10 * 1000l, link);
        } else {
            Thread.sleep(10000);
        }
        br.postPage("https://upfiles.io/file/go", query3);
        return PluginJSonUtils.getJsonValue(br, "url");
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String downloadUrl = getDownloadFileUrl(link, MethodName.requestFileInformation);
        URLConnectionAdapter con = null;
        try {
            br.setFollowRedirects(true);
            con = br.openGetConnection(downloadUrl);
            if (!con.getContentType().contains("html")) {
                logger.info("This url is a directurl");
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con).trim()));
                return AvailableStatus.TRUE;
            } else {
                br.followConnection();
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}