package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Arrays;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 41244 $", interfaceVersion = 3, names = { "warez-world.org", "ddl-mdh.org", "funxd.tv" }, urls = { "https?://(?:www\\.)?warez-world\\.org/(?:download|link)/.+", "https?://(?:www\\.)?ddl-mdh\\.org/(?:download|link)/.+", "https?://(?:www\\.)?funxd\\.tv/(?:download|link)/.+" })
public class GenericWBoardDecrypter extends antiDDoSForDecrypt {
    public GenericWBoardDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (!canHandle(br.getURL())) {
            // invalid/offline link, redirect to main domain
            return ret;
        }
        if (StringUtils.containsIgnoreCase(parameter, "/link/")) {
            final String[] fileIDs = new Regex(parameter, "/link/(\\d+)/(\\d+)").getRow(0);
            if (fileIDs == null || fileIDs.length != 2) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (br.containsHTML("grecaptcha")) {
                final Form captcha = br.getForm(0);
                final String sitekey = br.getRegex("sitekey\\s*:\\s*\"([^\"]+)\"").getMatch(0);
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, sitekey) {
                    @Override
                    public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractCaptchaHelperRecaptchaV2.TYPE getType() {
                        return TYPE.INVISIBLE;
                    };
                }.getToken();
                captcha.put("original", "");
                captcha.put("q", fileIDs[0]);
                captcha.put("sq", fileIDs[1]);
                captcha.put("tk", Encoding.urlEncode(recaptchaV2Response));
                submitForm(captcha);
                if (canHandle(br.getURL())) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    final DownloadLink downloadLink = createDownloadlink(br.getURL());
                    ret.add(downloadLink);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            final String title = br.getRegex("<title>\\s*([^<]+)\\s*&(?:r|l)aquo;").getMatch(0);
            final String[] links = br.getRegex("href\\s*=\\s*\"(/link/([^\"]+))\"").getColumn(0);
            if (links == null || links.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String password = br.getRegex(">\\s*Passwort:\\s*</div>\\s*<div class=\"ui2\">\\s*([^<]+)\\s*</div>").getMatch(0);
            if (password == null) {
                password = br.getRegex("<span>\\s*Passwor(?:d|t):\\s*</span>\\s*(.*?)\\s*<").getMatch(0);
            }
            for (final String link : links) {
                final String url = br.getURL(Encoding.htmlDecode(link)).toString();
                final DownloadLink downloadLink = createDownloadlink(url);
                if (StringUtils.isNotEmpty(password) && !StringUtils.equalsIgnoreCase(password, "Kein Passwort")) {
                    downloadLink.setSourcePluginPasswordList(new ArrayList<String>(Arrays.asList(password)));
                }
                ret.add(downloadLink);
            }
            if (title != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(title.trim()));
                fp.setProperty("ALLOW_MERGE", true);
                fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
                fp.addLinks(ret);
            }
        }
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
