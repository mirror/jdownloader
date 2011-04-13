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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "motherless.com" }, urls = { "http://([\\w\\.]*?|members\\.)(motherless\\.com/(movies|thumbs).*|(premium)?motherlesspictures(media)?\\.com/[a-zA-Z0-9/.]+|(premium)?motherlessvideos\\.com/[a-zA-Z0-9/.]+)" }, flags = { 2 })
public class MotherLessCom extends PluginForHost {

    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2500l);
        this.enablePremium("http://motherless.com/register");
    }

    public String getAGBLink() {
        return "http://motherless.com/terms";
    }

    private static final String SUBSCRIBEFAILED     = "Failed to subscribe";
    private static final String ONLY4REGISTEREDTEXT = "This link is only downloadable for registered users.";

    public void correctDownloadLink(DownloadLink link) {
        String theLink = link.getDownloadURL();
        theLink = theLink.replace("premium", "").replace("motherlesspictures", "motherless").replace("motherlessvideos", "motherless");
        link.setUrlDownload(theLink);
    }

    public AvailableStatus requestFileInformation(DownloadLink parameter) throws IOException, PluginException {
        if (parameter.getStringProperty("kind") != null) {
            logger.info(ONLY4REGISTEREDTEXT);
            parameter.getLinkStatus().setStatusText("This " + parameter.getStringProperty("kind") + " link can only be downloaded by registered users");
            return AvailableStatus.UNCHECKABLE;
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(parameter.getDownloadURL());
            if (con.getContentType().contains("html")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            parameter.setName(Plugin.getFileNameFromHeader(con));
            parameter.setDownloadSize(con.getLongContentLength());
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    public void handleFree(DownloadLink link) throws Exception {
        if (link.getStringProperty("kind") != null) {
            logger.info(ONLY4REGISTEREDTEXT);
            throw new PluginException(LinkStatus.ERROR_FATAL, ONLY4REGISTEREDTEXT);
        }
        if (!link.getDownloadURL().contains("/img/") && !link.getDownloadURL().contains("/dev")) {
            requestFileInformation(link);
        } else {
            // Access the page first to make the finallink valid
            String fileid = new Regex(link.getDownloadURL(), "/img/([A-Z0-9]+)").getMatch(0);
            if (fileid != null)
                br.getPage("http://motherless.com/" + fileid);
            else {
                br.getPage(link.getBrowserUrl());
            }
        }

        if (link.getDownloadURL().endsWith(".flv"))
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        else
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL());
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (link.getFinalFileName() == null) dl.setFilenameFix(true);
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.postPage("http://motherless.com/auth/login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember_me=1&__remember_me=0");
        if (br.getCookie("http://motherless.com/", "motherless_auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        String kind = link.getStringProperty("kind");
        if (kind != null) {
            br.getPage(link.getDownloadURL());
            if (br.containsHTML("<h1>Subscribers Only</h1>")) {
                String profileToSubscribe = br.getRegex("You can subscribe to the member from their <a href=\"(http://motherless\\.com/member/[a-zA-Z0-9]+)\">profile</a>").getMatch(0);
                if (profileToSubscribe == null) throw new PluginException(LinkStatus.ERROR_FATAL, SUBSCRIBEFAILED);
                br.getPage(profileToSubscribe + "/subscribe?");
                if (!br.containsHTML("<p>You are now subscribed to member")) throw new PluginException(LinkStatus.ERROR_FATAL, SUBSCRIBEFAILED);
                br.getPage(link.getDownloadURL());
            }
            // Here we use the code of the decrypter to find the urls.
            // If this part changes in the decrypter or here we have to update
            // both plugins!!
            if (kind.equals("picture")) {
                String finallink = br.getRegex("\"(http://members\\.motherless\\.com/img/.*?)\"").getMatch(0);
                if (finallink == null) {
                    finallink = br.getRegex("full_sized\\.jpg\" (.*?)\"(http://s\\d+\\.motherless\\.com/dev\\d+/\\d+/\\d+/\\d+/\\d+.*?)\"").getMatch(1);
                    if (finallink == null) {
                        finallink = br.getRegex("<div style=\"clear: left;\"></div>[\t\r\n ]+<img src=\"(http://.*?)\"").getMatch(0);
                    }
                }
                if (finallink == null) {
                    logger.warning("Failed to find the picture-downloadlink!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                link.setUrlDownload(finallink);
                link.setProperty("kind", Property.NULL);
            } else {
                String filelink = br.getRegex("s1\\.addParam\\(\\'flashvars\\',\\'file=(http://.*?\\.flv/[a-z0-9]+/[A-Z0-9]+\\.flv)").getMatch(0);
                if (filelink == null) filelink = br.getRegex("(http://s\\d+\\.motherlessmedia\\.com/dev[0-9/]+\\.flv/[a-z0-9]+/[A-Z0-9]+\\.flv)").getMatch(0);
                if (filelink == null) {
                    logger.warning("Failed to find the videolink #1");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                String matches = br.getRegex("s1.addParam\\(\\'flashvars\\',\\'file=([^)]*)").getMatch(0);
                if (matches == null) {
                    logger.info("File should be offline!");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                filelink = rot13(filelink);
                String finallink = matches.replaceAll("\\'(.*?)__file_url(.*?)\\'", filelink).replaceAll("&image=[^&]*", "").replaceAll("(&mute=.+)", "");
                Regex regexName = new Regex(matches, ".*&link=[^&]*/([^&]*)'");
                String finalName = regexName.getMatch(0);
                link.setFinalFileName(finalName + ".flv");
                link.setUrlDownload(finallink);
                link.setProperty("kind", Property.NULL);
            }
        }
        handleFree(link);

    }

    private String rot13(String s) {
        String output = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'm')
                c += 13;
            else if (c >= 'n' && c <= 'z')
                c -= 13;
            else if (c >= 'A' && c <= 'M')
                c += 13;
            else if (c >= 'A' && c <= 'Z') c -= 13;
            output += c;
        }
        return output;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
