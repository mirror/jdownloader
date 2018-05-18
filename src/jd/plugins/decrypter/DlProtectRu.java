package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 36737 $", interfaceVersion = 3, names = { "dl-protect.ru" }, urls = { "https?://(?:www\\.)?dl-protect\\.ru/other\\?id=[a-zA-Z0-9]+" })
public class DlProtectRu extends PluginForDecrypt {
    public DlProtectRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(parameter.getCryptedUrl());
        Form captcha = br.getFormByRegex("securimage");
        if (captcha != null) {
            final String img = captcha.getRegex("src\\s*=\\s*\"([^\"]+securimage_show.php)\"").getMatch(0);
            String code = getCaptchaCode(img, parameter);
            if (code == null) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            captcha.put("captcha_code", Encoding.urlEncode(code));
            br.submitForm(captcha);
            if (br.containsHTML("Le code n'est")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else if (br.containsHTML("Erreur")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final String href = br.getRegex("<div\\s*class\\s*=.*?<a\\s*href\\s*=\\s*\"(https?.*?)\"").getMatch(0);
        if (href != null) {
            ret.add(createDownloadlink(href));
        }
        return ret;
    }
}
