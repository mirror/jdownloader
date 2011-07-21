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

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ultra-protect.com" }, urls = { "http://(www\\.)?ultra\\-protect\\.com/linkcheck\\.php\\?linkid=[a-z0-9]+" }, flags = { 0 })
public class UltraPrtcCom extends PluginForDecrypt {

    public UltraPrtcCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        boolean failed = true;
        for (int i = 0; i <= 5; i++) {
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(cf, param);
            rc.setCode(c);
            if (br.containsHTML("(The CAPTCHA wasn\\'t entered correctly\\. Go back and try it again\\.|reCAPTCHA said: incorrect\\-captcha\\-sol)")) {
                br.getPage(parameter);
                continue;
            }
            failed = false;
            break;
        }
        if (failed) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        final String fpName = br.getRegex("<title>(.*?) \\| Ultra\\-Protect\\.com</title>").getMatch(0);
        String[] cryptedLinks = br.getRegex("</td><td style=\\'font\\-size:70%;font\\-family:Arial,Helvetica,sans\\-serif;\\'>[\t\n\r ]+<a href=(show.*?) target=_blank").getColumn(0);
        if (cryptedLinks == null || cryptedLinks.length == 0) {
            cryptedLinks = br.getRegex("(show\\.php\\?id=[A-Za-z0-9/\\=]+)").getColumn(0);
        }
        if (cryptedLinks == null || cryptedLinks.length == 0) { return null; }
        final String algo = br.getPage("http://www.ultra-protect.com/src.js");
        progress.setRange(cryptedLinks.length);
        for (final String cryptedLink : cryptedLinks) {
            br.getPage("http://www.ultra-protect.com/" + cryptedLink);
            final String fun = br.getRegex("var ppp=B\\.d\\(\"(.*?)\"\\)").getMatch(0);
            final String finallink = decryptSingleLink(algo, fun);
            if (finallink == null) {
                logger.warning("Decrypter failed for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    public String decryptSingleLink(final String algo, final String fun) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(algo);
            result = inv.invokeMethod(engine.get("B"), "d", fun);
        } catch (final ScriptException e) {
            return null;
        }
        if (result != null) { return new Regex(result.toString(), "SRC=(.*?)>").getMatch(0); }
        return null;
    }
}
