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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "scribd.com" }, urls = { "http://(www\\.)?((de|ru|es)\\.)?scribd\\.com/doc/\\d+" }, flags = { 2 })
public class ScribdCom extends PluginForHost {

    private final String   formats     = "formats";

    /** The list of server values displayed to the user */
    private final String[] allFormats  = new String[] { "PDF", "TXT", "DOCX" };

    private final String   NODOWNLOAD  = JDL.L("plugins.hoster.ScribdCom.NoDownloadAvailable", "Download is disabled for this file!");
    private final String   PREMIUMONLY = JDL.L("plugins.hoster.ScribdCom.premonly", "Download requires a scribd.com account!");

    public ScribdCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.scribd.com");
        setConfigElements();
    }

    public void correctDownloadLink(DownloadLink link) {
        final String linkPart = new Regex(link.getDownloadURL(), "(scribd\\.com/doc/\\d+)").getMatch(0);
        link.setUrlDownload("http://www." + linkPart);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        for (int i = 0; i <= 3; i++) {
            String newurl = br.getRedirectLocation();
            if (newurl != null) {
                if (newurl.contains("/removal/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setUrlDownload(newurl);
                br.getPage(downloadLink.getDownloadURL());
            } else {
                break;
            }
        }
        String filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta property=\"media:title\" content=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<Attribute name=\"title\">(.*?)</Attribute>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<h1 class=\"title\" id=\"\">(.*?)</h1>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
                    }
                }
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        downloadLink.setName(filename);
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
        ai.setStatus("Registered (Free) User");
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://support.scribd.com/forums/33939/entries/25459";
    }

    private String getConfiguredServer() {
        switch (getPluginConfig().getIntegerProperty(formats, -1)) {
        case 0:
            logger.fine("PDF format is configured");
            return "pdf";
        case 1:
            logger.fine("TXT format is configured");
            return "txt";
        case 2:
            logger.fine("DOCX format is configured");
            return "docx";
        default:
            logger.fine("No format is configured, returning PDF...");
            return "pdf";
        }
    }

    private String getConfiguredReplacedServer(final String oldText) {
        String newText = null;
        if (oldText.equals("pdf")) {
            newText = "\"pdf_download\":1";
        } else if (oldText.equals("txt")) {
            newText = "\"text_download\":1";
        } else if (oldText.equals("docx")) {
            newText = "\"word_download\":1";
        }
        return newText;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        sleep(10000, downloadLink, PREMIUMONLY);
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, NODOWNLOAD);
    }

    public void handlePremium(final DownloadLink parameter, final Account account) throws Exception {
        requestFileInformation(parameter);
        if (br.containsHTML("class=\"download_disabled_button\"")) throw new PluginException(LinkStatus.ERROR_FATAL, NODOWNLOAD);
        login(account);
        final String[] downloadInfo = getDllink(parameter);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, downloadInfo[0], true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        parameter.setFinalFileName(parameter.getName() + "." + downloadInfo[1]);
        dl.startDownload();
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.setFollowRedirects(true);
        br.getPage("https://www.scribd.com");

        final String thisToken = br.getRegex("name=\"authenticity_token\" type=\"hidden\" value=\"([a-z0-9]+)\"").getMatch(0);
        if (thisToken == null) {
            logger.warning("Login broken!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        br.postPage("https://www.scribd.com/login", "authenticity_token=" + thisToken + "&login_params%5Bnext_url%5D=&login_params%5Bcontext%5D=join2&form_name=login_lb_form_login_lb&login_or_email=" + Encoding.urlEncode(account.getUser()) + "&login_password=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("Invalid username or password") || !br.containsHTML("\"login\":true")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    private String[] getDllink(final DownloadLink parameter) throws PluginException, IOException {
        String[] dlinfo = new String[2];
        br.setFollowRedirects(false);
        dlinfo[1] = getConfiguredServer();
        final String fileId = new Regex(parameter.getDownloadURL(), "scribd\\.com/doc/(\\d+)/").getMatch(0);
        if (fileId == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser xmlbrowser = br.cloneBrowser();
        xmlbrowser.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        xmlbrowser.getHeaders().put("Accept", "*/*");
        xmlbrowser.postPage("http://www.scribd.com/document_downloads/request_document_for_download", "id=" + fileId);
        // xmlbrowser.postPage("http://www.scribd.com/document_downloads/register_download_attempt",
        // "next_screen=download_lightbox&source=read&doc_id=" + fileId);
        final String correctedXML = xmlbrowser.toString().replace("\\", "");
        // Check if the selected format is available
        if (correctedXML.contains("premium: true")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.scribdcom.errors.nofreedownloadlink", "Download is only available for premium users!"));
        dlinfo[0] = "http://www.scribd.com/document_downloads/" + fileId + "?secret_password=&extension=" + dlinfo[1];
        br.getPage(dlinfo[0]);
        if (br.containsHTML("Sorry, downloading this document in the requested format has been disallowed")) throw new PluginException(LinkStatus.ERROR_FATAL, dlinfo[1] + " format is not available for this file!");
        dlinfo[0] = br.getRedirectLocation();
        if (dlinfo[0] == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return dlinfo;
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), formats, allFormats, JDL.L("plugins.host.ScribdCom.formats", "Download files in this format:")).setDefaultValue(0));
    }
}