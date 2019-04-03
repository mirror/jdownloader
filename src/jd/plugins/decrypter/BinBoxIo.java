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
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "binbox.io" }, urls = { "https?://(?:www\\.)?binbox\\.io/(?!0|about|api|earn|help|login|register|submit|tour)\\w+(?:#\\w+)?" })
public class BinBoxIo extends antiDDoSForDecrypt {
    private String sjcl, uid, salt, token, paste;

    public BinBoxIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("https://", "http://");
        getPage(parameter);
        if (true) {
            try {
                // https://github.com/adsbypasser/adsbypasser
                final Browser brc = br.cloneBrowser();
                final GetRequest request = brc.createGetRequest(parameter + ".json");
                request.getCookies().add(new Cookie(getHost(), "referrer", "1"));
                sendRequest(brc, request);
                final Map<String, Object> response = JSonStorage.restoreFromString(request.getHtmlCode(), TypeRef.HASHMAP);
                if (Boolean.TRUE.equals(response.get("ok"))) {
                    final Map<String, Object> paste = (Map<String, Object>) response.get("paste");
                    final String urls[] = StringUtils.getLines((String) paste.get("url"));
                    for (final String url : urls) {
                        if (StringUtils.isNotEmpty(url)) {
                            decryptedLinks.add(createDownloadlink(url));
                        }
                    }
                    final String title = (String) paste.get("title");
                    if (title != null) {
                        final FilePackage fp = FilePackage.getInstance();
                        fp.setName(Encoding.htmlDecode(title.trim()));
                        fp.addLinks(decryptedLinks);
                    }
                    return decryptedLinks;
                }
            } catch (final PluginException e) {
                throw e;
            } catch (final InterruptedException e) {
                throw e;
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        if (br.containsHTML(">Page Not Found<|<h2 id=('|\"|)title\\1>Access Denied</h2>")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>([^<>\"]*?)- Binbox</title>").getMatch(0);
        uid = new Regex(parameter, "io/(\\w+)").getMatch(0);
        salt = parameter.substring(parameter.lastIndexOf("#") + 1);
        getPaste();
        if (paste == null) {
            Form captcha = br.getFormbyProperty("id", "captchaForm");
            if (captcha != null && captcha.containsHTML("solvemedia\\.com/papi/")) {
                for (int i = 1; i <= 3; i++) {
                    final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                    File cf = null;
                    try {
                        cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    } catch (final Exception e) {
                        if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                            throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                        }
                        throw e;
                    }
                    final String code = getCaptchaCode("solvemedia", cf, param);
                    if ("".equals(code)) {
                        // refresh (f5) button returns "", but so does a empty response by the user (send button)
                        continue;
                    }
                    final String chid = sm.getChallenge(code);
                    captcha.put("adcopy_response", Encoding.urlEncode(code));
                    captcha.put("adcopy_challenge", Encoding.urlEncode(chid));
                    submitForm(captcha);
                    if (br.containsHTML("solvemedia\\.com/papi/")) {
                        continue;
                    }
                    break;
                }
                if (br.containsHTML("solvemedia\\.com/papi/")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                getPaste();
            } else {
                // unsupported captcha type, or broken
                // logger.warning("Possible unsupported captcha type or broken decrypter, please confirm in browser.");
                // return null;
            }
        }
        doThis();
        if (br.containsHTML("<h1 id=('|\"|)paste-title\\1 class=('|\"|)float left\\2>\\[REMOVED\\]</h1>")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (salt != null && paste != null) {
            paste = paste.replace("&quot;", "\"");
            paste = Encoding.Base64Decode(paste);
            if (isEmpty(sjcl)) {
                final Browser brc = br.cloneBrowser();
                getPage(brc, "/public/js/sjcl.js");
                sjcl = brc.toString();
            }
            final String[] links = decryptLinks();
            if (links == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            } else if (links.length == 0) {
                logger.info("Link offline (empty): " + parameter);
                return decryptedLinks;
            }
            for (final String singleLink : links) {
                if (!singleLink.startsWith("http")) {
                    continue;
                }
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
        // doesn't always have salt etc.
        if (parameter.equals(salt)) {
            final String pasteText = br.getRegex("<div id=\"paste-text\".*?</div>").getMatch(-1);
            if (pasteText != null) {
                final String[] links = HTMLParser.getHttpLinks(pasteText, null);
                for (final String link : links) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
        }
        if (decryptedLinks.size() == 0) {
            if (br.containsHTML(/* password content.. unsupported feature */">Password Required</h1>")) {
                try {
                    decryptedLinks.add(createOfflinelink(parameter, "password required, unsupported feature", null));
                } catch (final Throwable t) {
                    logger.info("Link offline: " + parameter);
                }
            } else if (br.containsHTML(/* DCMA */"<div id=\"paste-deleted\"" +
                    /* suspended or deactivated account */"|This link is unavailable because |" +
                    /* content deleted */"The content you have requested has been deleted\\.")) {
                try {
                    decryptedLinks.add(createOfflinelink(parameter, fpName != null ? Encoding.htmlDecode(fpName.trim()) : null, null));
                } catch (final Throwable t) {
                    logger.info("Link offline: " + parameter);
                }
            } else {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String[] decryptLinks() throws Exception {
        String result = null;
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(sjcl);
            engine.eval("function sjclDecrypt(salt, paste){return sjcl.decrypt(salt, paste)}");
            result = (String) inv.invokeFunction("sjclDecrypt", salt, paste);
        } catch (final ScriptException e) {
            return new String[0];
        } catch (final Exception e) {
            logger.log(e);
            return null;
        }
        if (isEmpty(result)) {
            return null;
        }
        final String[] links = HTMLParser.getHttpLinks(result, null);
        return links;
    }

    private void doThis() throws Exception {
        // 20141030
        token = br.getRegex("\\?token=([a-f0-9]{40})").getMatch(0);
        Form action = br.getFormbyProperty("id", "paste-form");
        if (action != null) {
            if (action.getAction().contains("'+hash+")) {
                action.setAction(action.getAction().replaceAll("'\\s*\\+\\s*hash\\s*\\+'", ""));
            }
            br.setFollowRedirects(true);
            br.submitForm(action);
            getPaste();
        }
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    private void getPaste() {
        paste = br.getRegex("<div id=\"paste\\-json\" style=\"[^\"]+\">([^<]+)</div>").getMatch(0);
    }
}