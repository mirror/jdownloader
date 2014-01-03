//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 22600 $", interfaceVersion = 2, names = { "fryhost.com" }, urls = { "http://(www\\.)?fryhost\\.com/[a-z0-9]+" }, flags = { 0 })
public class FryhostCom extends PluginForHost {

    private static AtomicInteger MAXPREMDLS         = new AtomicInteger(-1);
    private static final String  MAXDLSLIMITMESSAGE = "Sorry, you cant download more then";
    private static final String  LIMITREACHED       = "Your Traffic is used up for today|Der Traffic für heute ist verbraucht";

    public FryhostCom(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(1000l);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        System.out.println(link);
    }

    @Override
    public Boolean rewriteHost(Account acc) {
        return false;
    }

    @Override
    public boolean isPremiumEnabled() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage("http://www.fryhost.com/");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("Name:</b>(.*?)<br>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        final String filesize = br.getRegex("Size:</b>(.*?)<br>").getMatch(0);
        if (filesize != null) {
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        handleFreeErrors();
        br.setFollowRedirects(true);
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.remove("btn");
        sleep(30 * 1001l, downloadLink);
        handleFreeErrors();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form);
        if (!dl.getConnection().isContentDisposition()) {
            logger.info("The finallink is no file, trying to handle errors...");
            br.followConnection();
            handleOtherErrors(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        account.setMaxSimultanDownloads(1);
        account.setConcurrentUsePossible(false);
        return account.getAccountInfo();
    }

    @Override
    public String getAGBLink() {
        return "http://www.fryhost.com/tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return MAXPREMDLS.get();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void handleFreeErrors() throws PluginException {
        if (br.containsHTML("Sorry, you cant download more then 50 files at time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        if (br.containsHTML("You can Download only 1 File in")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        if (br.containsHTML("No Downloadserver\\. Please try again") || br.containsHTML("Downloadserver im Moment nicht erreichbar.")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Downloadserver. Please try again later", 15 * 60 * 1000l);
        if (br.containsHTML(LIMITREACHED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
    }

    private void handleOtherErrors(DownloadLink downloadLink) throws PluginException {
        if (br.containsHTML(MAXDLSLIMITMESSAGE)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Maximum concurrent download sessions reached.", 30 * 60 * 1000l); }
        if (br.containsHTML("File can not be found")) {
            logger.info("File for the following is offline (server error): " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(">404 Not Found<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
        if (br.containsHTML("bad try")) {
            logger.warning("Hoster said \"bad try\" which means that jd didn't wait enough time before trying to start the download!");
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (br.containsHTML("your Traffic is used up for today")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001); }
        if (br.containsHTML("No Downloadserver. Please try again") || br.containsHTML("Downloadserver im Moment nicht erreichbar.")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No Downloadserver. Please try again later", 15 * 60 * 1000l); }
        if (br.containsHTML("you cant  download more then 1 at time")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1001); }
        if (br.getURL().contains("section=filenotfound")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return false;
    }

    public void login(final Account account) throws IOException, PluginException {
        setBrowserExclusive();
        br.setCustomCharset("UTF-8");/* workaround for buggy server */
        br.setFollowRedirects(false);
        logger.info("JD couldn't find out the membership of this account!");
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {

    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}