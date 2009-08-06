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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "duckload.com" }, urls = { "http://[\\w\\.]*?(duckload\\.com|youload\\.to)/(download/\\d+/.+|divx/[a-zA-Z0-9]+\\.html|[a-zA-Z0-9]+\\.html)" }, flags = { 0 })
public class DuckLoad extends PluginForHost {

    public DuckLoad(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://duckload.com/impressum.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        boolean stream = false;
        requestFileInformation(link);
        sleep(10 * 1000l, link);
        Form form = br.getForm(0);
        String capurl = "/design/Captcha"+br.getRegex("src=\"/design/Captcha\\d?(.*?\\.php\\?.*?key=.*?)\"").getMatch(0);
        if (capurl == null) capurl = "/design/Captcha"+br.getRegex("src='/design/Captcha\\d?(.*?\\.php.*?key=.*?)'").getMatch(0);
        String code = getCaptchaCode(capurl, link);
        if (form.containsHTML("appl_code")) {
            form = new Form();
            form.setAction(br.getForm(0).getAction());
            form.setMethod(MethodType.POST);
            form.put("server", "1");
            form.put("appl_code", code);
            stream = true;
        } else {
            form.put("cap", code);
            form.put("_____download.x", ""+((int)(Math.random()*168)));
            form.put("_____download.y", ""+((int)(Math.random()*44)));
        }
        br.submitForm(form);
        String url = null;
        if (!stream) {
            url = br.getRedirectLocation();
            br.setDebug(true);
            if (url != null && url.contains("error=wrongCaptcha")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else {
            url = br.getRegex("src\" value=\"(http://.*?)\"").getMatch(0);
            String filename = br.getRegex("Original Filename:</strong></td><td width=.*?>(.*?)</td>").getMatch(0);
            if (filename != null) link.setFinalFileName(filename);
            if (url == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br,link, url, true, 0);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        if (parameter.getDownloadURL().contains("duckload.com")) {
            br.getPage("http://duckload.com/english.html");
        } else {
            br.getPage("http://youload.to/english.html");
        }
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("stream_protection_h\">Bitte Server")) {
            /* streaming file */
            parameter.setName("VideoStream.avi");
            String filesize = br.getRegex("s_1\">Server.*?\\[(.*?)\\]").getMatch(0);
            if (filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            parameter.setDownloadSize(Regex.getSize(filesize.trim()));
            return AvailableStatus.TRUE;
        }
        if (br.containsHTML("File was not found!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("You want to download the file \"(.*?)\".*?!<br>").getMatch(0);
        String filesize = br.getRegex("You want to download the file \".*?\" \\((.*?)\\) !<br>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    /*
     * /* public String getVersion() { return getVersion("$Revision$"); }
     */
    public int getMaxSimultanFreeDownloadNum() {
        return getMaxSimultanDownloadNum();
    }

}
