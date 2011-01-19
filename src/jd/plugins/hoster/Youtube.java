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

import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.TbCm;
import jd.plugins.decrypter.TbCm.DestinationFormat;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youtube.com" }, urls = { "httpJDYoutube://[\\w\\.]*?youtube\\.com/(videoplayback\\?.+|get_video\\?.*?video_id=.+&.+(&fmt=\\d+)?)" }, flags = { 2 })
public class Youtube extends PluginForHost {

    private static final Object lock         = new Object();
    private boolean             prem         = false;
    private static final String IDASFILENAME = "ISASFILENAME";
    private static final String ALLOW_MP3    = "ALLOW_MP3";
    private static final String ALLOW_MP4    = "ALLOW_MP4";
    private static final String ALLOW_WEBM   = "ALLOW_WEBM";
    private static final String ALLOW_FLV    = "ALLOW_FLV";
    private static final String ALLOW_3GP    = "ALLOW_3GP";

    public Youtube(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.youtube.com/login?next=/index");
        setConfigElements();
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), IDASFILENAME, JDL.L("plugins.hoster.youtube.idasfilename", "Use Video-ID as filename?")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_WEBM, JDL.L("plugins.hoster.youtube.checkwebm", "Grab WEBM?")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MP4, JDL.L("plugins.hoster.youtube.checkmp4", "Grab MP4?")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MP3, JDL.L("plugins.hoster.youtube.checkmp3", "Grab MP3?")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_FLV, JDL.L("plugins.hoster.youtube.checkflv", "Grab FLV?")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_3GP, JDL.L("plugins.hoster.youtube.check3gp", "Grab 3GP?")).setDefaultValue(true));

    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("httpJDYoutube", "http"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            this.login(account, this.br);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus(JDL.L("plugins.hoster.youtube.accountok", "Account is OK."));
        ai.setValidUntil(-1);
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://youtube.com/t/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        this.prem = false;
        /* we now have to get fresh links */
        downloadLink.setProperty("valid", false);
        this.requestFileInformation(downloadLink);
        this.br.setDebug(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL(), true, 0);
        if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video")) {
            downloadLink.setProperty("valid", false);
            this.dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (this.dl.startDownload()) {
            this.postprocess(downloadLink);
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        synchronized (Youtube.lock) {
            this.login(account, this.br);
            this.prem = true;
            /* we now have to get fresh links */
            this.requestFileInformation(downloadLink);
            this.br.setDebug(true);
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL(), true, 0);
        }
        if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video")) {
            downloadLink.setProperty("valid", false);
            this.dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (this.dl.startDownload()) {
            this.postprocess(downloadLink);
        }
    }

    public void login(final Account account, Browser br) throws Exception {
        if (br == null) {
            br = this.br;
        }
        br.setDebug(true);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* TODO: google accounts with youtube profile do not work yet */
        br.getPage("http://www.youtube.com/");
        br.getPage("https://www.google.com/accounts/ServiceLogin?uilel=3&service=youtube&passive=true&continue=http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26nomobiletemp%3D1%26hl%3Den_US%26next%3D%252Findex&hl=en_US&ltmpl=sso");
        final Form form = br.getForm(1);
        form.put("continue", Encoding.urlEncode_light("http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26amp%3Bnomobiletemp%3D1%26amp%3Bhl%3Den_US%26amp%3Bnext%3D%252Findex"));
        form.put("Email", Encoding.urlEncode(account.getUser()));
        form.put("Passwd", Encoding.urlEncode(account.getPass()));
        br.setFollowRedirects(false);
        final String cook = br.getCookie("http://www.google.com", "GALX");
        if (cook == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.submitForm(form);
        if (br.getRedirectLocation() == null) {
            final String page = Encoding.htmlDecode(br.toString());
            final String red = new Regex(page, "url='(http://.*?)'").getMatch(0);
            if (red == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
            br.getPage(red);
        }
        br.setFollowRedirects(true);
        br.getPage(br.getRedirectLocation());
        if (br.getCookie("http://www.youtube.com", "LOGIN_INFO") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        br.getPage("http://www.youtube.com/index?hl=en");
    }

    private void postprocess(final DownloadLink downloadLink) {
        if (downloadLink.getProperty("convertto") != null) {
            final DestinationFormat convertto = DestinationFormat.valueOf(downloadLink.getProperty("convertto").toString());
            DestinationFormat InType = DestinationFormat.VIDEOFLV;
            if (convertto.equals(DestinationFormat.VIDEOMP4) || convertto.equals(DestinationFormat.VIDEO3GP)) {
                InType = convertto;
            }
            if (!TbCm.ConvertFile(downloadLink, InType, convertto)) {
                logger.severe("Video-Convert failed!");
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("valid", true)) {
            downloadLink.setFinalFileName(downloadLink.getStringProperty("name", "video.tmp"));
            downloadLink.setDownloadSize((Long) downloadLink.getProperty("size", Long.valueOf(0l)));
            return AvailableStatus.TRUE;
        } else {
            downloadLink.setFinalFileName(downloadLink.getStringProperty("name", "video.tmp"));
            downloadLink.setDownloadSize((Long) downloadLink.getProperty("size", Long.valueOf(0l)));
            final PluginForDecrypt plugin = JDUtilities.getPluginForDecrypt("youtube.com");
            if (plugin == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "cannot decrypt videolink"); }
            if (downloadLink.getStringProperty("fmtNew", null) == null) { throw new PluginException(LinkStatus.ERROR_FATAL, "You have to add link again"); }
            if (downloadLink.getStringProperty("videolink", null) == null) { throw new PluginException(LinkStatus.ERROR_FATAL, "You have to add link again"); }

            final HashMap<Integer, String[]> LinksFound = ((TbCm) plugin).getLinks(downloadLink.getStringProperty("videolink", null), this.prem, this.br);

            if (LinksFound.isEmpty()) {
                if (this.br.containsHTML("<div\\s+id=\"verify-age-actions\">")) { throw new PluginException(LinkStatus.ERROR_FATAL, "The entered account couldn't pass the age verification!"); }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setUrlDownload(LinksFound.get(downloadLink.getIntegerProperty("fmtNew", 0))[0]);
            return AvailableStatus.TRUE;
        }

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink downloadLink) {
        downloadLink.setFinalFileName(downloadLink.getStringProperty("name", "video.tmp"));
        downloadLink.setDownloadSize((Long) downloadLink.getProperty("size", Long.valueOf(0l)));
        downloadLink.setProperty("valid", false);
    }

    @Override
    public void resetPluginGlobals() {
    }
}
