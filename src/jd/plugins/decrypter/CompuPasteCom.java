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
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class CompuPasteCom extends PluginForDecrypt {
    public CompuPasteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.PASTEBIN };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "compupaste.com" });
        ret.add(new String[] { "lupaste.com" });
        ret.add(new String[] { "pfree.gatonplayseries.com" });
        /* 2023-08-09 */
        ret.add(new String[] { "avohdlinks.latinomegahd.net" });
        ret.add(new String[] { "hopepaste.download" });
        ret.add(new String[] { "fullpaste.todofullxd.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://" + buildHostsPatternPart(domains) + "/(?:index\\.php)?\\?v=([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * This plugin is purposely not using AbstractPastebinCrawler because although it looks like a pastebin website, it works more like a
     * linkcrypter.
     */
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String contenturl = param.getCryptedUrl();
        if (this.getHost().equalsIgnoreCase("hopepaste.download")) {
            contenturl = contenturl.replaceFirst("(?i)http://", "https://");
        }
        br.getPage(contenturl);
        if (this.getHost().equalsIgnoreCase("hopepaste.download") && br.getHttpConnection().getResponseCode() == 403 && br.getURL().contains("/.?")) {
            /* 2023-09-18: Small workaround for buggy website when redirect from http to https is supposed to happen. */
            contenturl = br.getURL().replace("/.?", "/?");
            br.getPage(contenturl);
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)no existe\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Form captchaForm = br.getForm(0);
        if (captchaForm != null) {
            if (SolveMedia.containsSolvemediaCaptcha(captchaForm)) {
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
                    captchaForm.put("adcopy_challenge", Encoding.urlEncode(chid));
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
            } else if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(captchaForm)) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                br.submitForm(captchaForm);
            } else {
                logger.info("No captcha required or required captcha is not supported");
            }
        }
        String title = null;
        final String[] h3s = br.getRegex("<h3>([^<]+)</h3>").getColumn(0);
        if (h3s != null && h3s.length > 0) {
            /* Some websites use multiple h3 tags (fuu) e.g. lupaste.com */
            title = h3s[h3s.length - 1];
        }
        final String htmlToCrawlLinksFrom;
        final String pasteText = br.getRegex("class\\s*=\\s*\"tab_content\"[^>]*>\\s*(.*?)\"wp-pagenavi\"\\s*>").getMatch(0);
        if (pasteText != null) {
            htmlToCrawlLinksFrom = pasteText;
        } else {
            logger.warning("Failed to find pastebin text --> Using fallback");
            htmlToCrawlLinksFrom = br.getRequest().getHtmlCode();
        }
        final String[] links = HTMLParser.getHttpLinks(htmlToCrawlLinksFrom, br.getURL());
        for (final String singleLink : links) {
            if (!this.canHandle(singleLink)) {
                ret.add(createDownloadlink(singleLink));
            }
        }
        /* Look for Click and Load Forms - those can contain additional mirrors. */
        final Form[] forms = br.getForms();
        if (forms.length > 0) {
            final Browser brc = br.cloneBrowser();
            for (final Form form : forms) {
                if (form.getAction() != null && form.getAction().matches(".*127\\.0\\.0\\.1:\\d+.*")) {
                    final InputField jk = form.getInputFieldByName("jk");
                    final InputField crypted = form.getInputFieldByName("crypted");
                    if (jk != null && StringUtils.isNotEmpty(jk.getValue()) && crypted != null && StringUtils.isNotEmpty(crypted.getValue())) {
                        final DownloadLink dummyCnl = DummyCNL.createDummyCNL(URLDecoder.decode(crypted.getValue(), "UTF-8"), URLDecoder.decode(jk.getValue(), "UTF-8"), null, param.getCryptedUrl());
                        ret.add(dummyCnl);
                    } else {
                        brc.submitForm(form);
                    }
                }
            }
        }
        if (title != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(title).trim());
            fp.setAllowInheritance(Boolean.TRUE);
            fp.addLinks(ret);
        }
        if (ret.isEmpty()) {
            logger.info("Failed to find any results");
            return ret;
        }
        return ret;
    }
}