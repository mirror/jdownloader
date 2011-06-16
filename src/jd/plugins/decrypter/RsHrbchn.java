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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hoerbuch.in" }, urls = { "http://[\\w\\.]*?hoerbuch\\.in/protection/folder_\\d+\\.html" }, flags = { 0 })
public class RsHrbchn extends PluginForDecrypt {
    private final String ua = RandomUserAgent.generate();

    public RsHrbchn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> passwords = new ArrayList<String>();
        passwords.add("www.hoerbuch.in");
        br.getHeaders().put("User-Agent", ua);
        String parameter = param.toString();
        br.getPage("http://hoerbuch.in/wp/");
        br.getPage(parameter);
        Form form = br.getForm(1);
        if (form == null) return null;
        br.submitForm(form);
        String links[] = br.getRegex("on.png.*?href=\"(http.*?)\"").getColumn(0);
        for (String link : links) {
            if (link.contains("in/protection")) {
                Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(false);
                brc.getPage(link);
                if (brc.getRedirectLocation() != null) {
                    decryptedLinks.add(this.createDownloadlink(brc.getRedirectLocation()));
                }
            } else {
                decryptedLinks.add(this.createDownloadlink(link));
            }
        }
        return decryptedLinks;
    }

}
