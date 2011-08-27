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

import java.io.File;
import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filearn.com" }, urls = { "http://(www\\.)?filearn\\.com/files/get/[A-Za-z0-9_\\-]+" }, flags = { 0 })
public class FilEarnCom extends PluginForHost {

    public FilEarnCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filearn.com/legal/tos";
    }

    private static final String TOOMANYSIMLUTANDOWNLOADS = ">Only premium users can download more than one file at a time";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(File Link Error<|Your file could not be found\\. Please check the download link\\.<|<title>File: Not Found \\- NoelShare</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<span id=\"name\">[\t\n\r ]+<nobr>(.*?)</nobr>").getMatch(0);
        String filesize = br.getRegex("<span id=\"size\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        // Check if decrypter also gave us a filename
        String videarnName = link.getStringProperty("videarnname");
        if (videarnName != null) {
            String ext = filename.substring(filename.lastIndexOf("."));
            if (ext == null) ext = "";
            filename = videarnName + ext;
        }
        link.setName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        Browser br2 = br.cloneBrowser();
        String dllink = downloadLink.getStringProperty("dllink");
        try {
            if (dllink != null) {
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html")) {
                    downloadLink.setProperty("dllink", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            }
        } catch (Exception e) {
            dllink = null;
        }
        if (dllink == null) {
            if (br.containsHTML(TOOMANYSIMLUTANDOWNLOADS)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
            String jsCrap = br.getRegex("</span></code>[\t\n\r ]+<div>[\t\n\r ]+<script language=\"javascript\">[\t\n\r ]+function [A-Za-z0-9]+\\(iioo\\) \\{(.*?return .*?;)").getMatch(0);
            if (jsCrap == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String action = br.getRegex("\"(http://(www\\.)?filearn\\.com/files/gen/.*?)\"").getMatch(0);
            String actionPart = execJS(jsCrap);
            if (action == null || actionPart == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            Form dlForm = new Form();
            dlForm.setAction(action + "/" + actionPart);
            dlForm.put("pass", "");
            dlForm.put("waited", "1");
            dlForm.setMethod(MethodType.POST);
            rc.setForm(dlForm);
            String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            rc.setId(id);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            // Waittime can be skipped atm.
            // long timeBefore = System.currentTimeMillis();
            String c = getCaptchaCode(cf, downloadLink);
            // int wait = 60;
            // String waittime =
            // br.getRegex("id=\"waittime\">(\\d+)</span>").getMatch(0);
            // if (waittime != null) wait = Integer.parseInt(waittime);
            // int passedTime = (int) ((System.currentTimeMillis() - timeBefore)
            // / 1000) - 1;
            // wait -= passedTime;
            // sleep(wait * 1000, downloadLink);
            rc.setCode(c);
            if (br.containsHTML(TOOMANYSIMLUTANDOWNLOADS)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/|>The Captcha you submited was incorrect)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dllink = br.getRedirectLocation();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // More chunks are possible but i think they cause many servererrors
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(TOOMANYSIMLUTANDOWNLOADS)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
            if (br.containsHTML("(>Download link does not exist|>An Error Was Encountered<)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server problems", 120 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("dllink", dllink);
        dl.startDownload();
    }

    private String execJS(String fun) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        String returnVar = new Regex(fun, "return ([A-Za-z0-9]+);").getMatch(0);
        if (returnVar == null) return null;
        fun = "var iioo = false;" + fun.replace("return " + returnVar + ";", "var lol = " + returnVar + ";");
        try {
            result = engine.eval(fun);
        } catch (final Exception e) {
            JDLogger.exception(e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (result == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return result.toString();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}