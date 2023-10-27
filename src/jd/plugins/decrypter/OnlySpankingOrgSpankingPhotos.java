package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class OnlySpankingOrgSpankingPhotos extends antiDDoSForDecrypt {
    public OnlySpankingOrgSpankingPhotos(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "spanking.photos" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(\\d+-[a-zA-Z0-9\\-_]+\\.html|vfile/[a-z0-9]{12})");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        if (contenturl.matches("(?i)https?://[^/]+/vfile/[a-z0-9]{12}")) {
            br.setFollowRedirects(false);
            getPage(contenturl);
            final String redirect = br.getRedirectLocation();
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (redirect == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            ret.add(this.createDownloadlink(redirect));
        } else {
            br.setFollowRedirects(true);
            getPage(contenturl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String startURL = br.getURL();
            final String dle_skin = br.getRegex("var\\s*dle_skin\\s*=\\s*'(.*?)'").getMatch(0);
            if (dle_skin == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            boolean captchaSuccess = false;
            for (int i = 0; i <= 2; i++) {
                /* Important! */
                br.getHeaders().put("Referer", startURL);
                if (i > 0) {
                    getPage(startURL);
                }
                final Form captchaform = getCaptchaForm(br);
                if (captchaform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (captchaform.containsHTML("id\\s*=\\s*\"getlink\"") && captchaform.containsHTML("data-sitekey")) {
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, "6Le7b3AUAAAAADGhizVG-ZB_jxfOha9WgXP-ahZd").getToken();
                    captchaform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    captchaform.put("skin", Encoding.urlEncode(dle_skin));
                    captchaform.put("sec_code", Encoding.urlEncode(recaptchaV2Response));
                    /* Important! */
                    br.getHeaders().put("Referer", startURL);
                    if (StringUtils.isEmpty(captchaform.getAction())) {
                        /* E.g. spanking.photos */
                        captchaform.setAction("/engine/ajax/getlink.php");
                    }
                    submitForm(br, captchaform);
                    /* Do not allow retries for reCaptcha and assume it is solved correctly with one attempt. */
                    captchaSuccess = true;
                    break;
                } else {
                    /* Simple image captcha */
                    // captchaform.setAction(ajax_action);
                    final String code = getCaptchaCode(br, getHost(), "/engine/modules/antibot/antibot.php?rndval=" + System.currentTimeMillis(), param);
                    captchaform.put("sec_code", code);
                    captchaform.put("skin", Encoding.urlEncode(dle_skin));
                    /* Important! */
                    br.getHeaders().put("Referer", startURL);
                    if (StringUtils.isEmpty(captchaform.getAction()) && !br.getURL().contains("/engine/ajax")) {
                        /* E.g. spanking.photos */
                        captchaform.setAction("/engine/ajax/getlink.php");
                    }
                    submitForm(br, captchaform);
                }
                if (br.getRequest().getHtmlCode().length() == 0) {
                    /* Empty page = Wrong captcha */
                    logger.info("Wrong captcha | Run: " + i);
                    continue;
                } else {
                    captchaSuccess = true;
                    break;
                }
            }
            if (!captchaSuccess) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            String finallink = null;
            /* Maybe no account is needed -> Look for downloadurls */
            final String[] urls = findDownloadlinks(br);
            if (urls == null || urls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String url : urls) {
                ret.add(this.createDownloadlink(url));
            }
            if (ret.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ret.add(createDownloadlink(finallink));
        }
        return ret;
    }

    private String[] findDownloadlinks(final Browser br) {
        return br.getRegex("href=\"(https?[^\"]+)\"[^<>\"]*?target=\"_blank\" rel=\"external noopener\"").getColumn(0);
    }

    private Form getCaptchaForm(final Browser br) {
        Form form = br.getFormbyProperty("name", "getlink");
        if (form == null) {
            form = br.getFormbyProperty("id", "getlink");
        }
        if (form == null) {
            /* Look into escaped parts of html */
            final String hiddenformhtml = br.getRegex("document\\.write\\(\"(<form.*?)\"\\);").getMatch(0);
            if (hiddenformhtml != null) {
                final Browser brc = br.cloneBrowser();
                brc.getRequest().setHtmlCode(PluginJSonUtils.unescape(hiddenformhtml));
                form = brc.getFormbyProperty("id", "getlink");
            }
        }
        return form;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.AntiBotCMS;
    }
}
