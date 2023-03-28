//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;

//Links come from a decrypter
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "file.karelia.ru" }, urls = { "https?://(?:www\\.)?file\\.kareliadecrypted\\.ru/([a-z0-9]+)/\\d+" })
public class FileKareliaRu extends PluginForHost {
    public FileKareliaRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://file.karelia.ru/terms";
    }

    private String folderID = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* Access fixed link */
        br.getPage(link.getDownloadURL().replace("file.kareliadecrypted.ru/", "file.karelia.ru/").replaceAll("(\\d+)$", ""));
        if (jd.plugins.decrypter.FileKareliaRuDecrypter.isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        folderID = new Regex(link.getDownloadURL(), "([a-z0-9]+)/\\d+$").getMatch(0);
        if (link.getBooleanProperty("partlink")) {
            link.setFinalFileName(link.getStringProperty("plainfilename"));
        } else {
            String filesize = br.getRegex("общим размером <strong id=\"totalSize\">([^<>\"]*?)</strong>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("class=\"totalSize\">\\d+ файл <strong>\\(([^<>\"]+)\\)<").getMatch(0);
            }
            if (filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(folderID + ".zip");
            if (filesize != null) {
                setFilesize(link, filesize);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink = null;
        if (link.getBooleanProperty("partlink")) {
            final boolean folderWithOnlySingleFile = link.getBooleanProperty("singlefile", false);
            final String filename_urlencoded = URLEncode.encodeURIComponent(link.getStringProperty("plainfilename"));
            final Regex fInfo = br.getRegex("\"(https?://[a-z0-9\\-]+\\.karelia\\.ru/" + folderID + "/[a-z0-9]+/[a-z0-9]+/" + filename_urlencoded + ")\"");
            dllink = fInfo.getMatch(0);
            if (dllink == null && folderWithOnlySingleFile) {
                /* Fallback for single files --> We do not necessarily have to grab the 'correct' directurl based on our filename! */
                dllink = br.getRegex("data\\-href=\"(https?://[^<>\"]+)").getMatch(0);
            }
        } else {
            dllink = br.getRegex("<span>Скачать архивом:</span>[\t\n\r ]+<a class=\"zip_button sel\" href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            /* Probably a single file */
            if (dllink == null) {
                dllink = br.getRegex("<a title=\"Скачать файл\"[^<>]*?href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public static void setFilesize(final DownloadLink dl, String filesize) {
        filesize = filesize.replace("Гбайта", "GB").replaceAll("Мбайта?", "MB").replace("Кбайта", "kb");
        dl.setDownloadSize(SizeFormatter.getSize(filesize));
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