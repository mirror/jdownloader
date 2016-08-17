//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "qq.com" }, urls = { "http://qqdecrypted\\.com/\\d+" }) 
public class QqCom extends PluginForHost {

    public QqCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.qq.com/contract.shtml";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String NOCHUNKS               = "NOCHUNKS";
    private String              type                   = null;
    public static final String  PROPERTY_TYPE          = "type";
    public static final String  PROPERTY_MUSIC_SONGID  = "songid";
    public static final String  PROPERTY_MUSIC_ALBUMID = "albumid";
    public static final String  TYPE_MUSIC             = "music";
    public static final String  TYPE_STANDARD          = "standard";

    private String              dllink                 = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        type = link.getStringProperty("type", TYPE_STANDARD);
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        if (type.equals(TYPE_MUSIC)) {
            this.br.setCookie("http://qq.com", "qqmusic_uin", "12345678");
            this.br.setCookie("http://qq.com", "qqmusic_key", "12345678");
            /* Server needs these cookies but accepts random values. */
            this.br.setCookie("http://qq.com", "pgv_pvid", "" + System.currentTimeMillis());
            this.br.setCookie("http://qq.com", "pgv_info", "ssid=s" + System.currentTimeMillis());
            this.br.setCookie("http://qq.com", "qqmusic_fromtag", "30");
            // this.br.setCookie("http://qq.com", "", "");
            // this.br.setCookie("http://qq.com", "", "");
            // this.br.setCookie("http://qq.com", "", "");
            this.dllink = "http://stream11.qqmusic.qq.com/" + link.getStringProperty(PROPERTY_MUSIC_SONGID, null) + ".mp3";
            this.br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                try {
                    /* HEAD request does not work! */
                    con = br.openGetConnection(this.dllink);
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            br.getPage(link.getStringProperty("mainlink", null));
            if (br.containsHTML(">很抱歉，此资源已被删除或包含敏感信息不能查看啦<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String originalqhref = link.getStringProperty("qhref", null);
            final String[] qhrefs = br.getRegex("qhref=\"(qqdl://[^<>\"]+)\"").getColumn(0);
            if (qhrefs == null || qhrefs.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            boolean failed = true;
            for (final String currentqhref : qhrefs) {
                if (currentqhref.equals(originalqhref)) {
                    failed = false;
                    break;
                }
            }
            if (failed) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        int maxChunks = 0;
        if (type.equals(TYPE_MUSIC)) {
        } else {
            final String hash = downloadLink.getStringProperty("filehash", null);
            // This should never happen
            if (hash == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("User-Agent", "Mozilla/4.0 (compatible; MSIE 9.11; Windows NT 6.1; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E)");
            br.postPage("http://fenxiang.qq.com/upload/index.php/share/handler_c/getComUrl", "filename=" + Encoding.urlEncode(downloadLink.getFinalFileName()) + "&filehash=" + hash);
            if (br.containsHTML("No htmlCode read")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            dllink = br.getRegex("\"com_url\":\"(htt[^<>\"]*?)\"").getMatch(0);
            final String cookie = br.getRegex("\"com_cookie\":\"([^<>\"]*?)\"").getMatch(0);
            if (dllink == null || cookie == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String finalhost = new Regex(dllink, "(https?://[A-Za-z0-9\\-\\.]+)(:|/)").getMatch(0);
            br.setCookie(finalhost, "FTN5K", cookie);

            /* Get better speeds - source: https://github.com/rhyzx/xuanfeng-userscript/blob/master/xuanfeng.user.js */
            final Regex server_regex = new Regex(dllink, "(xflx\\.store\\.([a-z0-9]+)\\.qq\\.com:443)");
            final String old_Server = server_regex.getMatch(0);
            if (old_Server != null) {
                final String server = server_regex.getMatch(1);
                final String new_server = "xf" + server + ".ctfs.ftn.qq.com";
                dllink = dllink.replace(old_Server, new_server);
            }
            dllink = dllink.replace("xflx.store.cd.qq.com:443", "xfcd.ctfs.ftn.qq.com");
            dllink = dllink.replace("xflx.sz.ftn.qq.com:80", "sz.disk.ftn.qq.com");
            dllink = dllink.replace("xflx.cd.ftn.qq.com:80", "cd.ctfs.ftn.qq.com");
            dllink = dllink.replace("xflxsrc.store.qq.com:443", "xfxa.ctfs.ftn.qq.com");
        }

        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxChunks);
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Server error 503: Service Unavailable!");
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(QqCom.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(QqCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(QqCom.NOCHUNKS, false) == false) {
                downloadLink.setProperty(QqCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}