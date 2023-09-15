//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CnnT extends PluginForDecrypt {
    public CnnT(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "canna-power.to", "canna.to" });
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
            ret.add("https?://(?:www\\.|[a-z]{2}\\.)?" + buildHostsPatternPart(domains) + "/(?:cpuser/)?links\\.php\\?action=[^<>\"/\\&]+\\&kat_id=\\d+\\&fileid=\\d+");
        }
        return ret.toArray(new String[0]);
    }

    public static Object LOCK = new Object();

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String host = new Regex(param.getCryptedUrl(), "https?://([^/]+)/").getMatch(0);
        final String kat_id = new Regex(param.getCryptedUrl(), "kat_id=(\\d+)").getMatch(0);
        final String fid = new Regex(param.getCryptedUrl(), "fileid=(\\d+)").getMatch(0);
        final String contentURL = "https://" + host + "/links.php?action=popup&kat_id=" + kat_id + "&fileid=" + fid;
        boolean valid = false;
        br.setFollowRedirects(true);
        br.getPage(contentURL);
        if (br.containsHTML("(?i)>Versuche es in wenigen Minuten nochmals")) {
            logger.info("Website overloaded at the moment: " + param.getCryptedUrl());
            throw new DecrypterRetryException(RetryReason.HOST);
        }
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)Es existiert kein (Eintrag|Upload) zu dieser ID")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final int maxAttempts = 5;
        synchronized (LOCK) {
            for (int retrycounter = 1; retrycounter <= maxAttempts; retrycounter++) {
                final Form[] allforms = br.getForms();
                Form captchaForm = br.getFormbyProperty("name", "download_form");
                if (captchaForm == null) {
                    captchaForm = allforms[allforms.length - 1];
                }
                final String captchaUrlPart = br.getRegex("\"(securimage_show\\.php\\?sid=[a-z0-9]+)\"").getMatch(0);
                if (captchaUrlPart == null || captchaForm == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String captchaurl;
                if (this.br.getURL().contains("/cpuser/")) {
                    captchaForm.setAction("/cpuser/links.php?action=load&fileid=" + fid);
                    captchaurl = "/cpuser/" + captchaUrlPart;
                } else {
                    captchaForm.setAction("/links.php?action=load&fileid=" + fid);
                    captchaurl = "/" + captchaUrlPart;
                }
                final String captchaCode = getCaptchaCode(captchaurl, param);
                captchaForm.put("cp_captcha", captchaCode);
                br.submitForm(captchaForm);
                if (br.containsHTML("(?u)Der Sicherheitscode ist falsch")) {
                    /* Falscher Captcha, Seite neu laden */
                    br.getPage(contentURL);
                } else {
                    valid = true;
                    String finallink = br.getRegex("URL=(.*?)\"").getMatch(0);
                    if (finallink != null) {
                        ret.add(createDownloadlink(finallink));
                    }
                    String links[] = br.getRegex("<a target=\"_blank\" href=\"(.*?)\">").getColumn(0);
                    if (links != null && links.length != 0) {
                        for (String link : links) {
                            ret.add(createDownloadlink(link));
                        }
                    }
                    break;
                }
                if (this.isAbort()) {
                    /* Aborted by user */
                    return ret;
                }
            }
        }
        if (!valid) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        return ret;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return true;
    }
}