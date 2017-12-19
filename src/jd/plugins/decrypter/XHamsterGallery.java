//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "xhamster.com" }, urls = { "https?://(www\\.)?((de|es|ru|fr|it|jp|pt|nl|pl)\\.)?xhamster\\.com/photos/(gallery/[0-9A-Za-z_\\-/]+(\\.html)?|view/[0-9A-Za-z_\\-/]+(\\.html)?)" })
public class XHamsterGallery extends PluginForDecrypt {
    public XHamsterGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("www.", "");
        /* Force english language */
        final String replace_string = new Regex(parameter, "(https?://(www\\.)?((de|es|ru|fr|it|jp|pt|nl|pl)\\.)?xhamster\\.com/)").getMatch(0);
        parameter = parameter.replace(replace_string, "https://xhamster.com/");
        br.addAllowedResponseCodes(410);
        br.addAllowedResponseCodes(452);
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        // Login if possible
        getUserLogin(false);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        /* Error handling */
        if (br.getHttpConnection().getResponseCode() == 410 || br.containsHTML("Sorry, no photos found|error\">Gallery not found<|>Page Not Found<")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.containsHTML(">This gallery is visible for")) {
            logger.info("This gallery is only visible for specified users, account needed: " + parameter);
            decryptedLinks.add(createOfflinelink(parameter, "Private gallery"));
            return decryptedLinks;
        }
        if (br.containsHTML(">This gallery needs password<")) {
            boolean failed = true;
            for (int i = 1; i <= 3; i++) {
                String passCode = getUserInput("Password?", param);
                br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode));
                if (br.containsHTML(">This gallery needs password<")) {
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        if (new Regex(br.getURL(), "/gallery/[0-9]+/[0-9]+").matches()) { // Single picture
            DownloadLink dl = createDownloadlink("directhttp://" + br.getRegex("class='slideImg'\\s+src='([^']+)").getMatch(0));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final String urlWithoutPageParameter = this.br.getURL();
        final String total_numberof_picsStr = this.br.getRegex("<h1 class=\"gr\">[^<>]+<small>\\[(\\d+) [^<>\"]+\\]</small>").getMatch(0);
        final int total_numberof_picsInt = total_numberof_picsStr != null ? Integer.parseInt(total_numberof_picsStr) : -1;
        String fpname = br.getRegex("<title>(.*?) \\- \\d+ (Pics|Bilder) \\- xHamster\\.com</title>").getMatch(0);
        if (fpname == null) {
            fpname = br.getRegex("<title>(.*?)\\s*>\\s*").getMatch(0);
        }
        if (fpname == null) {
            /* Final fallback */
            fpname = new Regex(parameter, "/(?:gallery|view)/(.+)(?:\\.html)?").getMatch(0);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpname.trim()));
        int pageIndex = 1;
        Boolean next = true;
        while (next) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                break;
            }
            if (pageIndex > 1) {
                br.getPage(urlWithoutPageParameter + "/" + pageIndex);
                if (br.getHttpConnection().getResponseCode() == 452 || br.containsHTML(">Page Not Found<")) {
                    break;
                }
                if (!br.containsHTML(">Next<")) {
                    next = false;
                }
            }
            String allLinks = br.getRegex("class='iListing'>(.*?)id='galleryInfoBox'>").getMatch(0);
            if (allLinks == null) {
                allLinks = br.getRegex("id='imgSized'(.*?)gid='\\d+").getMatch(0);
            }
            final String json_source = br.getRegex("\"photos\":(\\[\\{.*?\\}\\])").getMatch(0);
            // logger.info("json_source: " + json_source);
            if (json_source != null) {
                final ArrayList<Object> lines = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(json_source);
                for (final Object line : lines) {
                    // logger.info("line: " + line);
                    if (line instanceof Map) {
                        final Map<String, Object> entries = (Map<String, Object>) line;
                        final String imageURL = (String) entries.get("imageURL");
                        if (imageURL != null) {
                            // logger.info("imageURL: " + imageURL);
                            final DownloadLink dl = createDownloadlink(imageURL);
                            dl.setAvailable(true);
                            dl._setFilePackage(fp);
                            distribute(dl);
                            decryptedLinks.add(dl);
                        }
                    }
                }
            }
            pageIndex++;
        }
        if (total_numberof_picsInt != -1 && decryptedLinks.size() < total_numberof_picsInt) {
            logger.warning("Seems like not all images have been found");
        }
        return decryptedLinks;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("xhamster.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.XHamsterCom) hostPlugin).setBrowser(br);
            ((jd.plugins.hoster.XHamsterCom) hostPlugin).login(aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}