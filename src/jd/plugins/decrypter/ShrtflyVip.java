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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "shrtfly.com" }, urls = { "https?://(?:www\\.)?(?:shrtfly\\.vip|shrtfly\\.com|shrtvip\\.com|stfly\\.me|smwebs\\.xyz)/([A-Za-z0-9]+)" })
public class ShrtflyVip extends MightyScriptAdLinkFly {
    public ShrtflyVip(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected ArrayList<DownloadLink> handlePreCrawlProcess(final CryptedLink param) throws Exception {
        // final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        // final String parameter = param.toString();
        // br.setFollowRedirects(true);
        // /* Pre-setting Referer was an attempt to skip their captcha but this did not work. */
        // // br.getHeaders().put("Referer", "https://itsguider.com/");
        // getPage(parameter);
        // if (br.getHttpConnection().getResponseCode() == 404) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        // /* They call this "alias". */
        // // final String contentID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        // final Form form0 = br.getFormbyKey("alias");
        // submitForm(form0);
        // final Form form1 = br.getFormbyProperty("id", "myform");
        // /* This will redirect to an "external" fake blog website containing another form + captcha */
        // submitForm(form1);
        // final Form captchaForm = br.getFormbyProperty("id", "form");
        // if (captchaForm == null) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        // captchaForm.put("g-recaptcha-response=", Encoding.urlEncode(recaptchaV2Response));
        // submitForm(captchaForm);
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
        // /* Now continue with parent class code (requires 2nd captcha + waittime) */
        // return ret;
        /* 2022-09-13: Special handling not required anymore */
        return super.handlePreCrawlProcess(param);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        return super.decryptIt(param, progress);
    }
}
