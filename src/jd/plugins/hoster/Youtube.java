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
import jd.gui.swing.components.ConvertDialog.ConversionMode;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.decrypter.TbCm;
import jd.utils.JDMediaConvert;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youtube.com" }, urls = { "INSIDEJDhttp://[\\w\\.]*?youtube\\.com/(videoplayback\\?.+|get_video\\?video_id=.+&t=.+(&fmt=\\d+)?)" }, flags = { 2 })
public class Youtube extends PluginForHost {

    private static final Object lock = new Object();
    private boolean prem = false;

    public Youtube(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.youtube.com/login?next=/index");
    }

    @Override
    public String getAGBLink() {
        return "http://youtube.com/t/terms";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("INSIDEJDhttp", "http"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("valid", true)) {
            downloadLink.setFinalFileName(downloadLink.getStringProperty("name", "video.tmp"));
            downloadLink.setDownloadSize((Long) downloadLink.getProperty("size", Long.valueOf(0l)));
            return AvailableStatus.TRUE;
        } else {
            downloadLink.setFinalFileName(downloadLink.getStringProperty("name", "video.tmp"));
            downloadLink.setDownloadSize((Long) downloadLink.getProperty("size", Long.valueOf(0l)));
            PluginForDecrypt plugin = JDUtilities.getPluginForDecrypt("youtube.com");
            if (plugin == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "cannot decrypt videolink");
            if (downloadLink.getStringProperty("fmtNew", null) == null) throw new PluginException(LinkStatus.ERROR_FATAL, "You have to add link again");
            if (downloadLink.getStringProperty("videolink", null) == null) throw new PluginException(LinkStatus.ERROR_FATAL, "You have to add link again");
            HashMap<Integer, String> links = ((TbCm) plugin).getLinks(downloadLink.getStringProperty("videolink", null), prem, this.br);
            boolean oldLayout = false;
            if (links != null) oldLayout = true;
            if (links == null) links = ((TbCm) plugin).getLinksNew(downloadLink.getStringProperty("videolink", null), prem, this.br);
            if (links == null || links.size() == 0) {
                if (br.containsHTML("verify_age")) throw new PluginException(LinkStatus.ERROR_FATAL, "The entered account couldn't pass the age verification!");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (oldLayout) {
                downloadLink.setUrlDownload(links.get(0) + "&fmt=" + downloadLink.getIntegerProperty("fmtNew", 0));
            } else {
                downloadLink.setUrlDownload(links.get(downloadLink.getIntegerProperty("fmtNew", 0)));
            }
            return AvailableStatus.TRUE;
        }

    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        prem = false;
        /* we now have to get fresh links */
        downloadLink.setProperty("valid", false);
        requestFileInformation(downloadLink);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), false, 1);
        if (!dl.getConnection().isContentDisposition() && !dl.getConnection().getContentType().startsWith("video")) {
            downloadLink.setProperty("valid", false);
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (dl.startDownload()) {
            postprocess(downloadLink);
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        synchronized (lock) {
            login(account, br);
            prem = true;
            /* we now have to get fresh links */
            requestFileInformation(downloadLink);
            br.setDebug(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), false, 1);
        }
        if (!dl.getConnection().isContentDisposition() && !dl.getConnection().getContentType().startsWith("video")) {
            downloadLink.setProperty("valid", false);
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (dl.startDownload()) {
            postprocess(downloadLink);
        }
    }

    private void postprocess(DownloadLink downloadLink) {
        if (downloadLink.getProperty("convertto") != null) {
            ConversionMode convertto = ConversionMode.valueOf(downloadLink.getProperty("convertto").toString());
            ConversionMode InType = ConversionMode.VIDEOFLV;
            if (convertto.equals(ConversionMode.VIDEOMP4) || convertto.equals(ConversionMode.VIDEO3GP)) {
                InType = convertto;
            }
            if (!JDMediaConvert.ConvertFile(downloadLink, InType, convertto)) {
                logger.severe("Video-Convert failed!");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    public void login(Account account, Browser br) throws Exception {
        if (br == null) br = this.br;
        br.setDebug(true);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* TODO: google accounts with youtube profile do not work yet */
        br.getPage("http://www.youtube.com/");
        br.getPage("https://www.google.com/accounts/ServiceLogin?uilel=3&service=youtube&passive=true&continue=http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26nomobiletemp%3D1%26hl%3Den_US%26next%3D%252Findex&hl=en_US&ltmpl=sso");
        Form form = br.getForm(1);
        form.put("continue", Encoding.urlEncode_light("http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26amp%3Bnomobiletemp%3D1%26amp%3Bhl%3Den_US%26amp%3Bnext%3D%252Findex"));
        form.put("Email", Encoding.urlEncode(account.getUser()));
        form.put("Passwd", Encoding.urlEncode(account.getPass()));
        br.setFollowRedirects(false);
        String cook = br.getCookie("http://www.google.com", "GALX");
        if (cook == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(form);
        if (br.getRedirectLocation() == null) {
            String page = Encoding.htmlDecode(br.toString());
            String red = new Regex(page, "url='(http://.*?)'").getMatch(0);
            if (red == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            br.getPage(red);
        }
        br.setFollowRedirects(true);
        br.getPage(br.getRedirectLocation());
        if (br.getCookie("http://www.youtube.com", "LOGIN_INFO") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://www.youtube.com/index?hl=en");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, br);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus(JDL.L("plugins.hoster.youtube.accountok", "Account is OK."));
        ai.setValidUntil(-1);
        account.setValid(true);
        return ai;
    }

    @Override
    public void resetDownloadlink(DownloadLink downloadLink) {
        downloadLink.setFinalFileName(downloadLink.getStringProperty("name", "video.tmp"));
        downloadLink.setDownloadSize((Long) downloadLink.getProperty("size", Long.valueOf(0l)));
        downloadLink.setProperty("valid", false);
    }
}
