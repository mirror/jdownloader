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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ShrtflyVip extends MightyScriptAdLinkFly {
    public ShrtflyVip(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "shrtfly.com", "shrtfly.vip", "shrtvip.com", "stfly.me", "stfly.xyz", "smwebs.xyz" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected ArrayList<DownloadLink> handlePreCrawlProcess(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.setFollowRedirects(true);
        /* Pre-setting Referer was an attempt to skip their captcha but this did not work. */
        // br.getHeaders().put("Referer", "https://itsguider.com/");
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* They call this "alias". */
        // final String contentID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final Form form0 = br.getFormbyKey("alias");
        submitForm(form0);
        br.getHeaders().put("Origin", "https://" + br.getHost());
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        /* 2022-12-09 */
        int page = 1;
        Form nextForm = null;
        int totalWaitedSeconds = 0;
        do {
            logger.info("Working on page: " + page + " | Total waited seconds so far: " + totalWaitedSeconds);
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final String error = (String) entries.get("error");
            if (error != null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> data = (Map<String, Object>) entries.get("data");
            if (data == null) {
                logger.warning("Field 'data' is missing");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String finalResult = (String) data.get("final");
            if (!StringUtils.isEmpty(finalResult)) {
                ret.add(this.createDownloadlink(finalResult));
                return ret;
            }
            String next_page = (String) data.get("next_page");
            if (next_page == null && page == 1) {
                /* Special: First page can be "/null"! */
                next_page = "/null";
            }
            final String speed_token = (String) data.get("speed_token");
            if (StringUtils.isEmpty(next_page) || StringUtils.isEmpty(speed_token)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                return ret;
            } else if (page >= 8) {
                logger.warning("Preventing endless loop");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.postPage(next_page, "speed_token=" + Encoding.urlEncode(speed_token));
            nextForm = br.getFormbyProperty("id", "form");
            if (nextForm == null) {
                /* 2022-12-09 */
                nextForm = br.getFormbyProperty("id", "init_next_up");
            }
            if (nextForm == null) {
                logger.info("Stopping because: Failed to find nextForm");
                break;
            }
            if (CaptchaHelperCrawlerPluginRecaptchaV2.containsRecaptchaV2Class(nextForm)) {
                /* Typically captcha is located on first page/first run. */
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                nextForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            /* Step "progressbar". This can happen multiple times. */
            final String waitSecondsStr = br.getRegex("var progress_original = (\\d{1,2});").getMatch(0);
            if (waitSecondsStr != null) {
                logger.info("Waiting seconds: " + waitSecondsStr);
                final int secs = Integer.parseInt(waitSecondsStr);
                totalWaitedSeconds += secs;
                this.sleep(secs * 1001l, param);
            }
            final String waitSecondsStr2 = br.getRegex("var countdown = (\\d{1,2});").getMatch(0);
            if (waitSecondsStr2 != null) {
                logger.info("Waiting seconds_2: " + waitSecondsStr2);
                final int secs = Integer.parseInt(waitSecondsStr2);
                totalWaitedSeconds += secs;
                this.sleep(secs * 1001l, param);
            }
            this.submitForm(nextForm);
            page++;
        } while (true);
        /* Old handling down below */
        // final String alias = br.getRegex("var alias\\s*=\\s*'([a-z0-9]+)';").getMatch(0);
        // final String token = br.getRegex("var token\\s*=\\s*'([a-f0-9]{32})';").getMatch(0);
        // final String continueURL = br.getRegex("<a href=\"(https?://[^\"]+)\" id=\"surl\"[^>]*>Generating Link").getMatch(0);
        // if (alias == null || token == null || continueURL == null) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // final Browser brc = br.cloneBrowser();
        // brc.getHeaders().put("x-Requested-With", "XMLHttpRequest");
        // brc.getPage("/shrtfly/verify.php?alias=" + alias + "&token=" + token);
        // if (!brc.toString().equalsIgnoreCase("true")) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // getPage(continueURL);
        // if (this.regexAppVars(this.br) == null) {
        // logger.warning("Possible crawler failure...");
        // }
        /* Now continue with parent class code (requires 2nd captcha + waittime) */
        return ret;
        /* 2022-09-13: Special handling not required anymore */
        /* 2022-11-23: Special handling is now required again */
        // return super.handlePreCrawlProcess(param);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        return super.decryptIt(param, progress);
    }
}
