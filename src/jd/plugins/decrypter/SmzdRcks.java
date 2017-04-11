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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "smoozed.rocks" }, urls = { "https?://(www\\.)?smoozed\\.rocks/(folder/[A-Za-z0-9\\-_]+|redirect/[A-Za-z0-9\\-_]+\\?_link=[A-Za-z0-9\\-_]+)" }) 
public class SmzdRcks extends antiDDoSForDecrypt {

    private static WeakHashMap<Account, Map<String, Object>> ACCOUNTINFOS   = new WeakHashMap<Account, Map<String, Object>>();
    private ArrayList<DownloadLink>                          decryptedLinks = new ArrayList<DownloadLink>();
    private String                                           ssid;
    private String                                           smzd_id        = null;

    public SmzdRcks(PluginWrapper wrapper) {
        super(wrapper);
        // some kind of canvas browser id?
        // seems like a random number works pretty fine
        final long min = 1000000000l;
        final long max = 9999999999l;
        ssid = (min + (int) (Math.random() * ((max - min) + 1))) + "";
    }

    private String getProtocol() {
        if (Application.getJavaVersion() >= Application.JAVA17) {
            return "https://";
        } else {
            return "http://";
        }
    }

    @SuppressWarnings({ "deprecation", "unused" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String parameter = param.toString();
        parameter = parameter.replace("https://", getProtocol());
        boolean premiumActive = false;

        final PluginForHost smoozed_moch = JDUtilities.getPluginForHost("smoozed.com");
        final Account aa = AccountController.getInstance().getValidAccount(smoozed_moch);
        if (aa != null) {
            restoreAccountInfos(aa);
            /* Set smoozed.com 'sid' cookie - not necessarily required. */
            final Cookies cookies = aa.loadCookies("");
            if (cookies != null) {
                this.br.setCookies(smoozed_moch.getHost(), cookies);
            }
            final Map<String, Object> map;
            synchronized (ACCOUNTINFOS) {
                map = ACCOUNTINFOS.get(aa);
            }
            final String session_Key = get(aa, String.class, "data", "session_key");
            if (session_Key != null) {
                /*
                 * Do NOT trust this - it will usually remove the captcha BUT in case the users' account is a free ccount (case untested) or
                 * has other problems this might not skip the captcha!
                 */
                this.br.setCookie("www.smoozed.rocks", "smzd_id", session_Key);
                smzd_id = session_Key;
            }
        }

        getPage(parameter);

        String redirect = this.br.getRedirectLocation();
        if (redirect != null && redirect.contains("smoozed.rocks/404")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (redirect != null && parameter.contains("redirect/")) {
            decryptedLinks.add(createDownloadlink(redirect));
            return decryptedLinks;
        }

        if (br.containsHTML("\"download_free\"")) {
            // redirect url
            String base64 = br.getRegex("atob\\('([A-Za-z0-9\\-_]+)'\\)").getMatch(0);
            redirect = Encoding.Base64Decode(base64);
            if (StringUtils.isNotEmpty(redirect)) {
                decryptedLinks.add(createDownloadlink(redirect));
                return decryptedLinks;
            }
        }

        final String rcID = br.getRegex("challenge\\?k=([^\"]+)").getMatch(0);
        if (rcID != null) {
            // Form[] forms = br.getForms();
            final Recaptcha rc = new Recaptcha(br.cloneBrowser(), this);
            rc.setId(rcID);
            rc.load();
            String secretKey = null;
            Browser ajax = br.cloneBrowser();
            for (int i = 0; i <= 15; i++) {
                if (isAbort()) {
                    return decryptedLinks;
                }
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, param);
                if (c == null || c.length() == 0) {
                    rc.reload();
                    continue;
                }

                final Form rcForm = br.getFormbyKey("recaptcha_response_field");
                rcForm.getInputField("ssid").setValue(ssid);
                rcForm.getInputField("recaptcha_response_field").setValue(c);

                rcForm.addInputField(new InputField("recaptcha_challenge_field", rc.getChallenge()));
                rcForm.addInputField(new InputField("mode", "free"));
                rcForm.setAction(parameter + "/access");
                Cookies cookies = br.getCookies(getProtocol() + "www.smoozed.rocks");

                cookies.add(new Cookie("smoozed.rocks", "sid", ssid));
                cookies.add(new Cookie("smoozed.rocks", "jid", ssid));

                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                // waittime is not evaluated
                // Thread.sleep(15000);
                submitForm(ajax, rcForm);
                secretKey = PluginJSonUtils.getJsonValue(ajax, "secretKey");
                if (StringUtils.isNotEmpty(secretKey)) {
                    break;
                } else {
                    rc.reload();
                }

            }
            if (StringUtils.isEmpty(secretKey)) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            getPage(parameter + "/" + secretKey);
        } else {
            /* Premium - no captcha */
            final String[] dlids = this.br.getRegex("onclick=\"window\\.open\\(\\'/download/([^/]+)/?\\'").getColumn(0);
            if (dlids == null || dlids.length == 0) {
                return null;
            }
            for (final String id : dlids) {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    return decryptedLinks;
                }
                final String link = getProtocol() + "www.smoozed.rocks" + "/download/" + id + "/" + this.smzd_id + "?direct=1";
                decryptSingleID(link);
            }
        }
        parse(br.getRequest().getHtmlCode());
        return decryptedLinks;
    }

