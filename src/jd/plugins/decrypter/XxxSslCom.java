//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision: 36935 $", interfaceVersion = 3, names = { "xxxssl.com" }, urls = { "https?://(?:\\w+\\.)?xxxssl\\.com/embed_cdn\\.php\\?video=[^<>]+" })
public class XxxSslCom extends PornEmbedParser {

    public XxxSslCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // they have http player as sourceURL and hls sourceURL_HLS
        final String source = br.getRegex("var sourceURL\\s*=\\s*\"(.*?)\"").getMatch(0);
        final String sourceHLS = br.getRegex("var sourceURL_HLS\\s*=\\s*\"(.*?)\"").getMatch(0);
        if (source != null) {
            decryptedLinks.add(createDownloadlink("directhttp://" + source));
        } else if (sourceHLS != null) {
            decryptedLinks.add(createDownloadlink(sourceHLS));
        }
        return decryptedLinks;
    }

}
