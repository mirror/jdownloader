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
import java.io.InterruptedIOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "terafiles.net" }, urls = { "http://(www\\.)?terafiles\\.net/v-\\d+\\.html" }, flags = { 2 })
public class TeraFilesNet extends PluginForHost {

    public TeraFilesNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.terafiles.net/index.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.terafiles.net/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Fichier indisponible</h1>|Le fichier que vous souhaitez télécharger n'est plus disponible sur nos serveurs\\.)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>Télécharger (.*?) - Envoi, hebergement,").getMatch(0);
        String filesize = br.getRegex("\">Taille du fichier :</td>[\t\n\r ]+<td><strong>(.*?)</strong></td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filesize = filesize.replace("Mo", "Mb");
        link.setDownloadSize(Regex.getSize(filesize));
        link.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        br.setFollowRedirects(false);
        String fileID = new Regex(downloadLink.getDownloadURL(), "terafiles\\.net/v-(\\d+)\\.html").getMatch(0);
        String sessID = br.getRegex("sessdl=([a-z0-9]+)\\'").getMatch(0);
        if (fileID == null || sessID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.terafiles.net/ftp_download.php?id=" + fileID + "&sessdl=" + sessID);
        if (br.containsHTML("Warning</b>:  include(includes-download/captcha_error\\.php)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "ServerError", 15 * 60 * 1000l);
        String dllink = br.getRegex("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"\\d+;URL=(ftp://.*?)\">").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex(">Connexion au serveur FTP en cours\\. Merci de patienter\\.<br /><a href=\"(ftp://.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(ftp://ftpuser.*?@srv\\d+\\.terafiles\\.net/dl/[a-z0-9]+/.*?)\"").getMatch(0);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        try {
            ((Ftp) JDUtilities.getNewPluginForHostInstance("ftp")).download(Encoding.urlDecode(dllink, true), downloadLink);
        } catch (InterruptedIOException e) {
            if (downloadLink.isAborted()) return;
            throw e;
        } catch (IOException e) {
            if (e.toString().contains("maximum number of clients")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            } else
                throw e;
        }
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        // br.getPage("");
        br.postPage("http://www.terafiles.net/index.php?p=connexion", "remember=on&login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://www.terafiles.net/", "session_cookie_id") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        doFree(link);
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