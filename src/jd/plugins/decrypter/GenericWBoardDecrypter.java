package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Arrays;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "warez-world.org", "ddl-mdh.org" }, urls = { "https?://(?:www\\.)?warez-world\\.org/(?:download/[^/]+|link/\\d+/\\d+)", "https?://(?:www\\.)?(?:ddl-mdh\\.org|mdh\\.to)/(?:download/[^/]+|video/[^/]+|link/\\d+/\\d+)" })
public class GenericWBoardDecrypter extends antiDDoSForDecrypt {
    public GenericWBoardDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (!canHandle(br.getURL())) {
            // invalid/offline link, redirect to main domain
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (StringUtils.containsIgnoreCase(parameter, "/link/")) {
            final String[] fileIDs = new Regex(parameter, "/link/(\\d+)/(\\d+)").getRow(0);
            if (fileIDs == null || fileIDs.length != 2) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Form captchaform = br.getForm(0);
            final String sitekey = br.getRegex("sitekey\\s*:\\s*\"([^\"]+)\"").getMatch(0);
            if (captchaform == null || sitekey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, sitekey) {
                @Override
                public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                    return TYPE.INVISIBLE;
                };
            }.getToken();
            captchaform.put("original", "");
            captchaform.put("q", fileIDs[0]);
            captchaform.put("sq", fileIDs[1]);
            captchaform.put("tk", Encoding.urlEncode(recaptchaV2Response));
            br.setFollowRedirects(false);
            submitForm(captchaform);
            final String finallink = br.getRedirectLocation();
            if (finallink == null || canHandle(finallink)) {
                /* Redirect to same link */
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            final DownloadLink downloadLink = createDownloadlink(finallink);
            ret.add(downloadLink);
        } else {
            final String title = br.getRegex("<title>\\s*([^<]+)").getMatch(0);
            final String[] urls = br.getRegex("href\\s*=\\s*\"(/link/([^\"]+))\"").getColumn(0);
            if (urls == null || urls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String password = br.getRegex(">\\s*Passwort:\\s*</div>\\s*<div class=\"ui2\">\\s*([^<]+)\\s*</div>").getMatch(0);
            if (password == null) {
                password = br.getRegex("<span>\\s*Passwor(?:d|t):\\s*</span>\\s*(.*?)\\s*<").getMatch(0);
            }
            for (String url : urls) {
                url = br.getURL(Encoding.htmlDecode(url)).toExternalForm();
                final DownloadLink dlink = createDownloadlink(url);
                if (StringUtils.isNotEmpty(password) && !StringUtils.equalsIgnoreCase(password, "Kein Passwort")) {
                    dlink.setSourcePluginPasswordList(new ArrayList<String>(Arrays.asList(password)));
                }
                ret.add(dlink);
            }
            if (title != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(title).trim());
                fp.setAllowMerge(true);
                fp.setAllowInheritance(true);
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
