//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

/**
 *
 * after writing this... found out same as yep.pm (now dead)<br/>
 * Name: yep.pm <br/>
 * Addresses: 213.184.127.151<br/>
 * 108.61.185.218<br/>
 * 190.97.163.134<br/>
 * <br/>
 * Name: click.tf<br/>
 * Addresses: 190.97.163.134<br/>
 * 213.184.127.151<br/>
 * 108.61.185.218<br/>
 * <br/>
 * same server!!!<br/>
 *
 * I've created jac for this under this name.
 *
 * ssh.tf<br/>
 * Name: ssh.tf<br/>
 * Addresses: 103.39.133.244<br/>
 * 103.237.33.180<br/>
 * 111.221.47.171<br/>
 *
 * http://ssh.tf/services.html
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ClkTf extends PluginForDecrypt {

    // add new domains here.
    private static final String[] domains = { "click.tf", "ssh.tf", "yep.pm", "adlink.wf", "bily.ga", "bily.ml", "fave.cf", "fave.ga", "fave.gq", "iapp.ga", "icom.ga", "icom.ml", "igov.cf", "igov.ga", "igov.ml", "ihec.cf", "ihec.ga", "ihec.ml", "ihec.tk", "ilol.cf", "ilol.ml", "iref.ga", "iref.ml", "iref.tk", "itao.cf", "itao.ga", "itao.ml", "item.ga", "itop.cf", "itop.ga", "itop.ml", "iurl.ml", "iusa.cf", "iusa.ga", "iusd.cf", "iusd.ga", "iusd.ml", "iusd.tk", "kyc.pm", "lan.wf", "led.wf", "mcaf.cf", "mcaf.ga", "mcaf.gq", "mcaf.ml", "mcaf.tk", "owly.cf", "owly.ml", "ssh.yt", "tass.ga", "tass.ml", "twit.cf", "yourls.ml" };

    public ClkTf(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // some links are delivered by redirects!!
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Invalid Link\\.")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String finallink = br.getRedirectLocation();
        if (finallink != null) {
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        }
        // there could be captcha
        String fpName = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        if (fpName != null) {
            // remove /
            fpName = fpName.substring(1);
        }
        handleCaptcha(param);
        addLinks(decryptedLinks, parameter);

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void addLinks(final ArrayList<DownloadLink> decryptedLinks, final String parameter) throws PluginException {
        // weird they show it in another form final action!
        final Form f = br.getForm(0);
        if (f == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String link = f.getAction();
        if (link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        decryptedLinks.add(createDownloadlink(link));
    }

    private void handleCaptcha(final CryptedLink param) throws Exception {
        final int retry = 4;
        Form captcha = br.getForm(0);
        for (int i = 1; i < retry; i++) {
            if (captcha == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String captchaImage = captcha.getRegex("/captcha\\.php\\?cap_id=\\d+").getMatch(-1);
            if (captchaImage != null) {
                final String c = getCaptchaCode(captchaImage, param);
                if (c == null) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                captcha.put("ent_code", Encoding.urlEncode(c));
            }
            br.submitForm(captcha);
            if (br.containsHTML("<p style='color:\\s*red;'>Wrong CAPTCHA</p>")) {
                if (i + 1 > retry) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                } else {
                    captcha = br.getForm(0);
                    continue;
                }
            }
            break;
        }
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    public boolean hasAutoCaptcha() {
        return true;
    }

    private static AtomicReference<String> HOSTS           = new AtomicReference<String>(null);
    private static AtomicLong              HOSTS_REFERENCE = new AtomicLong(-1);

    @Override
    public void init() {
        // first run -1 && revision change == sync.
        if (this.getVersion() > HOSTS_REFERENCE.get()) {
            HOSTS.set(getHostsPattern());
            HOSTS_REFERENCE.set(this.getVersion());
        }
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "click.tf" };
    }

    /**
     * returns the annotation pattern array
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/[a-zA-Z0-9]{8,}(/.+)?" };
    }

    private static String getHostsPattern() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        final String hosts = "https?://(www\\.)?" + "(?:" + pattern.toString() + ")";
        return hosts;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.URLShortnerLLP_URLShortner;
    }

}