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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adcrun.ch" }, urls = { "http://(www\\.)?adcrun\\.ch/[A-Za-z0-9]+" }, flags = { 0 })
public class AdcrunCh extends PluginForDecrypt {

    public AdcrunCh(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        br.setFollowRedirects(false);
        br.getPage(parameter);
        String finallink = br.getRedirectLocation();
        if (finallink != null) {
            if (finallink.equals("http://adcrun.ch/")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (finallink.matches("http://(www\\.)?adcrun\\.ch/(image|slider).+")) {
                logger.info("Link invalid: " + parameter);
                return decryptedLinks;
            }
        } else if (br.containsHTML("<title>Link Locked</title>")) {
            // Survey needed to get finallink...NOT
            finallink = br.getRegex("\\&longurl=(http[^<>\"]*?)\"").getMatch(0);
        } else {
            if (br.containsHTML(">404 Not Found<")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }

            final String previousFinallink = br.getRegex("<iframe class=\\'fly_frame\\' src=\\'(http[^<>\"]*?)\\'").getMatch(0);

            if (finallink == null) {
                br.getHeaders().put("Referer", parameter);
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");

                String result = null;
                final ScriptEngineManager manager = new ScriptEngineManager();
                final ScriptEngine engine = manager.getEngineByName("javascript");
                try {
                    result = engine.eval(br.getRegex("eval(.*?)\n").getMatch(0)).toString();
                } catch (final Throwable e) {
                    return null;
                }

                int wait = 10;
                final String waittime = br.getRegex("id=\"redirectin\">(\\d+)</span>").getMatch(0);
                if (waittime != null) {
                    wait = Integer.parseInt(waittime);
                }

                final String nextUrl = "adcrun.ch/links";
                /* variable JS-Arrays in "Form" bringen */
                final String[] res = new Regex(result, "opt:(.*?\\})").getColumn(0);
                if (res == null || res.length == 0) { return null; }
                for (String r : res) {
                    String post = null;
                    r = r.replaceAll("\'|\\{|\\}|args:", "");
                    final String[] a = r.split(",");
                    if (a == null || a.length == 0) { return null; }
                    for (String f : a) {
                        f = f.replaceAll("ref:.+", "ref:");
                        post = post == null ? "opt=" : post + "&args[";
                        if (post.matches(".+args\\[")) {
                            post += f.replaceAll(":", "]=");
                        } else {
                            post += f;
                        }
                    }
                    if (!post.contains("make_log")) {
                        sleep(1000 * wait, param);
                    }
                    br.postPage("http://" + nextUrl + "/ajax.fly.php", post);
                    if (br.containsHTML("\\{\"error\":false,\"message\":false\\}")) {
                        finallink = previousFinallink;
                    } else {
                        finallink = br.getRegex("\"url\":\\s?\"(.*?)\"").getMatch(0);
                        if (finallink != null) finallink = finallink.replace("\\", "");
                    }
                }
            }
        }

        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
