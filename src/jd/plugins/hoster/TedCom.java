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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ted.com" }, urls = { "decrypted://decryptedtedcom\\.com/\\d+" }, flags = { 2 })
public class TedCom extends PluginForHost {

    private static final String CHECKFAST_VIDEOS                   = "CHECKFAST_VIDEOS";
    private static final String CHECKFAST_MP3                      = "CHECKFAST_MP3";
    private static final String CHECKFAST_SUBTITLES                = "CHECKFAST_SUBTITLES";

    private static final String GRAB_VIDEO_BEST                    = "GRAB_VIDEO_BEST";
    private static final String GRAB_VIDEO_LOWRES                  = "GRAB_VIDEO_LOWRES";
    private static final String GRAB_VIDEO_STANDARDRES             = "GRAB_VIDEO_STANDARDRES";
    private static final String GRAB_VIDEO_HIGHRES                 = "GRAB_VIDEO_HIGHRES";

    private static final String GRAB_MP3                           = "GRAB_MP3";
    private static final String GRAB_ALL_AVAILABLE_SUBTITLES       = "GRAB_ALL_AVAILABLE_SUBTITLES";
    private static final String GRAB_SUBTITLE_ALBANIAN             = "GRAB_SUBTITLE_ALBANIAN";
    private static final String GRAB_SUBTITLE_ARABIC               = "GRAB_SUBTITLE_ARABIC";
    private static final String GRAB_SUBTITLE_ARMENIAN             = "GRAB_SUBTITLE_ARMENIAN";
    private static final String GRAB_SUBTITLE_AZERBAIJANI          = "GRAB_SUBTITLE_AZERBAIJANI";
    private static final String GRAB_SUBTITLE_BENGALI              = "GRAB_SUBTITLE_BENGALI";
    private static final String GRAB_SUBTITLE_BULGARIAN            = "GRAB_SUBTITLE_BULGARIAN";
    private static final String GRAB_SUBTITLE_CHINESE_SIMPLIFIED   = "GRAB_SUBTITLE_CHINESE_SIMPLIFIED";
    private static final String GRAB_SUBTITLE_CHINESE_TRADITIONAL  = "GRAB_SUBTITLE_CHINESE_TRADITIONAL";
    private static final String GRAB_SUBTITLE_CROATIAN             = "GRAB_SUBTITLE_CROATIAN";
    private static final String GRAB_SUBTITLE_CZECH                = "GRAB_SUBTITLE_CZECH";
    private static final String GRAB_SUBTITLE_DANISH               = "GRAB_SUBTITLE_DANISH";
    private static final String GRAB_SUBTITLE_DUTCH                = "GRAB_SUBTITLE_DUTCH";
    private static final String GRAB_SUBTITLE_ENGLISH              = "GRAB_SUBTITLE_ENGLISH";
    private static final String GRAB_SUBTITLE_ESTONIAN             = "GRAB_SUBTITLE_ESTONIAN";
    private static final String GRAB_SUBTITLE_FINNISH              = "GRAB_SUBTITLE_FINNISH";
    private static final String GRAB_SUBTITLE_FRENCH               = "GRAB_SUBTITLE_FRENCH";
    private static final String GRAB_SUBTITLE_GEORGIAN             = "GRAB_SUBTITLE_GEORGIAN";
    private static final String GRAB_SUBTITLE_GERMAN               = "GRAB_SUBTITLE_GERMAN";
    private static final String GRAB_SUBTITLE_GREEK                = "GRAB_SUBTITLE_GREEK";
    private static final String GRAB_SUBTITLE_HEBREW               = "GRAB_SUBTITLE_HEBREW";
    private static final String GRAB_SUBTITLE_HUNGARIAN            = "GRAB_SUBTITLE_HUNGARIAN";
    private static final String GRAB_SUBTITLE_INDONESIAN           = "GRAB_SUBTITLE_INDONESIAN";
    private static final String GRAB_SUBTITLE_ITALIAN              = "GRAB_SUBTITLE_ITALIAN";
    private static final String GRAB_SUBTITLE_JAPANESE             = "GRAB_SUBTITLE_JAPANESE";
    private static final String GRAB_SUBTITLE_KOREAN               = "GRAB_SUBTITLE_KOREAN";
    private static final String GRAB_SUBTITLE_KURDISH              = "GRAB_SUBTITLE_KURDISH";
    private static final String GRAB_SUBTITLE_LITHUANIAN           = "GRAB_SUBTITLE_LITHUANIAN";
    private static final String GRAB_SUBTITLE_MACEDONIAN           = "GRAB_SUBTITLE_MACEDONIAN";
    private static final String GRAB_SUBTITLE_MALAY                = "GRAB_SUBTITLE_MALAY";
    private static final String GRAB_SUBTITLE_NORWEGIAN_BOKMAL     = "GRAB_SUBTITLE_NORWEGIAN_BOKMAL";
    private static final String GRAB_SUBTITLE_PERSIAN              = "GRAB_SUBTITLE_PERSIAN";
    private static final String GRAB_SUBTITLE_POLISH               = "GRAB_SUBTITLE_POLISH";
    private static final String GRAB_SUBTITLE_PORTUGUESE           = "GRAB_SUBTITLE_PORTUGUESE";
    private static final String GRAB_SUBTITLE_PORTUGUESE_BRAZILIAN = "GRAB_SUBTITLE_PORTUGUESE_BRAZILIAN";
    private static final String GRAB_SUBTITLE_ROMANIAN             = "GRAB_SUBTITLE_ROMANIAN";
    private static final String GRAB_SUBTITLE_RUSSIAN              = "GRAB_SUBTITLE_RUSSIAN";
    private static final String GRAB_SUBTITLE_SERBIAN              = "GRAB_SUBTITLE_SERBIAN";
    private static final String GRAB_SUBTITLE_SLOVAK               = "GRAB_SUBTITLE_SLOVAK";
    private static final String GRAB_SUBTITLE_SLOVENIAN            = "GRAB_SUBTITLE_SLOVENIAN";
    private static final String GRAB_SUBTITLE_SPANISH              = "GRAB_SUBTITLE_SPANISH";
    private static final String GRAB_SUBTITLE_SWEDISH              = "GRAB_SUBTITLE_SWEDISH";
    private static final String GRAB_SUBTITLE_THAI                 = "GRAB_SUBTITLE_THAI";
    private static final String GRAB_SUBTITLE_TURKISH              = "GRAB_SUBTITLE_TURKISH";
    private static final String GRAB_SUBTITLE_UKRAINIAN            = "GRAB_SUBTITLE_UKRAINIAN";
    private static final String GRAB_SUBTITLE_VIETNAMESE           = "GRAB_SUBTITLE_VIETNAMESE";

