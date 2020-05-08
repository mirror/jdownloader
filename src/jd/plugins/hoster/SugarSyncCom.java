//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sugarsync.com" }, urls = { "https?://(?:www\\.)?sugarsync\\.com/pf/(D[\\d\\_]+)" })
public class SugarSyncCom extends PluginForHost {
    public SugarSyncCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.sugarsync.com/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
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

    private String dllink = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("https://www.sugarsync.com/", "lang", "en");
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getPluginPatternMatcher());
            if (con.getContentType().contains("html")) {
                br.followConnection();
            } else {
                /* We have a directlink */
                dllink = link.getPluginPatternMatcher();
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                return AvailableStatus.TRUE;
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"pf-error-information-message\"")) {
            /* 2020-05-08 e.g. <div class="pf-main-error-message">Die Datei ist nicht mehr verfügbar.</div> */
            /*
             * or <div class="pf-error-information-message">Die Datei, auf die Sie zugreifen wollen, ist nicht mehr zum Herunterladen
             * verfügbar.<br/>Wenden Sie sich an den Besitzer, wenn Sie Fragen haben.</div>
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // br.getPage(link.getPluginPatternMatcher());
        final String sessionid = br.getCookie(br.getHost(), "JSESSIONID");
        final String somevaluesid = br.getRegex("id=\"someValuesId\" value=\"([^<>\"]+)\"").getMatch(0);
        if (StringUtils.isEmpty(sessionid)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String fid = getFID(link);
        final UrlQuery query = new UrlQuery();
        query.add("callCount", "1");
        query.add("page", "/pf/" + fid);
        query.add("httpSessionId", sessionid);
        query.add("scriptSessionId", "");
        query.add("c0-scriptName", "publicLinkPage");
        query.add("c0-methodName", "landingPagePublicLink");
        query.add("c0-id", "0");
        query.add("c0-param0", "boolean:false");
        query.add("c0-param1", "boolean:false");
        query.add("c0-param2", "string:https%3A%2F%2Fwww.sugarsync.com%2Fpf%2F" + fid);
        query.add("c0-param3", "string:" + somevaluesid);
        query.add("batchId", "0");
        br.postPageRaw("https://www.sugarsync.com/dwr/call/plaincall/publicLinkPage.landingPagePublicLink.dwr", query.toString());
        br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.toString()));
        if (br.getHttpConnection().getResponseCode() == 404) {
            // || br.containsHTML("class=\"pf-error-icon\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("error_no_public\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = PluginJSonUtils.getJson(br, "jsonPublicFileName");
        String filesize = PluginJSonUtils.getJson(br, "publicFileSize");
        if (filename != null) {
            link.setFinalFileName(filename.trim());
        }
        if (!StringUtils.isEmpty(filesize)) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        if (dllink == null) {
            final String token = PluginJSonUtils.getJson(br, "token");
            if (StringUtils.isEmpty(token)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String fid = getFID(link);
            dllink = String.format("https://www.sugarsync.com/pf/%s?_=0.%d&token=%s&customData=%s", fid, System.currentTimeMillis(), token, token);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("freelink", dllink);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}