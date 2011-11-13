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

import java.io.File;
import java.util.ArrayList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adjoin.me" }, urls = { "http://(www\\.)?adjoin\\.me/.+" }, flags = { 0 })
public class AdJoinMe extends PluginForDecrypt {

    // Plugin has structure like ZPagEs
    /* must be static so all plugins share same lock */
    private static final Object LOCK         = new Object();

    private static final String LINKREGEX    = "window\\.location = \"(http://.*?)\"";
    private static final String CAPTCHATEXT  = "google\\.com/recaptcha/api";
    private static final String CAPTCHATEXT2 = "adjoin.me/cap";

    public AdJoinMe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String decodeDownloadLink(final String s, final CryptedLink param) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            result = engine.eval(s);
        } catch (final Throwable e) {
            result = null;
        }
        if (result == null) { return null; }

        final Browser br2 = br.cloneBrowser();
        br2.setReadTimeout(120 * 1000);
        final String res = "url\":\"(.*?)\"";
        int waittime = 10;

        String[] args = new Regex(result, "\\$\\.post\\(\\'(.*?)\\',\\{opt:\\'(.*?)\\',args:\\{lid:(\\d+),oid:(\\d+)\\}").getRow(0);
        final String wait = new Regex(result, "else\\{.+?=(\\d+)\\}\\}").getMatch(0);
        if (args == null || args.length != 4) { return null; }

        br2.getHeaders().put("Referer", param.toString());
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String postData = "opt=" + args[1] + "&args%5Blid%5D=" + args[2] + "&args%5Boid%5D=" + args[3];

        // req#1
        br2.postPage(args[0], postData);

        if (!br2.getRegex("url\":\"#\"").matches()) {
            if (wait != null) {
                logger.info(" Waittime detected, waiting " + wait + " seconds from now on...");
                waittime = Integer.parseInt(wait);
            }
            sleep(waittime * 1000, param);

            // req#2
            br2.postPage(args[0], postData);
            if (!br2.getRegex("url\":\"#\"").matches()) { return null; }
        }

        args = new Regex(result, "skip_ad\\.click\\(function\\(\\)\\{\\$\\.post\\(\\'(.*?)\\',\\{opt:\\'(.*?)\\',args:\\{aid:(\\d+),lid:(\\d+),oid:(\\d+),ref:\\'(.*?)\\'\\}").getRow(0);
        if (args == null || args.length != 6) { return null; }
        // req#3
        br2.postPage(args[0], "opt=" + args[1] + "&args%5Baid%5D=" + args[2] + "&args%5Blid%5D=" + args[3] + "&args%5Boid%5D=" + args[4] + "args%5Bref%5D=" + args[5]);

        return br2.getRegex(res).getMatch(0);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setReadTimeout(120 * 1000);
        br.getPage(parameter);
        if (br.containsHTML("(>404 Not Found<|This link is either removed due to violated the Terms and Conditions or is deleted)")) { return decryptedLinks; }
        synchronized (LOCK) {
            String link = null;
            final String cryptedScripts[] = br.getRegex("eval(.*?)\n").getColumn(0);
            if (cryptedScripts != null && cryptedScripts.length != 0) {
                for (final String crypted : cryptedScripts) {
                    link = decodeDownloadLink(crypted, param);
                    if (link != null) {
                        link = link.replaceAll("\\\\", "");
                        break;
                    }
                }
            }
            if (link == null) {
                if (br.containsHTML(CAPTCHATEXT) || br.getURL().contains(CAPTCHATEXT2)) {
                    for (int i = 0; i <= 5; i++) {
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        rc.parse();
                        rc.getForm().setAction("http://adjoin.me/receiveRecaptcha.do");
                        rc.getForm().put("URL", parameter);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode(cf, param);
                        rc.setCode(c);
                        if (br.containsHTML(CAPTCHATEXT) || br.getURL().contains(CAPTCHATEXT2)) {
                            continue;
                        }
                        break;
                    }
                    if (br.containsHTML(CAPTCHATEXT) || br.getURL().contains(CAPTCHATEXT2)) { throw new DecrypterException(DecrypterException.CAPTCHA); }

                }
                link = br.getRegex(LINKREGEX).getMatch(0);
                if (link == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
            }
            // If we don't wait we have to enter reCaptchas more often which
            // would need even more time for ;)
            sleep(3000, param);
            decryptedLinks.add(createDownloadlink(link));
        }
        return decryptedLinks;
    }
}
