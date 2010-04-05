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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "failai.lt" }, urls = { "http://[\\w\\.]*?failai\\.lt/[a-z0-9]{12}" }, flags = { 0 })
public class FaiLaiLt extends PluginForHost {

    public FaiLaiLt(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(480000l);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://www.failai.lt", "lang", "english");
        br.setCookie("http://www.failai.lt", "lang_by_country", "1");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("You have reached the download-limit")) {
            logger.warning("Waittime detected, please reconnect to make the linkchecker work!");
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.containsHTML("(No such file|No such user exist|File not found)")) {
            logger.warning("file is 99,99% offline, throwing \"file not found\" now...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("You have requested.*?http://.*?/.*?/(.*?)</font>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("fname\" value=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h2>Download File(.*?)</h2>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("Filename.*?nowrap.*?>(.*?)</td").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("File Name.*?nowrap>(.*?)</td").getMatch(0);
                    }
                }
            }
        }
        String filesize = br.getRegex("<small>\\((.*?)\\)</small>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("\\(([0-9]+ bytes)\\)").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("</font>.*?\\((.*?)\\).*?</font>").getMatch(0);
            }
        }
        if (filename == null) {
            logger.warning("The filename equals null, throwing \"file not found\" now...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = filename.replaceAll("(</b>|<b>|\\.html)", "");
        link.setName(filename.trim());
        if (filesize != null) {
            logger.info("Filesize found, filesize = " + filesize);
            link.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        Form f1Form = br.getFormbyProperty("name", "F1");
        if (f1Form != null) {
            br.submitForm(f1Form);
        } else {
            String id = br.getRegex("id\" value=\"(.*?)\"").getMatch(0);
            String fname = br.getRegex("fname\" value=\"(.*?)\"").getMatch(0);
            Form freeform = br.getFormbyProperty("name", "waitForm");
            if (id == null || fname == null || freeform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            freeform.remove("method_premium");
            br.submitForm(freeform);
            // Ticket Time
            String ttt = br.getRegex("id=\"wait\" style=.*?'>(.*?)</span> seconds").getMatch(0);
            if (ttt != null) {
                ttt = ttt.trim();
                logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
                int tt = Integer.parseInt(ttt);
                sleep(tt * 1001, link);
            }
            Form dlform = new Form();
            dlform.setMethod(Form.MethodType.POST);
            dlform.setAction(link.getDownloadURL());
            dlform.put("op", "download1");
            dlform.put("usr_login", "");
            dlform.put("file_wait", "1");
            dlform.put("id", id);
            dlform.put("fname", fname);
            br.submitForm(dlform);
            Form finalform = br.getFormbyProperty("name", "F1");
            if (finalform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // 2nd Ticket Time
            String ttt2 = br.getRegex("countdown\">.*?(\\d+).*?</span>").getMatch(0);
            if (ttt2 != null) {
                ttt2 = ttt2.trim();
                logger.info("Waittime detected, waiting " + ttt2 + " seconds from now on...");
                int tt = Integer.parseInt(ttt2);
                sleep(tt * 1001, link);
            }
            br.submitForm(finalform);
        }
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    // TODO: Make a controller that only allows (a second) the next download to
    // start if the first is already started
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String getAGBLink() {
        return "http://www.failai.lt/tos.html";
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
