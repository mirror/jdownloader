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

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Random;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "worldclips.ru" }, urls = { "https?://(?:(www|dev)\\.)?worldclips\\.ru/clips/[^<>\"/]*?/[^<>\"/]+" })
public class WorldClipsRu extends PluginForHost {
    public WorldClipsRu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://worldclips.ru/info";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("dev.worldclips.ru/", "worldclips.ru/"));
    }

    private static final String TYPE_INVALID = "https?://[^/]+/clips/(top|new)/\\d+";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        if (link.getDownloadURL().matches(TYPE_INVALID)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<font size=\"8\">404</font><h2|> К сожалению, запрашиваемая страница не найдена<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String off = br.getRegex("<tr class=\"off\">(.*?)</tr>").getMatch(0);
        if (off == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] rows = new Regex(off, "<td>([^<>\"]*?)</td>").getColumn(0);
        if (rows == null || rows.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Regex urlFilename = new Regex(link.getDownloadURL(), "worldclips\\.ru/clips/([^<>\"/]*?)/([^<>\"/]+)");
        String filename = urlFilename.getMatch(0) + " - " + urlFilename.getMatch(1) + "." + rows[1];
        String filesize = rows[0];
        filesize = filesize.replace("Г", "G");
        filesize = filesize.replace("М", "M");
        filesize = filesize.replaceAll("(к|К)", "k");
        filesize = filesize.replaceAll("(Б|б)", "");
        filesize = filesize + "b";
        try {
            final String charSet = br.getHttpConnection().getCharset();
            /* everything works with UTF-8, if you have different charset, then you have to use this than default one */
            filename = URLDecoder.decode(filename, charSet);
            link.setFinalFileName(filename.trim());
        } catch (final Throwable e) {
            link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        }
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        throw new AccountRequiredException();
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Do not validate cookies */
                        return;
                    }
                    br.getPage("http://" + this.getHost());
                    if (this.isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Performing full login");
                br.postPage("http://" + this.getHost() + "/personal", "expire=on&auth=1&mail=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedIN(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("/personal/exit")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new AccountInvalidException("Bitte gib deine E-Mail Adresse ins Benutzername Feld ein!");
            } else {
                throw new AccountInvalidException("Please enter your e-mail address in the username field!");
            }
        }
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        final String clipID = getClipID();
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, br.getURL(), "download_clip_id=" + clipID + "&x=" + new Random().nextInt(100) + "&y=" + new Random().nextInt(100), true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getClipID() throws PluginException {
        String clipID = br.getRegex("name=\"download_clip_id\" value=\"(\\d+)\"").getMatch(0);
        if (clipID == null) {
            clipID = br.getRegex("name=\"clip_id\" value=\"(\\d+)\"").getMatch(0);
        }
        if (clipID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return clipID;
    }

    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        /* Account required for downloading */
        if (account != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}