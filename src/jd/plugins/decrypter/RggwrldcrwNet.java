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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.ReggaeWorldCrewNet;

@DecrypterPlugin(revision = "$Revision: 31871 $", interfaceVersion = 3, names = { "reggaeworldcrew.net" }, urls = { "https?://(www\\.)?reggaeworldcrew\\.net/foro/.*" }, flags = { 0 })
public class RggwrldcrwNet extends PluginForDecrypt {

    public RggwrldcrwNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        ReggaeWorldCrewNet.login(br);
        br.getPage(parameter);

        // String befoire = br.toString();
        String title = Encoding.htmlDecode(br.getRegex("<title>(.*?)</title>").getMatch(0));
        br.setFollowRedirects(false);

        String keywords = br.getRegex("<meta\\s+name=\"keywords\"\\s+content=\"([^\"]+)").getMatch(0);
        for (String link : keywords.split("\\s*,\\s*")) {
            if (link.startsWith("http") && !link.contains(br.getHost())) {
                link = Encoding.htmlDecode(link);
                System.out.println(link);
                decryptedLinks.add(createDownloadlink(link));
            }
        }
        try {
            String[] posts = br.getRegex("<li[^>]+class=\"[^\"]*postcontainer[^\"]*\"[^>]*>(.*?<div[^>]+class=\"postfoot\"[^>]*>.*?)</li>").getColumn(0);
            boolean saidThanks = false;
            for (String post : posts) {

                if (post.contains("/images/misc/locked-htnx.gif")) {
                    // need top say thanks
                    String pid = new Regex(post, "\"post(\\d+)\"").getMatch(0);
                    saidThanks = true;

                    br.getPage("showthread.php?thanks_mode=add&postid=" + pid);

                }
            }
            if (saidThanks) {
                br.getPage(parameter);
            }
            String[] redirects = br.getRegex("redirector.php\\?url=([^\"]+)").getColumn(0);
            for (String s : redirects) {
                decryptedLinks.add(createDownloadlink(Encoding.urlDecode(Encoding.htmlDecode(s), true)));
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