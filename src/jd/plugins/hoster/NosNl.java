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
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nos.nl" }, urls = { "http://nosdecrypted\\.nl/\\d+|http://nos\\.nl/(audio/\\d+/|embed/\\?id=a:\\d+)" }, flags = { 2 })
public class NosNl extends PluginForHost {

    public NosNl(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://over.nos.nl/organisatie/regelgeving/";
    }

    /** Settings stuff */
    private static final String FASTLINKCHECK    = "FASTLINKCHECK";
    private static final String ALLOW_LQ         = "ALLOW_LQ";
    private static final String ALLOW_HQ         = "ALLOW_HQ";
    private static String       CUSTOM_DATE      = "CUSTOM_DATE";
    private static String       CUSTOM_FILENAME  = "CUSTOM_FILENAME";

    private static final String TYPE_AUDIO       = "http://nos\\.nl/audio/\\d+/";
    private static final String TYPE_AUDIO_EMBED = "http://nos\\.nl/embed/\\?id=a:\\d+";

    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches(TYPE_AUDIO_EMBED)) {
            link.setUrlDownload("http://nos.nl/audio/" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) + "/");
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException, ParseException {
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (downloadLink.getDownloadURL().matches(TYPE_AUDIO)) {
            br.getPage(downloadLink.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String title = br.getRegex("id=\"article\">[\t\n\r ]+<h1>([^<>]*?)</h1>").getMatch(0);
            final String date = br.getRegex("class=\"meta clearfix\">[\t\n\r ]+<li>[a-z]+ ([^<>\"]*?)</li>").getMatch(0);
            final String id = new Regex(downloadLink.getDownloadURL(), "nos\\.nl/audio/(\\d+)/").getMatch(0);
            if (title == null || date == null || id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("http://nos.nl/playlist/audio/mp3-web01/" + id + ".json");
            DLLINK = br.getRegex("\"videofile\":\"(http:[^<>\"]*?)\"").getMatch(0);
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            DLLINK = DLLINK.replace("\\", "");
            downloadLink.setProperty("directlink", DLLINK);
            downloadLink.setProperty("plain_qualityname", "HQ");
            downloadLink.setProperty("plain_filename", Encoding.htmlDecode(title).trim());
            downloadLink.setProperty("plain_date", TimeFormatter.getMilliSeconds(date, "dd MMMM yyyy, HH:mm", Locale.forLanguageTag("nl")));
            downloadLink.setProperty("plain_linkid", id);
            downloadLink.setProperty("plain_ext", ".mp3");
        } else {
            DLLINK = checkDirectLink(downloadLink, "directlink");
            if (DLLINK == null) {
                if (downloadLink.getStringProperty("mainlink", null) == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                br.getPage(downloadLink.getStringProperty("mainlink", null));
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* Undefined case */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFormattedFilename(downloadLink));
        // In case the link redirects to the finallink
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", DLLINK);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    final static String[][]     REPLACES              = { { "plain_date", "date", "Date when the content was uploaded" }, { "plain_linkid", "linkid", "ID of the video" }, { "plain_ext", "ext", "Extension of the file" }, { "plain_qualityname", "quality", "Quality of the file" }, { "plain_filename", "title", "Title of the media" } };

    private final static String defaultCustomFilename = "*title*_*quality**ext*";
    private final static String defaultCustomDate     = "dd.MM.yyyy";

    public static String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("nos.nl");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (!formattedFilename.contains("*title") && !formattedFilename.contains("*ext*") && !formattedFilename.contains("*linkid*")) {
            formattedFilename = defaultCustomFilename;
        }
        for (final String[] replaceinfo : REPLACES) {
            final String property = replaceinfo[0];
            final String fulltagname = "*" + replaceinfo[1] + "*";
            String tag_data = downloadLink.getStringProperty(property, "-");
            if (fulltagname.equals("*date*")) {
                if (tag_data.equals("-")) {
                    tag_data = "0";
                }
                final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
                SimpleDateFormat formatter = null;

                Date theDate = new Date(Long.parseLong(tag_data));

                if (userDefinedDateFormat != null) {
                    try {
                        formatter = new SimpleDateFormat(userDefinedDateFormat);
                        tag_data = formatter.format(theDate);
                    } catch (Exception e) {
                        // prevent user error killing plugin.
                        tag_data = "-";
                    }
                }
            }
            formattedFilename = formattedFilename.replace(fulltagname, tag_data);
        }
        formattedFilename = encodeUnicode(formattedFilename);
        return formattedFilename;
    }

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public String getDescription() {
        return "JDownloader's NOS Plugin helps downloading Videoclips from nos.nl. NOS provides different video qualities.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, JDL.L("plugins.hoster.NosNl.fastLinkcheck", "Fast linkcheck (filesize won't be shown in linkgrabber)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_LQ, JDL.L("plugins.hoster.NosNl.checkLQ", "Grab LQ?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HQ, JDL.L("plugins.hoster.NosNl.checkHQ", "Grab HQ?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filenames"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.NosNl.customdate", "Define how the date should look.")).setDefaultValue(defaultCustomDate));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*date*_*title**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, JDL.L("plugins.hoster.NosNl.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("Explanation of the available tags:<br>");
        for (final String[] replaceinfo : REPLACES) {
            final String fulltagname = "*" + replaceinfo[1] + "*";
            final String tagdescription = replaceinfo[2];
            sb.append(fulltagname + " = " + tagdescription + "<br>");
        }
        sb.append("</html>");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
