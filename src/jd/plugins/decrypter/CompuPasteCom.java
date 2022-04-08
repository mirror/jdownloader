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
import java.net.URLDecoder;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "compupaste.com" }, urls = { "https?://(?:[a-z0-9]+\\.)?compupaste\\.com/\\?v=[A-Za-z0-9]+" })
public class CompuPasteCom extends PluginForDecrypt {
    public CompuPasteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Tags: Pastebin */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)no existe\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Form captchaForm = br.getForm(0);
        if (captchaForm != null && SolveMedia.containsSolvemediaCaptcha(captchaForm)) {
            boolean success = false;
            for (int i = 0; i <= 3; i++) {
                final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                File cf = null;
                try {
                    cf = sm.downloadCaptcha(getLocalCaptchaFile());
                } catch (final InterruptedException e) {
                    throw e;
                } catch (final Exception e) {
                    if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support", -1, e);
                    } else {
                        throw e;
                    }
                }
                final String code = getCaptchaCode("solvemedia", cf, param);
                String chid = null;
                try {
                    chid = sm.getChallenge(code);
                } catch (final PluginException e) {
                    if (e.getLinkStatus() == LinkStatus.ERROR_CAPTCHA) {
                        logger.info("Wrong captcha");
                        continue;
                    } else {
                        throw e;
                    }
                }
                captchaForm.put("adcopy_challenge", chid);
                br.submitForm(captchaForm);
                if (!SolveMedia.containsSolvemediaCaptcha(br)) {
                    success = true;
                    break;
                } else {
                    logger.info("Wrong captcha");
                }
            }
            if (!success) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        final String htmlToCrawlLinksFrom;
        final String pasteText = br.getRegex("class\\s*=\\s*\"tab_content\"[^>]*>\\s*(.*?)\\s*<div[^>]*id\\s*=\\s*\"wp-pagenavi\"\\s*>").getMatch(0);
        if (pasteText != null) {
            htmlToCrawlLinksFrom = pasteText;
        } else {
            logger.warning("Failed to find pastebin text --> Using fallback");
            htmlToCrawlLinksFrom = br.getRequest().getHtmlCode();
        }
        final String[] links = HTMLParser.getHttpLinks(htmlToCrawlLinksFrom, br.getURL());
        for (final String singleLink : links) {
            if (!this.canHandle(singleLink)) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
        /* Look for Clickv and Load Forms - those can contain additional mirrors. */
        final Form[] forms = br.getForms();
        if (forms.length > 0) {
            final Browser brc = br.cloneBrowser();
            for (final Form form : forms) {
                if (form.getAction() != null && form.getAction().matches(".*127\\.0\\.0\\.1:\\d+.*")) {
                    final InputField jk = form.getInputFieldByName("jk");
                    final InputField crypted = form.getInputFieldByName("crypted");
                    if (jk != null && StringUtils.isNotEmpty(jk.getValue()) && crypted != null && StringUtils.isNotEmpty(crypted.getValue())) {
                        final DownloadLink dummyCnl = DummyCNL.createDummyCNL(URLDecoder.decode(crypted.getValue(), "UTF-8"), URLDecoder.decode(jk.getValue(), "UTF-8"), null, param.getCryptedUrl());
                        decryptedLinks.add(dummyCnl);
                    } else {
                        brc.submitForm(form);
                    }
                }
            }
        }
        if (decryptedLinks.isEmpty()) {
            logger.info("Failed to find any results");
            return decryptedLinks;
        }
        return decryptedLinks;
    }
}