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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jumbofiles.com" }, urls = { "http://(www\\.)?jumbofiles\\.com/[0-9a-z]{12}" }, flags = { 2 })
public class JumboFilesCom extends PluginForHost {

    private String              BRBEFORE    = "";
    private static final String COOKIE_HOST = "http://jumbofiles.com";
    private static final String NOCHUNKS    = "NOCHUNKS";
    private static final String NORESUME    = "NORESUME";
    private static String       UA          = RandomUserAgent.generate();

    public JumboFilesCom(final PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://jumbofiles.com/premium.html");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        br.setCookie("http://www.jumbofiles.com", "lang", "english");
        br.getPage(link.getDownloadURL());
        doSomething();
        if (BRBEFORE.contains("No such file") || BRBEFORE.contains("No such user exist") || BRBEFORE.contains("File not found") || BRBEFORE.contains(">File Not Found<")) {
            logger.warning("file is 99,99% offline, throwing \"file not found\" now...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = new Regex(BRBEFORE, "Filename:.*?</TD><TD>(.*?)</TD>").getMatch(0);
        String filesize = new Regex(BRBEFORE, "Filesize:.*?</TD><TD>(.*?)</TD>").getMatch(0);
        // They got different pages for stream, normal and pw-protected files so
        // we need special handling
        if (filesize == null) filesize = new Regex(BRBEFORE, "<small>\\((.*?)\\)</small>").getMatch(0);
        if (filesize == null) filesize = new Regex(BRBEFORE, "F<TD><center>.*?<br>(.*?)<br>").getMatch(0);
        if (filesize == null) filesize = new Regex(BRBEFORE, "You have requested.*?>http.*?font> \\((.*?)\\)").getMatch(0);

        if (filename == null) filename = new Regex(BRBEFORE, "down_direct\" value=.*?<input type=\"image\" src=.*?</TD></TR>.*?<TR><TD>(.*?)<small").getMatch(0);
        if (filename == null) filename = new Regex(BRBEFORE, "<TD><center>(.*?)<br>").getMatch(0);
        if (filename == null) filename = new Regex(BRBEFORE, "fname\" value=\"(.*?)\"").getMatch(0);

        if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        filename = filename.replace("&nbsp;", "");
        filename = filename.trim();
        link.setName(filename);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    // Removed fake messages which can kill the plugin
    public void doSomething() throws NumberFormatException, PluginException {
        BRBEFORE = br.toString();
        final ArrayList<String> someStuff = new ArrayList<String>();
        final ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        for (final String aRegex : regexStuff) {
            final String lolz[] = br.getRegex(aRegex).getColumn(0);
            if (lolz != null) {
                for (final String dingdang : lolz) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (final String fun : someStuff) {
            BRBEFORE = BRBEFORE.replace(fun, "");
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<TD>Premium-Account expire:</TD><TD><b>(.*?)</b>").getMatch(0);
        if (expire != null) {
            expire = expire.replaceAll("(<b>|</b>)", "");
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://jumbofiles.com/tos.html";
    }

    private String getDllink() {
        String linkurl = br.getRegex("\"(http://[a-zA-Z0-9]+\\.jumbofiles\\.com(:\\d+)?/files/.*?/.*?/.*?)\"").getMatch(0);
        if (linkurl == null) {
            linkurl = br.getRegex("href=\"(http://[0-9]+\\..*?:[0-9]+/d/.*?/.*?)\"").getMatch(0);
            if (linkurl == null) {
                linkurl = br.getRegex("</td></tr></table>[\t\n\r ]+<br>[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
            }
            if (linkurl == null) {
                linkurl = br.getRegex("('|\")(http://(www\\d+|[a-z0-9]+)\\.jumbofiles\\.com(:\\d+)?/d/[a-z0-9]+/.*?)('|\")").getMatch(1);
            }
        }
        return linkurl;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(true);
        String dllink = null;
        // Form um auf "Datei herunterladen" zu klicken
        Form dlForm = br.getForm(0);
        if (dlForm == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        for (int i = 0; i <= 5; i++) {
            String passCode = null;
            if (BRBEFORE.contains("<b>Password:</b>")) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = getUserInput(null, downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                dlForm.put("password", passCode);
            }
            br.submitForm(dlForm);
            doSomething();
            if (BRBEFORE.contains("Wrong password")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
            dllink = getDllink();
            if (dllink == null && br.containsHTML("(?i)<Form name=\"F1\" method=\"POST\" action=\"\"")) {
                dlForm = br.getFormbyProperty("name", "F1");
                continue;
            } else if (dllink == null && !br.containsHTML("(?i)<Form name=\"F1\" method=\"POST\" action=\"\"")) {
                handleErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                break;
            }
        }
        boolean resume = true;
        int chunks = -5;
        if (downloadLink.getBooleanProperty(JumboFilesCom.NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, chunks);
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(JumboFilesCom.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(JumboFilesCom.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(JumboFilesCom.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(JumboFilesCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String passCode = null;
        requestFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            Form DLForm = br.getFormbyProperty("name", "F1");
            if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (br.containsHTML("(<b>Passwort:</b>|<b>Password:</b>)")) {
                logger.info("The downloadlink seems to be password protected.");
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                DLForm.put("password", passCode);
                logger.info("Put password \"" + passCode + "\" entered by user in the DLForm and submitted it.");
            }
            br.submitForm(DLForm);
            if (br.containsHTML("(Wrong password|<b>Passwort:</b>|<b>Password:</b>)")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            dllink = br.getRedirectLocation();
            if (dllink == null) dllink = getDllink();
        }
        if (dllink == null) {
            handleErrors();
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        int chunks = -5;
        boolean resume = true;
        if (downloadLink.getBooleanProperty(JumboFilesCom.NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, chunks);
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(JumboFilesCom.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(JumboFilesCom.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(JumboFilesCom.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(JumboFilesCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    private void handleErrors() throws PluginException {
        if (br.containsHTML(">Error happened when generating Download Link.<br>Please try again or Contact administrator")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l); }
    }

    private void login(final Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", UA);
        br.setCookie(COOKIE_HOST, "lang", "english");
        br.getPage(COOKIE_HOST + "/login.html");
        Form loginform = br.getForm(0);
        if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        loginform.put("login", Encoding.urlEncode(account.getUser()));
        loginform.put("password", Encoding.urlEncode(account.getPass()));
        loginform.put("x", String.valueOf((int) (Math.random() * 57)));
        loginform.put("y", String.valueOf((int) (Math.random() * 21)));
        br.submitForm(loginform);
        br.getPage(COOKIE_HOST + "/?op=my_account");
        if (br.getCookie(COOKIE_HOST, "login") == null || br.getCookie(COOKIE_HOST, "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}