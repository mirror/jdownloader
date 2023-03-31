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
import java.util.List;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MirroraceCom extends antiDDoSForDecrypt {
    public MirroraceCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mirrorace.com", "mirrorace.org" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/m/[A-Za-z0-9]+");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*Links Unavailable\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        {
            /* 2020-04-14: New */
            final Form preReCaptchaForm = br.getFormbyProperty("id", "protection");
            if (preReCaptchaForm != null) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                preReCaptchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                this.submitForm(preReCaptchaForm);
            }
        }
        final String fpName = br.getRegex("<title>\\s*(?:Download)?\\s*([^<]*?)\\s*(?:-\\s*MirrorAce)?\\s*</title>").getMatch(0);
        // since mirrorace is single file response > many mirrors, results in package of compression parts all have there own package...
        // which then results in extraction tasks failing...
        final FilePackage fp = fpName != null && false ? FilePackage.getInstance() : null;
        if (fp != null) {
            fp.setName(Encoding.htmlDecode(fpName.trim()));
        }
        /* 2019-11-26: They are possibly changing their website against us! */
        final String[] links = br.getRegex("\"(https?://mirrorace\\.(?:com|org)/m/[A-Za-z0-9]+/\\d+\\?t=[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<String> domains = new ArrayList<String>();
        for (final String[] dmns : getPluginDomains()) {
            for (final String dmn : dmns) {
                domains.add(dmn);
            }
        }
        int prgress = 0;
        for (final String singleLink : links) {
            prgress++;
            logger.info("Crawling item " + prgress + "/" + links.length);
            final Browser br = this.br.cloneBrowser();
            getPage(br, singleLink);
            {
                // sometimes a captcha event can happen here
                final Form captchaForm = br.getFormByRegex("class=\"g-recaptcha\"");
                if (captchaForm != null) {
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                    captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    submitForm(br, captchaForm);
                }
            }
            final String finallink = br.getRegex("<a class=\"uk-button[^\"]*\"\\s*href=\"(https?[^<>\"]+\\&k=[^<>\"]+)\"").getMatch(0);
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink dl;
            final String domain = Browser.getHost(finallink);
            if (domains.contains(domain)) {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(false);
                brc.getPage(finallink);
                if (brc.getRedirectLocation() != null) {
                    dl = createDownloadlink(brc.getRedirectLocation());
                } else {
                    continue;
                }
            } else {
                dl = createDownloadlink(finallink);
            }
            ret.add(dl);
            if (fp != null) {
                fp.add(dl);
            }
            distribute(dl);
            if (this.isAbort()) {
                return ret;
            }
        }
        return ret;
    }
}
