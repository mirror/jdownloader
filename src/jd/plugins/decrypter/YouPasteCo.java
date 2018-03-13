//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptcha;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

/**
 * notes: using cloudflare
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youpaste.co" }, urls = { "http://(www\\.)?youpaste\\.co/(?:index\\.php/paste|p)/[a-zA-Z0-9_/\\+\\=\\-]+" })
public class YouPasteCo extends antiDDoSForDecrypt {
    public YouPasteCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        getPage(parameter);
        if (br.containsHTML(">Lo sentimos, este paste no existe")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("name=\"capcode\" id=\"capcode\"")) {
            final int repeat = 3;
            // using dummie DownloadLink for auto retry code within handleKeyCaptcha
            final DownloadLink dummie = createDownloadlink(parameter);
            for (int i = 0; i < repeat; i++) {
                String result = handleCaptchaChallenge(new KeyCaptcha(this, br, dummie).createChallenge(this));
                if ("CANCEL".equals(result)) {
                    return decryptedLinks;
                }
                if (result == null) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                postPage(br.getURL(), "capcode=" + Encoding.urlEncode(result));
                if (br.containsHTML("name=\"capcode\" id=\"capcode\"")) {
                    if (i + 1 == repeat) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    continue;
                } else {
                    break;
                }
            }
        }
        processLinks(decryptedLinks, parameter);
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> processLinks(final ArrayList<DownloadLink> decryptedLinks, String parameter) throws Exception {
        // they present links in <center>
        final String center = br.getRegex("<div class=\"pasteContent.*?</div>").getMatch(-1);
        String[] links = new Regex((center != null ? center : br.toString()), "(?:https?:|ftp:)?//[^\"'<>\t\r\n]{2,}|href\\s*=\\s*('|\"|)(&#x[a-f0-9]{2};)+\\1").getColumn(-1);
        if (links == null || links.length == 0) {
            links = br.getRegex("<p[^>]*>(https?://.*?)\\1</pre>").getColumn(0);
        }
        for (final String link : links) {
            decryptedLinks.add(createDownloadlink(link));
        }
        {
            final ArrayList<String> crypted = new ArrayList<String>();
            // click and load support. when forms are not provided only input fields within <div>
            final boolean input = br.containsHTML("<input\\s+([^>]+crypted=(?:'|\"|)[a-z0-9A-Z\\-_]+)");
            final boolean button = br.containsHTML("<button\\s+([^>]+crypted=(?:'|\"|)[a-z0-9A-Z\\-_]+)");
            if (button) {
                // correction, sometimes crypted is in a button and not inputfield. its easy to correct it here
                final String correction = br.toString().replaceAll("<button\\s+([^>]+crypted=(?:'|\"|)[a-z0-9A-Z\\-_]+)", "<input$1");
                final ArrayList<InputField> inputfields = this.getInputFields(correction);
                for (final InputField i : inputfields) {
                    final String cry = i.getProperty("crypted", null);
                    if (cry != null) {
                        crypted.add(cry);
                    }
                }
            }
            for (final String c : crypted) {
                // here they go to linkbucks but we should be able to bypass this
                Browser br2 = br.cloneBrowser();
                try {
                    br2.getHeaders().put("Accept", "*/*");
                    br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    postPage(br2, "/index.php/cnl/ss/?" + System.currentTimeMillis(), "source=" + Encoding.urlEncode(br2.getURL()) + "&crypted=" + Encoding.urlEncode(c) + "&send=send");
                    final String a = br2.toString();
                    br2 = br.cloneBrowser();
                    br2.getHeaders().put("Referer", (button ? "" : "http://www.linkbucks.com/b4d7d9f8/url/http://youpaste.co/index.php/cnl/" + a));
                    Thread.sleep(10000);
                    getPage(br2, "/index.php/cnl/" + a);
                    br2.getHeaders().put("Referer", null);
                } catch (final Exception e) {
                    continue;
                }
                /* use cnl2 button if available */
                String cnlUrl = "http://127\\.0\\.0\\.1:9666/flash/addcrypted2";
                if (br2.containsHTML(cnlUrl)) {
                    final Browser cnlbr = br2.cloneBrowser();
                    Form cnlForm = null;
                    for (Form f : cnlbr.getForms()) {
                        if (f.containsHTML(cnlUrl)) {
                            cnlForm = f;
                            break;
                        }
                    }
                    if (cnlForm != null) {
                        if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
                            String jk = cnlbr.getRegex("<input type=\"hidden\" name=\"jk\" value=\"([^\"]+)\"").getMatch(0);
                            HashMap<String, String> infos = new HashMap<String, String>();
                            infos.put("crypted", Encoding.urlDecode(cnlForm.getInputField("crypted").getValue(), false));
                            infos.put("jk", jk);
                            String source = cnlForm.getInputField("source").getValue();
                            if (StringUtils.isEmpty(source)) {
                                source = parameter.toString();
                            } else {
                                source = Encoding.urlDecode(source, true);
                            }
                            infos.put("source", source);
                            String json = JSonStorage.toString(infos);
                            final DownloadLink dl = createDownloadlink("http://dummycnl.jdownloader.org/" + HexFormatter.byteArrayToHex(json.getBytes("UTF-8")));
                            decryptedLinks.add(dl);
                            return decryptedLinks;
                        } else {
                            String jk = cnlbr.getRegex("<input type=\"hidden\" name=\"jk\" value=\"([^\"]+)\"").getMatch(0);
                            cnlForm.remove("jk");
                            cnlForm.put("jk", (jk != null ? jk.replaceAll("\\+", "%2B") : "nothing"));
                            try {
                                cnlbr.submitForm(cnlForm);
                                if (cnlbr.containsHTML("success")) {
                                    return decryptedLinks;
                                }
                                if (cnlbr.containsHTML("^failed")) {
                                    logger.warning("CNL2 Postrequest was failed! Please upload now a logfile, contact our support and add this loglink to your bugreport!");
                                    logger.warning("CNL2 Message: " + cnlbr.toString());
                                }
                            } catch (Throwable e) {
                                logger.info("ExternInterface(CNL2) is disabled!");
                            }
                        }
                    }
                }
            }
        }
        return decryptedLinks;
    }

    public ArrayList<InputField> getInputFields(final String string) {
        ArrayList<InputField> inputfields = new ArrayList<InputField>();
        final Matcher matcher = Pattern.compile("(?s)(<[\\s]*(input|textarea|select).*?>)", Pattern.CASE_INSENSITIVE).matcher(string);
        while (matcher.find()) {
            final InputField nv = InputField.parse(matcher.group(1));
            if (nv != null) {
                inputfields.add(nv);
            }
        }
        return inputfields;
    }
}