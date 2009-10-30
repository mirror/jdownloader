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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "http://[\\w\\.]*?4shared\\.com/dir/\\d+/[\\w]+/?" }, flags = { 0 })
public class FrShrdFldr extends PluginForDecrypt {

    public FrShrdFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String pass = "";
        br.getPage(parameter);
        if (br.containsHTML("enter a password to access")) {
            Form form = br.getFormbyProperty("name", "theForm");
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (int retry = 1; retry <= 5; retry++) {
                pass = getUserInput("Password:", param);
                form.put("userPass2", pass);
                br.submitForm(form);
                if (!br.containsHTML("enter a password to access")) {
                    break;
                } else if (retry == 5) logger.severe("Wrong Password!");
            }
        }
        String script = br.getRegex("src=\"(/account/homeScript.*?)\"").getMatch(0);
        br.cloneBrowser().getPage("http://4shared.com" + script);
        String pages[] = br.getRegex("javascript:pagerShowFiles\\((\\d+)\\);").getColumn(0);
        String burl = br.getRegex("var bUrl = \"(/account/changedir.jsp\\?sId=.*?)\";").getMatch(0);
        String name = br.getRegex("hidden\" name=\"defaultZipName\" value=\"(.*?)\">").getMatch(0);
        String[] links;
        if (!br.containsHTML("dirPwdVerified="))
            links = br.getRegex("<a href=\"(http://[\\w\\.]*?4shared.com/file/.*?)\"").getColumn(0);
        else
            links = br.getRegex("<a href=\"(http://[\\w\\.]*?4shared.com/file/.*?)\\?dirPwdVerified").getColumn(0);
        FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        if (links.length == 0) return null;
        for (String dl : links) {
            DownloadLink dlink;
            dlink = createDownloadlink(dl);
            if (pass.length() != 0) dlink.setProperty("pass", pass);
            decryptedLinks.add(dlink);
        }
        String changedir = br.getRegex("var currentDirId = (\\d+);").getMatch(0);
        for (int i = 0; i < pages.length - 1; i++) {
            String url = "http://4shared.com" + burl + "&ajax=true&changedir=" + changedir + "&firstFileToShow=" + pages[i] + "&sortsMode=NAME&sortsAsc=&random=0.1863370989474954";
            br.getPage(url);
            if (!br.containsHTML("dirPwdVerified="))
                links = br.getRegex("<a href=\"(http://[\\w\\.]*?4shared.com/file/.*?)\"").getColumn(0);
            else
                links = br.getRegex("<a href=\"(http://[\\w\\.]*?4shared.com/file/.*?)\\?dirPwdVerified").getColumn(0);
            if (links.length == 0) return null;
            for (String dl : links) {
                DownloadLink dlink;
                dlink = createDownloadlink(dl);
                if (pass.length() != 0) dlink.setProperty("pass", pass);
                decryptedLinks.add(dlink);
            }
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
