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
import java.util.Random;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "download.su" }, urls = { "http://[\\w\\.]*?download\\.su/(photo/|photo-|file/)[a-z0-9]+" }, flags = { 0 })
public class DownloadSu extends PluginForHost {
    // NOTE: If you delete this plugin (in case the host goes offline) please
    // also delete
    // the other part of it which is in the decrypter
    // DecrypterForRedirectServicesWithoutDirectRedirects
    public DownloadSu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.download.su/page/agreement";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("windows-1251");
        String filename = null;
        String filesize = null;
        int filesize0 = 0;
        br.getPage(link.getDownloadURL());
        if (link.getDownloadURL().contains("/photo")) {
            /* Error handling */
            if (br.containsHTML("(Подождите, сейчас Вы будете перемещены|Спасибо, фотография не найдена)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String finallink = br.getRegex("\"(http://dl\\.download\\.su/full/.*?)\"").getMatch(0);
            String ending = new Regex(finallink, ".*?(\\..{3,4}$)").getMatch(0);
            String filename0 = br.getRegex("<title>(.*?)\\. Описание").getMatch(0);
            if (filename0 == null) {
                filename0 = br.getRegex(">Описание фото:(.*?)</h1>").getMatch(0);
                if (filename0 == null) {
                    filename0 = br.getRegex("thepic\" alt=\"(.*?)\"").getMatch(0);
                }
            }
            if (ending != null && filename0 != null) filename = filename0.trim() + new Random().nextInt(10) + ending;
            URLConnectionAdapter con = br.openGetConnection(finallink);
            filesize0 = con.getContentLength();
        } else {
            if (br.containsHTML("(or was removed|is not existed)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("Скачать файл: </b> <b style=\"color:#dd0000; font-size:18px\">(.*?)</b").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>Random data for testing ->  Скачать файл:(.*?)->").getMatch(0);
            filesize = br.getRegex("<b>Размер:</b> </td><td>(.*?)</td>").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setFinalFileName(filename.trim());
        if (filesize != null) {
            filesize = filesize.replace("Г", "G");
            filesize = filesize.replace("М", "M");
            filesize = filesize.replace("к", "k");
            filesize = filesize.replaceAll("(Б|б)", "");
            filesize = filesize + "b";
            link.setDownloadSize(Regex.getSize(filesize));
        }
        if (filesize0 != 0) link.setDownloadSize(filesize0);
        String md5 = br.getRegex("<b>MD5:</b> </td><td>(.*?)</td>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String finallink = null;
        if (downloadLink.getDownloadURL().contains("/photo")) {
            // The site of the photo needs to be accessed again to get a valid
            // link to download!
            br.getPage(downloadLink.getDownloadURL());
            finallink = br.getRegex("\"(http://dl\\.download\\.su/full/.*?)\"").getMatch(0);
        } else {
            br.postPage(downloadLink.getDownloadURL(), "upload_type=free");
            String timeKey = br.getRegex("time_key\\((.*?)\\)").getMatch(0);
            String filename = downloadLink.getFinalFileName();
            if (timeKey == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.postPage("http://www.download.su/external/ajax.php?JsHttpRequest=0-xml", "code=" + timeKey + "&action=time_key");
            String crappyServerKey = br.getRegex("time\": \"(.*?)\"").getMatch(0);
            if (crappyServerKey == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            finallink = "http://dl.download.su/files/" + timeKey + "/" + crappyServerKey + "/" + filename;
        }
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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