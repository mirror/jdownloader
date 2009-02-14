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

package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class ShareProtect extends PluginForDecrypt {

    public ShareProtect(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        String[] matches = br.getRegex("unescape\\(\\'(.*?)'\\)").getColumn(0);
        StringBuilder htmlc = new StringBuilder();
        for (String element : matches) {
            htmlc.append(Encoding.htmlDecode(element) + "\n");
        }

        String[] links = new Regex(htmlc, "<input type=\"button\" value=\"Free\" onClick=.*? window\\.open\\(\\'\\./(.*?)\\'").getColumn(0);
        progress.setRange(links.length);
        htmlc = new StringBuilder();
        for (String element : links) {

            br.getPage("http://" + br.getHost() + "/" + element);
            htmlc.append(Encoding.htmlDecode(br.getRegex("unescape\\(\\'(.*?)'\\)").getMatch(0)) + "\n");
            progress.increase(1);
        }
        br.getRequest().setHtmlCode(htmlc.toString());
        Form[] forms = br.getForms();
        for (Form element : forms) {
            decryptedLinks.add(createDownloadlink(element.getAction()));
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}