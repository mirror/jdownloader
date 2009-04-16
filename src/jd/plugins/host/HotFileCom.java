package jd.plugins.host;

//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class HotFileCom extends PluginForHost {

    public HotFileCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://hotfile.com/terms-of-service.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        String filename = br.getRegex("Downloading(.*?)\\(.*?\\)</h2>").getMatch(0);
        String filesize = br.getRegex("Downloading.*?\\((.*?)\\)</h2>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.trim()));
        return true;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        getFileInformation(link);
        if (br.containsHTML("You are currently downloading")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Only 1 Download as FreeUser!", 5 * 60 * 1000l);
        if (br.containsHTML("JavaScript>starthtimer")) {
            String waittime = br.getRegex("starthtimer\\(\\).*?timerend=.*?\\+(\\d+);").getMatch(0);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waittime.trim()));
        }
        this.sleep(60 * 1000l, link);
        Form form = br.getForm(1);
        br.submitForm(form);
        String dl_url = br.getRegex("Downloading.*?<a href=\"(.*?/get/.*?)\">").getMatch(0);
        br.setFollowRedirects(true);
        dl = br.openDownload(link, dl_url, true, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
