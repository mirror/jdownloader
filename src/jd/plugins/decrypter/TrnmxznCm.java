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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.TrinimixzoneCom;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision: 31871 $", interfaceVersion = 3, names = { "trinimixzone.com" }, urls = { "http://(www\\.)?trinimixzone\\.com/forum/.*" }, flags = { 0 })
public class TrnmxznCm extends PluginForDecrypt {

    public TrnmxznCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        TrinimixzoneCom.login(br);
        br.getPage(parameter);
        String title = br.getRegex("<title>(.*?)</title>").getMatch(0);
        br.setFollowRedirects(false);
        try {
            String[] posts = br.getRegex("<!-- start: postbit -->(.*?)<!-- end: postbit -->").getColumn(0);

            for (String post : posts) {
                if (post.contains("alerta_thx message")) {
                    // need top say thanks
                    String pid = new Regex(post, "pid(\\d+)").getMatch(0);

                    if (parameter.contains("?pid=")) {
                        br.getPage(parameter + "&action=thank");
                    } else {
                        br.getPage(parameter + "?pid=" + pid + "&action=thank");
                    }

                }
            }

            for (String link : HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getBaseURL())) {
                if (link.contains("http://trinimixzone.com/")) {
                    continue;
                }
                decryptedLinks.add(createDownloadlink(link));
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}