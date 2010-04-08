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

import java.util.ArrayList;

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

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("youload.to/", "duckload.com/"));
    }

    public String aBrowser = "";

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        boolean stream = false;
        requestFileInformation(link);
        br.setDebug(true);
        haveFun();
        // waittime check
        if (aBrowser.contains("Your downloadticket was booked")) {
            sleep(10 * 1000l, link);
        }
        Form form = br.getForm(0);
        String capurl = br.getRegex("src=\"/design/Captcha\\d?(.*?\\.php\\?.*?=.*?)\"").getMatch(0);
        if (capurl == null) capurl = br.getRegex("src='/design/Captcha\\d?(.*?\\.php\\?.*?=.*?)'").getMatch(0);
        if (capurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        capurl = "/design/Captcha2" + capurl;
        String code = getCaptchaCode(capurl, link);
        // Check this part first if the plugin is defect!
        String applcode = null;
        applcode = br.getRegex("src=\"/design/Captcha.*?php\\?.*?\".*?<input name=\"(.*?)\"").getMatch(0);
        String[] matches = br.getRegex("<input( id=\".*?\" |.*?)name=\"(.*?)\"").getColumn(1);
        if (matches == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        ArrayList<String> matchList = new ArrayList<String>();
        for (String match : matches) {
            if (!matchList.contains(match)) matchList.add(match);
        }
        if (applcode == null) {
            logger.warning("regex for applcode is defect!");
            if (form.containsHTML("a_code")) {
                applcode = "a_code";
            } else if (form.containsHTML("b_code")) {
                applcode = "b_code";
            } else if (form.containsHTML("appl_code")) {
                applcode = "appl_code";
            } else if (applcode == null) {
                logger.warning("No standard captcha inputname found, using humpf!");
                applcode = "humpf";
            }
        }
        if (aBrowser.contains("Your downloadticket was booked")) {
            String fileid = new Regex(link.getDownloadURL(), "duckload\\.com/download/(\\d+)/").getMatch(0);
            String filenamefromlink = new Regex(link.getDownloadURL(), "duckload\\.com/download/.*?/(.+)").getMatch(0);
            String postlink = "http://duckload.com/index.php?Modul=download&id=" + fileid + "&name=" + filenamefromlink + "&Ajax=true";
            form = new Form();
            form.setAction(postlink);
            form.setMethod(MethodType.POST);
        } else {
            form = new Form();
            form.setAction(br.getURL());
            form.setMethod(MethodType.POST);
            form.put("server", "1");
            stream = true;
        }
        // form.put(applcode, code);
        for (String omg : matchList) {
            form.put(omg, code);
        }
        br.submitForm(form);
        String url = null;
        if (!stream) {
            url = br.getRedirectLocation();
            br.setDebug(true);
            if (url != null && url.contains("error=wrongCaptcha")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else {
            url = br.getRegex("type=\"video/divx\" src=\"(.*?)\"").getMatch(0);
            if (url == null) url = br.getRegex("\"(http://dl[0-9]+\\.duckload\\.com:[0-9]+/Get/.*?/.*?)\"").getMatch(0);
            String filename = br.getRegex("Original Filename:</strong></td><td width=.*?>(.*?)</td>").getMatch(0);
            if (filename != null) link.setFinalFileName(filename);
            if (url == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, -8);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.toString().trim().equals("no")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Servererror", 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
        if (br.containsHTML("(File was not found!|Die angeforderte Datei konnte nicht gefunden werden)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Stream Protection")) {
            /* streaming file */
            parameter.setName("VideoStream.avi");
            String filesize = br.getRegex(">Server \\(#\\d+\\) <i.*?\">\\[(.*?)\\]").getMatch(0);
            if (filesize != null)
                parameter.setDownloadSize(Regex.getSize(filesize.trim()));
            else
                logger.warning("Filesize regex is broken!");
            return AvailableStatus.TRUE;
        }
        String filename = br.getRegex("You want to download the file \"(.*?)\".*?!<br>").getMatch(0);
        String filesize = br.getRegex("You want to download the file \".*?\" \\((.*?)\\) !<br>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    public void haveFun() throws Exception {
        aBrowser = br.toString();
        String replaces[] = br.getRegex("<!--(.*?)-->").getColumn(0);
        if (replaces != null) {
            for (String dingdang : replaces) {
                dingdang = dingdang.trim();
                aBrowser = aBrowser.replace(dingdang, "");
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    /*
     * /* public String getVersion() { return getVersion("$Revision$");
     * }
     */
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}
