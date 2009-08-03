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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filegu.ru" }, urls = { "http://[\\w\\.]*?filegu\\.ru/f/[0-9A-Za-z]+/.*" }, flags = { 2 })
public class FileGuRu extends PluginForHost {

    public FileGuRu(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://filegu.ru/tos";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        int retry = 0;
        if (br.getForms().length > 1) {
            if (link.getStringProperty("pass", null) == null) {
                while (br.getForm(1).hasInputFieldByName("pass")) {
                    passCode = Plugin.getUserInput("Password?", link);
                    br.getPage(link.getDownloadURL() + "?pass=" + passCode);
                    if (retry >= 2) throw new PluginException(LinkStatus.ERROR_FATAL, "Wrong Password!");
                    retry++;
                }
            } else {

                passCode = link.getStringProperty("pass", null);
                br.getPage(link.getDownloadURL() + "?pass=" + passCode);
            }
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        sleep(20 * 1001, link);
        String id = br.getRegex("var dl = new Download\\( '(.*?)', '(.*?)', '(.*?)', (.*?) \\);").getMatch(0);
        String slot = br.getRegex("var dl = new Download\\( '(.*?)', '(.*?)', '(.*?)', (.*?) \\);").getMatch(1);
        String hash = br.getRegex("var dl = new Download\\( '(.*?)', '(.*?)', '(.*?)', (.*?) \\);").getMatch(2);
        if (id == null || slot == null || hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.getPage(slot + "/link/" + id + "/" + hash);
        String dllink = slot + "/link/" + id + "/" + hash;
        dl = br.openDownload(link, dllink, false, 1);
        dl.startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.getPage(parameter.getDownloadURL());
        if (br.toString().contains("http://filegu.ru/error:ERR_NO_ID")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<dl>.*?<h1>.*?bordeux.*?>(.*?)</span>").getMatch(0);
        String filesize = br.getRegex("<dl>.*?</label> <strong>(.*?).</strong></dd>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize.replaceAll("М", "M");
        filesize = filesize.replaceAll("к", "k");
        filesize = filesize + "b";
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
