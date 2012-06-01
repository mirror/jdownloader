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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 8391 $", interfaceVersion = 2, names = { "tekdosya.com" }, urls = { "http://[\\w\\.]*?tekdosya\\.com/(redirect|files)/.*" }, flags = { 0 })
public class TkdsyCm extends PluginForDecrypt {

    public TkdsyCm(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (parameter.toString().contains("http://www.tekdosya.com/redirect/")) {
            br.setFollowRedirects(false);
            br.getPage(parameter.toString());
            DownloadLink dl = createDownloadlink(br.getRedirectLocation());
            decryptedLinks.add(dl);
        } else {
            String url = parameter.toString();
            url = url.replace("/files/", "/links/");
            br.getPage(url);
            br.setFollowRedirects(false);
            String[] links = br.getRegex("<a href=\"(.*?)\" target=\"_blank\">indir</a></div>").getColumn(0);
            if (links.length == 0) return null;
            progress.setRange(links.length);
            for (String dl : links) {
                br.getPage("http://www.tekdosya.com" + dl);
                decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
                progress.increase(1);
            }

        }

        return decryptedLinks;
    }

}
