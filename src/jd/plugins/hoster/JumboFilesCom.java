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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jumbofiles.com" }, urls = { "http://[\\w\\.]*?jumbofiles\\.com/[0-9a-z]+{12}" }, flags = { 2 })
public class JumboFilesCom extends PluginForHost {

    public JumboFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String brbefore = "";

    @Override
    public String getAGBLink() {
        return "http://jumbofiles.com/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.jumbofiles.com", "lang", "english");
        br.getPage(link.getDownloadURL());
        doSomething();
        if (brbefore.contains("No such file") || brbefore.contains("No such user exist") || brbefore.contains("File not found") || brbefore.contains(">File Not Found<")) {
            logger.warning("file is 99,99% offline, throwing \"file not found\" now...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = new Regex(brbefore, "Filename:.*?</TD><TD>(.*?)</TD>").getMatch(0);
        String filesize = new Regex(brbefore, "Filesize:.*?</TD><TD>(.*?)</TD>").getMatch(0);
        // They got different pages for stream, normal and pw-protected files so
        // we need special handling
        if (filesize == null) {
            filesize = new Regex(brbefore, "<small>\\((.*?)\\)</small>").getMatch(0);
            if (filesize == null) filesize = new Regex(brbefore, "F<TD><center>.*?<br>(.*?)<br>").getMatch(0);
        }
        if (filename == null) {
            filename = new Regex(brbefore, "down_direct\" value=.*?<input type=\"image\" src=.*?</TD></TR>.*?<TR><TD>(.*?)<small").getMatch(0);
            if (filename == null) filename = new Regex(brbefore, "<TD><center>(.*?)<br>").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = filename.replace("&nbsp;", "");
        filename = filename.trim();
        link.setName(filename);
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        // Form um auf "Datei herunterladen" zu klicken
        Form dlForm = br.getFormbyProperty("name", "F1");
        if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String passCode = null;
        if (brbefore.contains("<b>Password:</b>")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            dlForm.put("password", passCode);
        }
        br.submitForm(dlForm);
        doSomething();
        if (brbefore.contains("Wrong password")) {
            logger.warning("Wrong password!");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        String dllink = new Regex(brbefore, "SRC=\"http://jumbofiles\\.com/images/dd\\.gif\" WIDTH=\"5\" HEIGHT=\"5\"><BR> <form name=\".*?\" action=\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = new Regex(brbefore, "\"(http://www\\d+\\.jumbofiles\\.com:\\d+/d/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        dl.startDownload();
    }

    // Removed fake messages which can kill the plugin
    public void doSomething() throws NumberFormatException, PluginException {
        brbefore = br.toString();
        ArrayList<String> someStuff = new ArrayList<String>();
        ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add("<!(--.*?--)>");
        regexStuff.add("(display: none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        for (String aRegex : regexStuff) {
            String lolz[] = br.getRegex(aRegex).getColumn(0);
            if (lolz != null) {
                for (String dingdang : lolz) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (String fun : someStuff) {
            brbefore = brbefore.replace(fun, "");
        }
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