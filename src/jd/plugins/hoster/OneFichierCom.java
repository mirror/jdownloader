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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1fichier.com" }, urls = { "http://[a-z0-9]+\\.(dl4free\\.com|alterupload\\.com|cjoint\\.net|desfichiers\\.com|dfichiers\\.com|megadl\\.fr|mesfichiers\\.org|piecejointe\\.net|pjointe\\.com|tenvoi\\.com|1fichier\\.com)/" }, flags = { 0 })
public class OneFichierCom extends PluginForHost {

    public OneFichierCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.1fichier.com/en/cgu.html";
    }

    public void correctDownloadLink(DownloadLink link) {
        // Note: We cannot replace all domains with "1fichier.com" because the
        // downloadlinks are always bind to a domains
        // Prefer english language
        if (!link.getDownloadURL().contains("/en/")) {
            // /en/index.html
            Regex idhostandName = new Regex(link.getDownloadURL(), "http://(.*?)\\.(.*?)/");
            link.setUrlDownload("http://" + idhostandName.getMatch(0) + "." + idhostandName.getMatch(1) + "/en/index.html");
        }
    }

    private static final String PASSWORDTEXT = "(Accessing this file is protected by password|Please put it on the box bellow)";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(The requested file could not be found|The file may has been deleted by its owner|Le fichier demandé n\\'existe pas\\.|Il a pu être supprimé par son propriétaire\\.)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>Téléchargement du fichier : (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("content=\"Téléchargement du fichier (.*?)\">").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("(>Cliquez ici pour télécharger|>Click here to download) (.*?)</a>").getMatch(1);
                if (filename == null) {
                    filename = br.getRegex("\">(Nom du fichier :|File name :)</th>[\t\r\n ]+<td>(.*?)</td>").getMatch(1);
                    if (filename == null) filename = br.getRegex("<title>Download of (.*?)</title>").getMatch(0);
                }
            }
        }
        String filesize = br.getRegex("<th>(Taille :|File size :)</th>[\t\n\r ]+<td>(.*?)</td>").getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) {
            filesize = filesize.replace("Go", "Gb").replace("Mo", "Mb").replace("Ko", "Kb");
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (br.containsHTML(PASSWORDTEXT)) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.onefichiercom.passwordprotected", "This link is password protected"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String passCode = null;
        // Their limit is just very short so a 30 second waittime for all
        // downloads will remove the limit
        if (br.containsHTML("(You already downloading some files,|Téléchargements en cours,)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 30 * 1000l);
        String dllink = null;
        if (br.containsHTML(PASSWORDTEXT)) {
            logger.info("This link seems to be password protected, continuing...");
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            br.postPage(br.getURL(), "pass=" + passCode);
            if (br.containsHTML(PASSWORDTEXT)) throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.onefichiercom.wrongpassword", "Password wrong!"));
            dllink = br.getRedirectLocation();
        } else {
            dllink = br.getRegex("<br/>\\&nbsp;<br/>\\&nbsp;<br/>\\&nbsp;[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -15);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
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