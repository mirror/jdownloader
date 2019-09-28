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
import java.util.Collections;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vidstreaming.io" }, urls = { "https?://(www\\d*\\.)?vidstreaming\\.io/(?:streaming|load)\\.php?.+" })
public class VidStreamingIo extends antiDDoSForDecrypt {
    public VidStreamingIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        ArrayList<String> targets = new ArrayList<String>();
        String[] links = br.getRegex("<li\\s*class\\s*=\\s*\"[^\"]*linkserver[^\"]*\"[^>]+data-status\\s*=\\s*\"[^\"]+\"\\s*data-video\\s*=\\s*\"(?://)?([^\"]+)\">").getColumn(0);
        String[] embeds = br.getRegex("\\{\\s*file\\s*:\\s*[\"']([^\"']+)[\"'][^\\}]*\\}").getColumn(0);
        Collections.addAll(targets, links);
        Collections.addAll(targets, embeds);
        if (targets != null) {
            for (String link : targets) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
            }
        }
        return decryptedLinks;
    }
}