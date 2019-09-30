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

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornze.com" }, urls = { "https?://((www|player)\\.)?pornze\\.com/(video/)?[^/]+/?" })
public class PornZeCom extends antiDDoSForDecrypt {
    public PornZeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<meta\\s*property\\s*=\\s*\"og:title\"[^>]+content\\s*=\\s*\"([^\"]+)\"[^>]+>").getMatch(0);
        String[] links = br.getRegex("<iframe[^>]+src\\s*=\\s*\"([^\"]+)\"[^>]+class\\s*=\\s*\"[^\"]*wps-player[^\"]*\"[^>]*>").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("<div[^>]*class\\s*=\\s*\"responsive-player\"[^>]*>\\s*<iframe[^>]+src\\s*=\\s*\"([^\"]+)\"[^>]*>").getColumn(0);
        }
        if (links == null || links.length == 0) {
            if (StringUtils.containsIgnoreCase(br.getHost(true), "player.")) {
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                // String jsJuicyLibUrl = brc.getRegex("<script[^>]+src\\s*=\\s*\"(?://)?([^\"]+/jwplayer/[^\"]+)").getMatch(0);
                String jsJuicyCall = brc.getRegex("JuicyCodes\\.Run\\s*\\(\\s*\"([^<]+)\"\\s*\\)").getMatch(0);
                if (StringUtils.isNotEmpty(jsJuicyCall)) {
                    jsJuicyCall = jsJuicyCall.replaceAll("[\"\\s+]+", "");
                    jsJuicyCall = new String(Base64.decode(jsJuicyCall));
                    final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
                    final ScriptEngine engine = manager.getEngineByName("javascript");
                    try {
                        engine.eval("var res = " + jsJuicyCall + ";");
                        // TODO: Modify variable jsJuicyCall to prepend a JS class "jwplayer" that allows us to grab its config data.
                        // Deobfuscated Example for https://player.pornze.com/video/OM6vjRRldXdK3Dq/ as follows (variable still has the
                        // "(p,a,c,k,e,d)" version:
                        //
                        // var player = jwplayer("video_player");
                        // var config = {
                        // advertising: {client: "vast",schedule: {adbreak1: {offset: "pre",tag:
                        // "https://tsyndicate.com/do2/6403549152464484b87bff9dc88ff1dc/vast?",skipoffset: 5},},},
                        // width: "",
                        // height: "100%",
                        // aspectratio: "16:9",
                        // autostart: false,
                        // controls: true,
                        // primary: "html5",
                        // abouttext: "pornze.com",
                        // aboutlink: "http://pornze.com/",
                        // image: "http://pornze.com/wp-content/uploads/2018/12/Facial-for-a-rasta-teen-girlfriend-pornze.jpg",
                        // sources: [{"file":
                        // "https://player.pornze.com/link/OM6vjRRldXdK3Dq/360/334c97d792d98f4f369164b2d61b00ae/","label": "360P","type":
                        // "video/mp4"}],
                        // tracks: null,
                        // logo: {file: "",link: "http://pornze.com/",position: "top-left",},
                        // captions: {color: "#efbb00",fontSize: "14",fontFamily: "Trebuchet MS, Sans Serif",backgroundColor: "rgba(0, 0, 0,
                        // 0.4);",}
                        // };
                        // player.setup(config);
                        String result = (String) engine.get("res");
                    } catch (final Exception e) {
                        e.printStackTrace();
                        e.printStackTrace(); // HINT: Breakpoint here!!!
                    }
                }
            }
        }
        if (links != null) {
            for (String link : links) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
            }
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}