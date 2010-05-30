//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "romhustler.net" }, urls = { "http://[\\w\\.]*?romhustler\\.net/download/.*?/\\d+" }, flags = { 0 })
public class RomHustlerNet extends PluginForHost {

    private String downloadUrl;

    public RomHustlerNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://romhustler.net/disclaimer.php";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            this.setBrowserExclusive();
            br.getPage(downloadLink.getDownloadURL());
            downloadUrl = decodeurl(br.getRegex(Pattern.compile("link_enc=new Array\\((.*?)\\);", Pattern.CASE_INSENSITIVE)).getMatch(0));
            if (downloadUrl == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String name = Encoding.htmlDecode(downloadUrl.replaceAll("^.*/", ""));
            downloadLink.setName(name);
            return AvailableStatus.TRUE;
        } catch (Exception e) {
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, -2);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    private static String decodeurl(String page) {
        if (page == null) return null;
        StringBuffer sb = new StringBuffer();
        String pattern = "('.'),?";
        Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(page);
        while (r.find()) {
            if (r.group(1).length() > 0) {
                String content = r.group(1).replaceAll("'|,", "");
                r.appendReplacement(sb, content);
            }
        }
        r.appendTail(sb);
        return sb.toString();
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
