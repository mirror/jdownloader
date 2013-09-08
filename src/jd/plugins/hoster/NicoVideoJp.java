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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nicovideo.jp" }, urls = { "http://(www\\.)?nicovideo\\.jp/watch/(sm|nm)\\d+" }, flags = { 2 })
public class NicoVideoJp extends PluginForHost {

    private static final String MAINPAGE               = "http://www.nicovideo.jp/";

    private static final String ONLYREGISTEREDUSERTEXT = "Only downloadable for registered users";
    private static final String CUSTOM_DATE            = "CUSTOM_DATE";
    private static final String CUSTOM_FILENAME        = "CUSTOM_FILENAME";

    public NicoVideoJp(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://secure.nicovideo.jp/secure/register");
        setConfigElements();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, ParseException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<title>ニコニコ動画　ログインフォーム</title>") || br.getURL().contains("/secure/") || br.getURL().contains("login_form?")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1 itemprop=\"name\">([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        link.setProperty("plainfilename", filename);
        final String date = br.getRegex("property=\"video:release_date\" content=\"(\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}\\+\\d{4})\"").getMatch(0);
        if (date != null) link.setProperty("originaldate", date);
        final String channel = br.getRegex("Uploader: <strong itemprop=\"name\">([^<>\"]*?)</strong>").getMatch(0);
        if (channel != null) link.setProperty("channel", channel);

        filename = getFormattedFilename(link);
        link.setFinalFileName(filename);
        link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.nicovideojp.only4registered", ONLYREGISTEREDUSERTEXT));

        return AvailableStatus.TRUE;
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
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://info.nicovideo.jp/base/rule.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        // More simultan downloads are possible but cause errors!
        return 4;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.nicovideojp.only4registered", ONLYREGISTEREDUSERTEXT));
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        // if
        // (link.getDownloadURL().matches("http://(www\\.)?nicovideo\\.jp/watch/nm\\d+"))
        // throw new PluginException(LinkStatus.ERROR_FATAL,
        // "This linktype isn't supported yet!");
        login(account);
        br.setFollowRedirects(false);
        // Important, without accessing the link we cannot get the downloadurl!
        br.getPage(link.getDownloadURL());
        if (Encoding.htmlDecode(br.toString()).contains("closed=1\\&done=true")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
        String vid = new Regex(link.getDownloadURL(), "nicovideo\\.jp/watch/((sm|nm)\\d+)").getMatch(0);
        br.postPage("http://flapi.nicovideo.jp/api/getflv", "eco=4&as3=1&v=" + vid);
        String dllink = new Regex(Encoding.htmlDecode(br.toString()), "\\&url=(http://.*?)\\&").getMatch(0);
        if (dllink == null) dllink = new Regex(Encoding.htmlDecode(br.toString()), "(http://smile-com\\d+\\.nicovideo\\.jp/smile\\?v=[0-9\\.]+)").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.postPage("https://secure.nicovideo.jp/secure/login?site=niconico", "next_url=&mail=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie(MAINPAGE, "user_session") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    private String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        String videoName = downloadLink.getStringProperty("plainfilename", null);
        final SubConfiguration cfg = SubConfiguration.getConfig("nicovideo.jp");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = defaultCustomFilename;
        if (!formattedFilename.contains("*videoname") || !formattedFilename.contains("*ext*")) formattedFilename = defaultCustomFilename;

        String date = downloadLink.getStringProperty("originaldate", null);
        final String channelName = downloadLink.getStringProperty("channel", null);

        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            date = date.replace("T", ":");
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, "dd.MM.yyyy_HH-mm-ss");
            // 2009-08-30T22:49+0900
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd:HH:mm+ssss");
            Date dateStr = formatter.parse(date);

            formattedDate = formatter.format(dateStr);
            Date theDate = formatter.parse(formattedDate);

            if (userDefinedDateFormat != null) {
                try {
                    formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = "";
                }
            }
            if (formattedDate != null)
                formattedFilename = formattedFilename.replace("*date*", formattedDate);
            else
                formattedFilename = formattedFilename.replace("*date*", "");
        }
        if (formattedFilename.contains("*channelname*")) {
            if (channelName != null)
                formattedFilename = formattedFilename.replace("*channelname*", channelName);
            else
                formattedFilename = formattedFilename.replace("*channelname*", "");
        }
        formattedFilename = formattedFilename.replace("*ext*", ".flv");
        // Insert filename at the end to prevent errors with tags
        formattedFilename = formattedFilename.replace("*videoname*", videoName);

        return formattedFilename;
    }

    @Override
    public String getDescription() {
        return "JDownloader's nicovideo.jp plugin helps downloading videoclips. JDownloader provides settings for the filenames.";
    }

    private final static String defaultCustomFilename = "*videoname**ext*";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.nicovideojp.customdate", "Define how the date should look.")).setDefaultValue("dd.MM.yyyy_HH-mm-ss"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*channelname*_*date*_*videoname**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, JDL.L("plugins.hoster.nicovideojp.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("Explanation of the available tags:\r\n");
        sb.append("*channelname* = name of the channel/uploader\r\n");
        sb.append("*date* = date when the video was posted - appears in the user-defined format above\r\n");
        sb.append("*videoname* = name of the video without extension\r\n");
        sb.append("*ext* = the extension of the file, in this case usually '.flv'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}