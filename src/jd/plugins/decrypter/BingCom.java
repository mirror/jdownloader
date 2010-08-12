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
import jd.plugins.PluginForDecrypt;
import jd.utils.JDHexUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bing.com" }, urls = { "http://[\\w\\.]*?bing\\.com/videos/watch/video/.*?/[a-z0-9]+" }, flags = { 0 })
public class BingCom extends PluginForDecrypt {

    public BingCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<title>(.*?) - Bing Videos</title>").getMatch(0);
        boolean setFilename = false;
        if (filename != null) {
            setFilename = true;
            filename = filename.replace(":", "-");
        }
        String[] regexes = { "formatCode: 1003, url: \\'(http.*?\\.flv)'\\}", "formatCode: 1002, url: '(http.*?\\.wmv)'\\}" };
        for (String regex : regexes) {
            String finallink = br.getRegex(regex).getMatch(0);
            if (finallink != null) {
                finallink = JDHexUtils.decodeJavascriptHex(finallink);
                DownloadLink fnllink = createDownloadlink(finallink);
                if (setFilename) {
                    if (finallink.endsWith(".flv"))
                        fnllink.setFinalFileName(filename + ".flv");
                    else
                        fnllink.setFinalFileName(filename + ".wmv");
                }
                decryptedLinks.add(fnllink);
            }
        }
        return decryptedLinks;
    }
}
