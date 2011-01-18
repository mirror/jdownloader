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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filer.cx" }, urls = { "http://[\\w\\.]*?filer.cx/file/\\d+/[0-9a-zA-Z-.]+" }, flags = { 0 })
public class FilerCx extends PluginForHost {

    public FilerCx(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://www.filer.cx/rules.php";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        Form form = br.getForm(0);
        String captchaUrl = "http://www.filer.cx/captcha.php";
        String captchaCode = getCaptchaCode(captchaUrl, link);
        form.put("captchacode", captchaCode);
        br.submitForm(form);
        if (br.containsHTML("Captcha number error or expired")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        String dllink = null;
        dllink = br.getRegex("onclick=\"highlight\\('downloadurl'\\);\" ondblclick=\"ClipBoard\\('downloadurl'\\);\">(.*?)</textarea>").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        String filename = br.getRegex(Pattern.compile("<b>File name:</b></td>.*<td align=left width=150px>(.*?)</td>", Pattern.DOTALL)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("<td align=left><b>File size:</b></td>.*<td align=left>(.*?)</td>.*<td align=left><b>Downloads:</b>", Pattern.DOTALL)).getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}
