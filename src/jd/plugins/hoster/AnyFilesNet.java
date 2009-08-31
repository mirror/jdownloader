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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anyfiles.net" }, urls = { "http://[\\w\\.]*?anyfiles.net/download/[a-z|0-9]+/.+\\.html" }, flags = { 0 })
public class AnyFilesNet extends PluginForHost {

    public AnyFilesNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        String md5crypt = br.getRegex("md5crypt\" value=\"(.*?)\"").getMatch(0);
        String host = br.getRegex("host\" value=\"(.*?)\"").getMatch(0);
        String uid = br.getRegex("uid\" value=\"(.*?)\"").getMatch(0);
        String name = br.getRegex("name\" value=\"(.*?)\"").getMatch(0);
        String realuid = br.getRegex("realuid\" value=\"(.*?)\"").getMatch(0);
        String realname = br.getRegex("realname\" value=\"(.*?)\"").getMatch(0);
        String optiondir = br.getRegex("optiondir\" value=\"(.*?)\"").getMatch(0);
        String pin = br.getRegex("pin\" value=\"(.*?)\"").getMatch(0);
        String ssserver = br.getRegex("ssserver\" value=\"(.*?)\"").getMatch(0);
        String sssize = br.getRegex("realuid\" value=\"(.*?)\"").getMatch(0);
        String free = br.getRegex("free\" type=\"submit\" class=\"button\" value=\"(.*?)\"").getMatch(0);
        
        Form form = new Form();
        form.setMethod(Form.MethodType.POST);
        form.setAction("http://www.anyfiles.net/download3-any.php");
        form.put("md5crypt", md5crypt);
        form.put("host", host);
        form.put("uid", uid);
        form.put("name", name);
        form.put("realuid", realuid);
        form.put("realname", realname);
        form.put("optiondir", optiondir);
        form.put("pin", pin);
        form.put("ssserver", ssserver);
        form.put("sssize", sssize);
        form.put("free", free);

        br.submitForm(form);
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -20);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            System.out.print(br.toString());
            if (br.containsHTML("error: Invalid request")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("Too many simultaneous downloads")) {                
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
            }
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public String getAGBLink() {
        return "http://www.anyfiles.net/page/terms.php";
    }

    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.postPage("http://anyfiles.net/", "de.x=10&de.y=9&vote_cr=de");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("was not found on this server.") || br.containsHTML("The requested file was not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Der Besitzer der Datei begrenzte Anzahl von kostenlosen Downloads pro Tag")) {
            logger.warning(JDL.L("plugins.host.anyfilesnet.premiumonly", "Anyfiles.net: The owner of the file limited the number of free downloads per day, you will have to buy a premium account or wait till you can download the file as free user again!"));
            throw new PluginException(LinkStatus.ERROR_FATAL, "See log");
        }
        String filename = Encoding.htmlDecode(br.getRegex("DATEI:</td><td width=\"500\" valign=\"top\"><h2 style=\"margin-top:1px; margin-bottom:0px;\">(.*?)</h2></td></tr>").getMatch(0));
        String filesize = br.getRegex("SSE:</td><td width=\"500\" valign=\"top\">(.*?( Mb| b| Gb| Kb))  &nbsp;&nbsp;&nbsp").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
