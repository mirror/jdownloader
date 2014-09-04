//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 21813 $", interfaceVersion = 2, names = { "datator.cz" }, urls = { "thisbreakspluginhttp://(www\\.)?datator\\.cz/soubor-ke-stazeni-.*\\d+\\.html" }, flags = { 2 })
public class DataTorCz extends PluginForHost {

    // devnotes
    // number before .html is the file uid
    // to make valid link soubor-ke-stazeni- + number + .html

    public DataTorCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.datator.cz/clanek-vseobecne-podminky-3.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        final String msg = "účet Povinné - Account Required";
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, msg, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, msg);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        // offline
        if (br.getURL().endsWith("datator.cz/404.html")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        final String[] fileInfo = br.getRegex("content=\".*?: (.*?) \\| \\d{1,2}\\.\\d{1,2}\\.\\d{4} \\|  ([\\d A-Z]+)\" />").getRow(0);
        if (fileInfo != null) {
            if (fileInfo[1] != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(fileInfo[1].replaceAll("\\s", "")));
            }
            if (fileInfo[0] != null) {
                downloadLink.setFinalFileName(fileInfo[0]);
            } else {
                return AvailableStatus.FALSE;
            }
        }
        // In case the link redirects to the finallink
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.postPage("http://www.datator.cz/prihlaseni.html", "username=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&permLogin=on&Ok=Log+in&login=1");

        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
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