    private void parse(final String htmlCode) throws Exception {

        final ArrayList<Object> obj = JSonStorage.restoreFromString(br.toString(), new TypeRef<ArrayList<Object>>() {
        });
        // String cnl = (String) obj.get(0);
        // String dlc = (String) obj.get(1);
        final String accessKey = (String) obj.get(3);

        final Map<String, List<List<String>>> mirrorMap = (Map<String, List<List<String>>>) obj.get(2);

        for (final Entry<String, List<List<String>>> es : mirrorMap.entrySet()) {
            for (final List<String> linkInfo : es.getValue()) {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    return;
                }
                // String status = linkInfo.get(0);
                // String name = linkInfo.get(1);
                // String name_short = linkInfo.get(2);
                // String size = linkInfo.get(3);
                final String id = linkInfo.get(4);
                final String link = getProtocol() + "www.smoozed.rocks/dl/" + id + "/" + accessKey + "?direct=1";
                decryptSingleID(link);
            }

        }
    }

    private void decryptSingleID(final String link) throws Exception {
        final Browser clone = br.cloneBrowser();
        clone.setFollowRedirects(false);
        getPage(clone, link);
        final String redirect = clone.getRedirectLocation();
        if (StringUtils.isNotEmpty(redirect)) {
            decryptedLinks.add(createDownloadlink(redirect));
        }
    }

    private void restoreAccountInfos(final Account account) {
        synchronized (ACCOUNTINFOS) {
            if (!ACCOUNTINFOS.containsKey(account)) {
                final String responseString = account.getStringProperty(jd.plugins.hoster.SmoozedCom.PROPERTY_ACCOUNTINFO, null);
                if (StringUtils.isNotEmpty(responseString)) {
                    try {
                        if (StringUtils.equals(Hash.getSHA256((account.getUser() + account.getPass()).toLowerCase(Locale.ENGLISH)), account.getStringProperty(jd.plugins.hoster.SmoozedCom.PROPERTY_ACCOUNTHASH, null))) {
                            final HashMap<String, Object> responseMap = JSonStorage.restoreFromString(responseString, new TypeRef<HashMap<String, Object>>() {
                            }, null);
                            if (responseMap != null && responseMap.size() > 0) {
                                ACCOUNTINFOS.put(account, responseMap);
                                return;
                            }
                        }
                    } catch (final Throwable e) {
                        LogSource.exception(logger, e);
                    }
                    account.removeProperty(jd.plugins.hoster.SmoozedCom.PROPERTY_ACCOUNTHASH);
                    account.removeProperty(jd.plugins.hoster.SmoozedCom.PROPERTY_ACCOUNTINFO);
                }
            }
        }
    }

    private <T> T get(Account account, Class<T> type, String... keyPath) {
        final Map<String, Object> map;
        synchronized (ACCOUNTINFOS) {
            map = ACCOUNTINFOS.get(account);
        }
        return get(map, type, keyPath);
    }

    private <T> T get(Map<String, Object> map, Class<T> type, String... keyPath) {
        final Object ret = jd.plugins.hoster.SmoozedCom.get(map, keyPath);
        if (ret != null && type != null && type.isAssignableFrom(ret.getClass())) {
            return (T) ret;
        }
        return null;
    }

}
