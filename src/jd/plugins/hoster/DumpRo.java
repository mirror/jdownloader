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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision="$Revision", interfaceVersion=1, names = { "dump.ro"}, urls ={ "http://[\\w\\.]*?dump\\.ro/[0-9A-Za-z/\\-\\.\\?\\=\\&]+"}, flags = {0})
public class DumpRo extends PluginForHost {

    public DumpRo(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://www.dump.ro/termeni-si-conditii";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        Form[] form = br.getForms();
        br.submitForm(form[2]);
        br.setFollowRedirects(false);

        String dlform = new String();
        if (br.getRegex("download_file\\('(.*?)','(.*?)',.*?\\);").matches() == false) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dlform = "download.php?action=download&type=file&id=" + br.getRegex("download_file\\('(.*?)','(.*?)',.*?\\);").getMatch(0) + "&act=" + br.getRegex("download_file\\('(.*?)','(.*?)',.*?\\);").getMatch(1);

        Form forms = new Form();
        forms.setAction(dlform);
        forms.setMethod(MethodType.POST);
        InputField nv2 = new InputField("actiune", "download");
        forms.addInputField(nv2);

        dl = br.openDownload(link, forms, false, 1);
        dl.startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());

        if (br.containsHTML("Link invalid")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Pattern.compile("REGEXP", Pattern.DOTALL);
        String filename = br.getRegex(Pattern.compile("<td width=\"30%\" align=\"left\"><b>Nume fisier:</b></td>.*<td width=\"70%\" align=\"left\">(.*?)</td>", Pattern.DOTALL)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("<td width=\"30%\" align=\"left\"><b>Marime:</b></td>.*<td align=\"left\">(.*?)</td>.*</tr>.*<tr>.*<td width=\"30%\" align=\"left\"><b>Tip:</b></td>", Pattern.DOTALL)).getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
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

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

  
    /* /* public String getVersion() {
        return getVersion("$Revision$");
    } */

}