    public TedCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("decrypted://", "http://"));
    }

    @Override
    public String getAGBLink() {
        return "http://zdf.de";
    }

    private String DLLINK    = null;
    private int    maxChunks = 0;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        DLLINK = link.getStringProperty("directlink", null);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if ("subtitle".equals(link.getStringProperty("type", null))) maxChunks = 1;
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(link.getStringProperty("finalfilename", null));
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
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
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
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

    @Override
    public String getDescription() {
        return "JDownloader's Ted Plugin helps downloading videoclips and subtitles from ted.com. Ted provides different subtitles and video qualities.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Linkcheck settings: "));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CHECKFAST_VIDEOS, JDL.L("plugins.hoster.tedcom.checkfast.videos", "Fast linkcheck for video links (filesize won't be shown in linkgrabber)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CHECKFAST_MP3, JDL.L("plugins.hoster.tedcom.checkfast.audio", "Fast linkcheck for audio (mp3) links (filesize won't be shown in linkgrabber)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CHECKFAST_SUBTITLES, JDL.L("plugins.hoster.tedcom.checkfast.subtitles", "Fast linkcheck for subtitle links (filesize won't be shown in linkgrabber)?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Video settings: "));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry best = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_VIDEO_BEST, JDL.L("plugins.hoster.tedcom.video.grabbest", "Only grab the best available resolution")).setDefaultValue(false);
        getConfig().addEntry(best);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_VIDEO_LOWRES, JDL.L("plugins.hoster.tedcom.video.grablowres", "Grab low-resolution?")).setDefaultValue(true).setEnabledCondidtion(best, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_VIDEO_STANDARDRES, JDL.L("plugins.hoster.tedcom.video.grabstandardres", "Grab standard-resolution?")).setDefaultValue(true).setEnabledCondidtion(best, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_VIDEO_HIGHRES, JDL.L("plugins.hoster.tedcom.video.grabhighres", "Grab high-resolution?")).setDefaultValue(true).setEnabledCondidtion(best, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_MP3, JDL.L("plugins.hoster.tedcom.audio.grabmp3", "Grab MP3?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Subtitle settings (not finished yet): "));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        // final ConfigEntry hq = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_ALL_AVAILABLE_SUBTITLES,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.graballavailablesubtitles", "Grab all available subtitles?")).setDefaultValue(false);
        // getConfig().addEntry(hq);
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_ALBANIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.albanian", "Grab Albanian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_ARABIC,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.arabic", "Grab Arabic subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_ARMENIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.armenian", "Grab Armenian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_AZERBAIJANI,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.azerbaijani",
        // "Grab Azerbaijani subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_BENGALI,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.bengali", "Grab Bengali subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_BULGARIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.bulgarian",
        // "Grab Bulgarian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_CHINESE_SIMPLIFIED,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.chinesesimplified",
        // "Grab Chinese, Simplified subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_CHINESE_TRADITIONAL,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.chinesetraditional",
        // "Grab Chinese, Traditional subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_CROATIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.croatian", "Grab Croatian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_CZECH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.czech", "Grab Czech subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_DANISH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.danish", "Grab Danish subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_DUTCH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.dutch", "Grab Dutch subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_ENGLISH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.english", "Grab English subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_ESTONIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.estonian", "Grab Estonian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_FINNISH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.finnish", "Grab Finnish subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_FRENCH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.french", "Grab French subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_GEORGIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.georgian", "Grab Georgian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_GERMAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.german", "Grab German subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_GREEK,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.greek", "Grab Greek subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_HEBREW,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.hebrew", "Grab Hebrew subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_HUNGARIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.hungarian",
        // "Grab Hungarian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_INDONESIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.indonesian",
        // "Grab Indonesian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_ITALIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.italian", "Grab Italian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_JAPANESE,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.japanese", "Grab Japanese subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_KOREAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.korean", "Grab Korean subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_KURDISH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.kurdish", "Grab Kurdish subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_FINNISH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.finnish", "Grab Finnish subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_LITHUANIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.lithuanian",
        // "Grab Lithuanian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_MACEDONIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.macedonian",
        // "Grab Macedonian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_MALAY,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.malay", "Grab Malay subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_NORWEGIAN_BOKMAL,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.norwegianbokmal",
        // "Grab Norwegian, Bokmal subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_PERSIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.persian", "Grab Persian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_POLISH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.polish", "Grab Polish subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_PORTUGUESE,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.portugese",
        // "Grab Portugese subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_PORTUGUESE_BRAZILIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.portugesebrazilian",
        // "Grab Portugese, Brazilian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_ROMANIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.romanian", "Grab Romanian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_RUSSIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.russian", "Grab Russian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_SERBIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.serbian", "Grab Serbian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_SLOVAK,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.slovak", "Grab Slovak subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_SLOVENIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.slovenian",
        // "Grab Slovenian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_SPANISH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.spanish", "Grab Spanish subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_SWEDISH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.swedish", "Grab Swedish subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_THAI,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.thai", "Grab Thai subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_TURKISH,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.turkish", "Grab Turkish subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq,
        // false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_UKRAINIAN,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.ukrainian",
        // "Grab Ukrainian subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_SUBTITLE_VIETNAMESE,
        // JDL.L("plugins.hoster.tedcom.grabsubtitles.vietnamese",
        // "Grab Vietnamese subtitle?")).setDefaultValue(false).setEnabledCondidtion(hq, false));
    }

}