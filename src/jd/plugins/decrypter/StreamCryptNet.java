//    jDownloader - Downloadmanager
//    Copyright (C) 2020  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import java.util.ArrayList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "streamcrypt.net" }, urls = { "https?://[\\w.]*?streamcrypt\\.net/(?:hoster\\.[\\w.]+?\\.php\\?id=|[^/]+/)\\p{Alnum}++(?:-\\d+x\\d+\\.html)?" })
public class StreamCryptNet extends antiDDoSForDecrypt {
    public StreamCryptNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.toString());
        DownloadLink downloadLink = createDownloadlink(br.getRedirectLocation());
        downloadLink.setProperty("redirect_link", param.toString());
        decryptedLinks.add(downloadLink);
        distribute(downloadLink);
        return decryptedLinks;
    }
}
