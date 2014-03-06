//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileflyer.com" }, urls = { "http://[\\w\\.]*?fileflyer\\.com/view/[\\w]+" }, flags = { 2 })
public class FileFlyerCom extends PluginForHost {

    private static Object       LOCK         = new Object();

    private static final String ONLY4PREMIUM = "(Access to old files is available to premium users only|>This is an Always Premium Download<|The requested file is over 1 GB and thus is available for download to premium users only|have no free download availability, unlike most other downloads on Fileflyer)";

    public FileFlyerCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        this.enablePremium("http://www.fileflyer.com/");
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        synchronized (LOCK) {
            AccountInfo ai = new AccountInfo();
            ai.setStatus("Status can only be checked while downloading!");
            account.setValid(true);
            return ai;
        }
    }

    public String getAGBLink() {
        return "http://www.fileflyer.com/legal/terms.aspx";
    }

    private String getDllink() {
        String linkurl = br.getRegex(Pattern.compile("<a id=\"ItemsList_ctl00_(img|file)\".*href=\"([^\"\\'<>]+)\"")).getMatch(1);
        if (linkurl == null) linkurl = br.getRegex(Pattern.compile("\"(http://dsa?\\d+\\.fileflyer\\.com/d/[a-z0-9\\-]+/[^\"\\'<>]+)\"")).getMatch(0);
        return linkurl;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.setCookie("http://www.fileflyer.com", "lang", "en");
        br.getPage(downloadLink.getDownloadURL());
        String filesize = br.getRegex(Pattern.compile("id=\"ItemsList_ctl00_size\">(.*?)</span>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String name = br.getRegex(Pattern.compile("id=\"ItemsList_ctl00_file\" title=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (name == null) name = br.getRegex(Pattern.compile("id=\"ItemsList_ctl00_img\" title=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (br.containsHTML(ONLY4PREMIUM)) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.FileFlyerCom.errors.Only4Premium", "Only downloadable for premium users"));
        if (br.containsHTML("(class=\"handlink\">Expired</a>|class=\"handlink\">Removed</a>|>To report a bug ,press this link</a>|>Expired</a>|class=\"removedlink\")")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (name == null || filesize == null) {
            if (!br.containsHTML("class=\"fileslistwrap\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setName(name.trim().replace("", ""));
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(ONLY4PREMIUM)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        if (br.containsHTML("serveroverload")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server overloaded!", 10 * 60 * 1000l);
        if (br.containsHTML("access to the service may be unavailable for a while")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available", 30 * 60 * 1000l);
        String linkurl = getDllink();
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        linkurl = Encoding.htmlDecode(linkurl);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 0);
        if (dl.getConnection().getContentType() == null || (dl.getConnection().getContentType().contains("html")) && dl.getConnection().getLongContentLength() < downloadLink.getDownloadSize() - 10) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        br.setDebug(true);
        String linkurl = null;
        synchronized (LOCK) {
            if (!account.isValid()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            AccountInfo ai = account.getAccountInfo();
            if (ai == null) {
                ai = new AccountInfo();
                account.setAccountInfo(ai);
            }
            Form form = br.getForm(0);
            form.remove("convertor");
            form.put("TextBox1", "");
            form.put("Password", Encoding.urlEncode(account.getPass()));
            br.submitForm(form);
            // Invalid account?
            if (br.containsHTML("(class=\"badpassword\">Wrong password|Wrong password\\. Try again\\.<)")) {
                account.setValid(false);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            linkurl = getDllink();
            if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            /* account valid */
            ai.setStatus("Account valid!");
        }
        if (br.containsHTML("serveroverload")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server overloaded!", 10 * 60 * 1000l);
        if (br.containsHTML("access to the service may be unavailable for a while")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available", 30 * 60 * 1000l);
        linkurl = Encoding.htmlDecode(linkurl);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 0);
        if (dl.getConnection().getContentType() == null || (dl.getConnection().getContentType().contains("html")) && dl.getConnection().getLongContentLength() < downloadLink.getDownloadSize() - 10) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}