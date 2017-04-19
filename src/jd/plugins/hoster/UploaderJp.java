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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploader.jp" }, urls = { "http://(www\\.)?ux\\.getuploader\\.com/[a-z0-9\\-_]+/download/\\d+" })
public class UploaderJp extends antiDDoSForHost {

    public UploaderJp(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.uploader.jp/rule.html";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        final Form form = br.getFormByInputFieldKeyValue("q", "age_confirmation");
        if (form != null) {
            submitForm(form);
        }
        if (br.containsHTML("404 File Not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex(">オリジナル</span><span class=\"right\">([^<>\"]*?)</span>").getMatch(0);
        String filesize = br.getRegex(">ファイル</span><span class=\"right\">download \\(([^<>\"]*?)\\)</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<th>オリジナル</th>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("<th>容量</th>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        Form form = br.getFormbyProperty("name", "agree");
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (form.hasInputFieldByName("password")) {
            String passCode = downloadLink.getDownloadPassword();
            if (passCode == null) {
                passCode = getUserInput(null, downloadLink);
            }
            form.put("password", Encoding.urlEncode(passCode));
            submitForm(form);
            // check to see if its correct password
            if ((form = br.getFormbyProperty("name", "agree")) != null && form.hasInputFieldByName("password")) {
                if (downloadLink.getDownloadPassword() != null) {
                    downloadLink.setDownloadPassword(null);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
            }
            downloadLink.setDownloadPassword(passCode);
            // standard download
            submitForm(form);
        } else {
            // standard download
            submitForm(form);
        }
        final String md5 = br.getRegex("MD5\\s*\\|?\\s*([a-f0-9]{32})").getMatch(0);
        if (md5 != null) {
            downloadLink.setMD5Hash(md5);
        }
        String dllink = br.getRegex("\"(https?://d(?:ownload|l)\\d+\\.getuploader\\.com/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}