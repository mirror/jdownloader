package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "onlyspanking.org" }, urls = { "https?://(\\w+\\.)?onlyspanking.org/\\d+-[a-zA-Z0-9\\-_]+\\.html" })
public class OnlySpankingOrg extends antiDDoSForDecrypt {
    // finallink will usually be an ubiqfile.com URL
    public OnlySpankingOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(parameter.getCryptedUrl().replace("http://", "https://"));
        final String dle_skin = br.getRegex("var\\s*dle_skin\\s*=\\s*'(.*?)'").getMatch(0);
        if (dle_skin == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (!br.containsHTML("<meta property\\s*=\\s*\"og:title\"")) {
            return ret;
        }
        Browser brc = br.cloneBrowser();
        getPage(brc, "/engine/ajax/getcap.php");
        Form form = brc.getForm(0);
        final String ajax_action = "https://onlyspanking.org/engine/ajax/getlink.php";
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            form.setAction(ajax_action);
        }
        final String code = getCaptchaCode(br.cloneBrowser(), getHost(), "https://onlyspanking.org/engine/modules/antibot/antibot.php?rndval=" + System.currentTimeMillis(), parameter);
        form.put("sec_code", code);
        form.put("skin", Encoding.urlEncode(dle_skin));
        /* Important!! */
        brc = br.cloneBrowser();
        submitForm(brc, form);
        form = brc.getForm(0);
        if (form != null && form.containsHTML("id\\s*=\\s*\"getlink\"") && form.containsHTML("data-sitekey")) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, "6Le7b3AUAAAAADGhizVG-ZB_jxfOha9WgXP-ahZd").getToken();
            form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            form.put("skin", Encoding.urlEncode(dle_skin));
            form.put("sec_code", Encoding.urlEncode(recaptchaV2Response));
            form.setAction(ajax_action);
            brc = br.cloneBrowser();
            submitForm(brc, form);
        }
        String finallink = null;
        final String redirect = brc.getRegex("(https?://(\\w+\\.)?onlyspanking\\.org/(video|file)/[a-zA-Z0-9]+)").getMatch(0);
        if (redirect == null) {
            if (brc.containsHTML("To access the exclusive category you need to purchase")) {
                /*
                 * Special case: Users who own a premium account of a specified OCH can auth themselves as premium here to get the
                 * downloadlinks!
                 */
                String ubiqfile_premium_mail = null;
                final ArrayList<Account> accs = AccountController.getInstance().getValidAccounts("ubiqfile.com");
                for (final Account acc : accs) {
                    final String accMailTmp = acc.getStringProperty("PROPERTY_UBIQFILE_MAIL", null);
                    if (acc.getType() == AccountType.PREMIUM && accMailTmp != null) {
                        ubiqfile_premium_mail = accMailTmp;
                        break;
                    }
                }
                if (ubiqfile_premium_mail == null) {
                    logger.info("Content is premiumonly and user does not own premium access");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                }
                logger.info("Content is premiumonly and user should have premium access via mail: " + ubiqfile_premium_mail);
                final Form premiumForm = new Form();
                premiumForm.setMethod(MethodType.POST);
                premiumForm.setAction(ajax_action);
                premiumForm.put("skin", Encoding.urlEncode(dle_skin));
                premiumForm.put("email", Encoding.urlEncode(ubiqfile_premium_mail));
                /* Important!! */
                brc = br.cloneBrowser();
                this.submitForm(brc, premiumForm);
                finallink = brc.getRegex("href=\"(https?[^\"]+)\"[^<>\"]*?target=\"_blank\" rel=\"external noopener\"").getMatch(0);
                if (finallink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        if (finallink == null) {
            brc = br.cloneBrowser();
            brc.setFollowRedirects(false);
            getPage(brc, redirect);
            finallink = brc.getRedirectLocation();
        }
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            ret.add(createDownloadlink(finallink));
            return ret;
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.AntiBotCMS;
    }
}
