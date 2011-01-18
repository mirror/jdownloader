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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {"1gabba.com"}, urls = {"http://[\\w\\.]*?1gabba\\.com/node/[\\d]{4}"}, flags = {0})
public class NGbbCm extends PluginForDecrypt {

    public NGbbCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String linkHMTL = br.getRegex(Pattern.compile("Download links:.*?<div class=\"codeblock\"><pre>(.*?)(</a>)?</pre></div>", Pattern.DOTALL)).getMatch(0);
        String links[] = new Regex(linkHMTL, Pattern.compile("(http://.+)")).getColumn(0);
        progress.setRange(links.length);
        for (String element : links) {
            if (element.contains("\">")) {
                element = new Regex(element, Pattern.compile("(http://.*?)\">")).getMatch(0);
            }
            decryptedLinks.add(createDownloadlink(element));
            progress.increase(1);
        }

        return decryptedLinks;
    }

}
