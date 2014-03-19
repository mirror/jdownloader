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
import java.util.logging.Level;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "binbox.io" }, urls = { "https?://(www\\.)?binbox\\.io/\\w+#\\w+" }, flags = { 0 })
public class BinBoxIo extends PluginForDecrypt {

    private static String sjcl;

    public BinBoxIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("https://", "http://");
        br.getPage(parameter);
        if (br.containsHTML(">Page Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>([^<>\"]*?)\\- Binbox</title>").getMatch(0);
        final String salt = parameter.substring(parameter.lastIndexOf("#") + 1);
        String paste = getPaste();
        if (paste == null) {
            if (br.containsHTML("solvemedia\\.com/papi/")) {
                final String validate = br.getRegex("type=\"hidden\" name=\"validate\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (validate == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (int i = 1; i <= 3; i++) {
                    final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                    final jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((jd.plugins.decrypter.LnkCrptWs) solveplug).getSolveMedia(br);
                    File cf = null;
                    try {
                        cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    } catch (final Exception e) {
                        if (jd.plugins.decrypter.LnkCrptWs.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                        throw e;
                    }
                    final String code = getCaptchaCode(cf, param);
                    final String chid = sm.getChallenge(code);
                    br.postPage(br.getURL(), "validate=" + validate + "&adcopy_response=" + Encoding.urlEncode(code) + "&adcopy_challenge=" + chid);
                    if (br.containsHTML("solvemedia\\.com/papi/")) continue;
                    break;
                }
                if (br.containsHTML("solvemedia\\.com/papi/")) throw new DecrypterException(DecrypterException.CAPTCHA);
                paste = getPaste();
            }
        }

        if (salt != null && paste != null) {
            paste = paste.replace("&quot;", "\"");
            paste = Encoding.Base64Decode(paste);
            if (isEmpty(sjcl)) sjcl = br.getPage("/public/js/sjcl.js");
            final String[] links = decryptLinks(salt, paste);
            if (links == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            } else if (links.length == 0) {
                logger.info("Link offline (empty): " + parameter);
                return decryptedLinks;
            }
            for (final String singleLink : links) {
                if (!singleLink.startsWith("http")) continue;
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }

        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private String[] decryptLinks(final String salt, final String paste) throws Exception {
        String result = null;
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("ECMAScript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(sjcl);
            engine.eval("function sjclDecrypt(salt, paste){return sjcl.decrypt(salt, paste)}");
            result = (String) inv.invokeFunction("sjclDecrypt", salt, paste);
        } catch (final ScriptException e) {
            return new String[0];
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
        if (isEmpty(result)) return null;
        return result.split("[\r\n]+");
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    private String getPaste() {
        return br.getRegex("<div id=\"paste\\-json\" style=\"[^\"]+\">([^<]+)</div>").getMatch(0);
    }

}