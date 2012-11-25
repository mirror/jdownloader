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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nzbload.com" }, urls = { "http://(www\\.)?nzbload\\.com/en/download/[a-z0-9]+(/\\d+)?" }, flags = { 0 })
public class NzbLoadComFolder extends PluginForDecrypt {

    public NzbLoadComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // Pass single links over to the hosterplugin
        if (parameter.matches("http://(www\\.)?nzbload\\.com/en/download/[a-z0-9]+/\\d+")) {
            decryptedLinks.add(createDownloadlink(parameter.replace("nzbload.com/", "nzbloaddecrypted.com/")));
            return decryptedLinks;
        }
        final String fid = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
        br.getHeaders().put("Accept", "text/plain, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("http://www.nzbload.com/data/download.json?t=" + System.currentTimeMillis() + "&sub=" + fid);
        // String[][] fileInfo = br.getRegex("(\\d+)\":\\{\"filename\":\"([^<>\"]*?)\",\"date\":\\d+,\"size\":\"(\\d+)\"").getMatches();
        String[][] fileInfo = br.getRegex("(\\d+)\":\\{(.*?)\\}").getMatches();
        if (fileInfo == null || fileInfo.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String[] singleLinkInfo : fileInfo) {
            final DownloadLink dl = createDownloadlink("http://nzbloaddecrypted.com/en/download/" + fid + "/" + singleLinkInfo[0]);
            dl.setName(getJson("filename", singleLinkInfo[1]));
            dl.setDownloadSize(SizeFormatter.getSize(getJson("size", singleLinkInfo[1])));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    private String getJson(final String parameter, final String source) {
        return new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
    }
}
