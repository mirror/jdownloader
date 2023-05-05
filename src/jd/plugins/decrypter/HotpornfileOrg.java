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
import java.util.Map;
import java.util.Random;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hotpornfile.org" }, urls = { "https?://(?:www\\.)?hotpornfile\\.org/(?!page)([^/]+)/(\\d+)" })
public class HotpornfileOrg extends PluginForDecrypt {
    public HotpornfileOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private final String PROPERTY_recaptcha_response = "recaptcha_response";
    private final String PROPERTY_cid                = "cid";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.getPage(parameter);
        String fpName = br.getRegex("(?i)<title>\\s*(.*?)\\s*(\\s*-\\s*Hotpornfile\\s*)?</title>").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        }
        final String postID = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String lastRecaptchaV2Response = this.getPluginConfig().getStringProperty(PROPERTY_recaptcha_response);
        boolean allowReUseLastReCaptchaV2Response = true;
        boolean hasEverReUsedFreshReCaptchaV2Token = false;
        String freshReCaptchaV2Response = null;
        int attempt = 0;
        Map<String, Object> entries = null;
        String lastNext = null;
        String next = null;
        String links_text = null;
        String cid = this.getPluginConfig().getStringProperty(PROPERTY_cid);
        do {
            attempt++;
            final String recaptchaV2Response;
            if (lastRecaptchaV2Response != null && allowReUseLastReCaptchaV2Response) {
                logger.info("Trying to re-use last reCaptchaV2Response: " + lastRecaptchaV2Response);
                br.setCookie(this.getHost(), "cPass", lastRecaptchaV2Response);
                recaptchaV2Response = lastRecaptchaV2Response;
            } else if (freshReCaptchaV2Response != null) {
                /* E.g. first attempt: cid is null -> Obtain freshReCaptchaV2Response -> Fresh cid is needed -> Try again with same */
                if (hasEverReUsedFreshReCaptchaV2Token) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                logger.info("Trying to re-use freshReCaptchaV2Response created in this session reCaptchaV2Response: " + freshReCaptchaV2Response);
                recaptchaV2Response = freshReCaptchaV2Response;
                hasEverReUsedFreshReCaptchaV2Token = true;
            } else {
                br.getPage(parameter);
                recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, "6Lf1jhYUAAAAAN8kNxOBBEUu3qBPcy4UNu4roO5K").getToken();
                freshReCaptchaV2Response = recaptchaV2Response;
            }
            // br.setCookie(br.getHost(), "cAsh", cid);
            final UrlQuery query = new UrlQuery();
            query.add("action", "get_links");
            query.add("postId", postID);
            if (cid == null) {
                /* E.g. first request */
                query.add("cid", "null");
            } else {
                query.add("cid", cid);
                query.add("fb", "true");
            }
            query.add("challenge", Encoding.urlEncode(recaptchaV2Response));
            br.postPage("/wp-admin/admin-ajax.php", query);
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            lastNext = next;
            next = (String) entries.get("next");
            final Object error = entries.get("error");
            links_text = (String) entries.get("links");
            if ("links".equalsIgnoreCase(next) && links_text != null) {
                logger.info("Stopping because: next=links and links not empty. error=" + error);
                break;
            } else if (attempt > 10) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if ("ash".equalsIgnoreCase(next)) {
                /* [New] cid value is needed. */
                if (cid == null) {
                    logger.info("New [first] cid value is needed");
                } else {
                    logger.info("New [fresh] cid value is needed");
                }
                cid = generateCID();
                logger.info("Continue because: next=ash");
                continue;
            } else if ("challenge".equalsIgnoreCase(next)) {
                /* Fresh captcha needs to be solved. */
                logger.info("Continue because: lastNext=" + lastNext + "|next=challenge");
                allowReUseLastReCaptchaV2Response = false;
                continue;
            } else if (Boolean.FALSE.equals(error)) {
                logger.info("Stopping because error == FALSE | lastNext=" + lastNext + "|next=" + next + "|error=" + error + "|attempt=" + attempt);
                break;
            } else {
                logger.info("Stopping because attempt > 0 |  lastNext=" + lastNext + "|next=" + next + "|attempt=" + attempt);
                break;
            }
        } while (!isAbort());
        if (StringUtils.isEmpty(links_text)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] links = new Regex(links_text, "\"(https?://[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleLink : links) {
            ret.add(createDownloadlink(singleLink));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        if (ret.size() > 0 && freshReCaptchaV2Response != null) {
            /* Save reCaptchaV2 response and cid as we might be able to use it multiple times! */
            logger.info("Saving reCaptchaV2Response for next time: " + freshReCaptchaV2Response);
            this.getPluginConfig().setProperty(PROPERTY_recaptcha_response, freshReCaptchaV2Response);
            this.getPluginConfig().setProperty(PROPERTY_cid, cid);
        }
        return ret;
    }

    private static String generateCID() {
        // https://www.hotpornfile.org/wp-content/cache/autoptimize/js/autoptimize_192cd586b7da5c9f3b02b330c898f3cd.js
        // return generateRandomString("0123456789", 4) + "XXXX" + generateRandomString("0123456789abcdef", 16);
        return "1002XXXX" + generateRandomString("0123456789abcdef", 16);
    }

    public static String generateRandomString(final String chars, final int length) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(new Random().nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2023-03-27: Attempt to avoid captchas. */
        return 1;
    }
}
