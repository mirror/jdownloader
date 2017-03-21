package jd.plugins.hoster;

import java.io.IOException;
import java.net.URL;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.plugins.config.BasicAdvancedConfigPluginPanel;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dropbox.com" }, urls = { "https?://(?:www\\.)?(dl\\-web\\.dropbox\\.com/get/.*?w=[0-9a-f]+|([\\w]+:[\\w]+@)?api\\-content\\.dropbox\\.com/\\d+/files/.+|dropboxdecrypted\\.com/.+)" })
public class DropboxCom extends PluginForHost {

    public DropboxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.dropbox.com/pricing");

    }

    public void correctDownloadLink(DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("dropboxdecrypted.com/", "dropbox.com/").replaceAll("#", "%23").replaceAll("\\?dl=\\d", ""));

    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return DropboxConfig.class;
    }

    public interface DropboxConfig extends PluginConfigInterface {
        @DefaultBooleanValue(false)
        @AboutConfig
        @DescriptionForConfigEntry("If enabled, the Linkgrabber will offer a zip archive to download folders")
        boolean isZipFolderDownloadEnabled();

        void setZipFolderDownloadEnabled(boolean b);
    }

    private static final String             TYPE_S                                           = "https?://(www\\.)?dropbox\\.com/s/.+";
    private static Object                   LOCK                                             = new Object();
    private static HashMap<String, Cookies> accountMap                                       = new HashMap<String, Cookies>();

    private boolean                         passwordProtected                                = false;
    private String                          url                                              = null;
    private boolean                         temp_unavailable_file_generates_too_much_traffic = false;
    private BasicAdvancedConfigPluginPanel  configPanel;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        url = null;
        temp_unavailable_file_generates_too_much_traffic = false;
        br = new Browser();
        br.setAllowedResponseCodes(new int[] { 429 });
        if (link.getBooleanProperty("decrypted", false)) {
            URLConnectionAdapter con = null;
            if (link.getPluginPatternMatcher().matches(TYPE_S)) {
                br.setCookie("http://dropbox.com", "locale", "en");
                url = link.getPluginPatternMatcher().replace("https://", "https://dl.");
                for (int i = 0; i < 2; i++) {
                    try {
                        br.setFollowRedirects(true);
                        con = i == 0 ? br.openHeadConnection(url) : br.openGetConnection(url);
                        if (!con.getContentType().contains("html")) {
                            link.setProperty("directlink", con.getURL().toString());
                            link.setDownloadSize(con.getLongContentLength());
                            String name = Encoding.htmlDecode(getFileNameFromHeader(con).trim());
                            link.setFinalFileName(name);
                            return AvailableStatus.TRUE;
                        }
                    } finally {
                        try {
                            con.disconnect();
                        } catch (Throwable e) {
                        }
                    }
                }
                url = link.getPluginPatternMatcher();
                /* Either offline or password protected */
                br.getPage(url);
                if (this.br.getHttpConnection().getResponseCode() == 429) {
                    /* 2017-01-30 */
                    temp_unavailable_file_generates_too_much_traffic = true;
                    return AvailableStatus.TRUE;
                }
                if (!this.br.getURL().contains("/password")) {
                    // NOT TRUE , https://svn.jdownloader.org/issues/81049
                    // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    passwordProtected = true;
                    return AvailableStatus.TRUE;
                }
            } else {
                url = link.getPluginPatternMatcher();
                br.setCookie("http://dropbox.com", "locale", "en");
                br.setFollowRedirects(true);
                for (int i = 0; i < 2; i++) {
                    try {
                        con = i == 0 ? br.openHeadConnection(url) : br.openGetConnection(url);
                        if (con.getResponseCode() == 403) {
                            link.getLinkStatus().setStatusText("Forbidden 403");
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        if (con.getResponseCode() == 509) {
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
                        }
                        if (!con.getContentType().contains("html")) {

                            link.setDownloadSize(con.getLongContentLength());
                            String name = Encoding.htmlDecode(getFileNameFromHeader(con).trim());
                            link.setFinalFileName(name);
                            return AvailableStatus.TRUE;
                        }
                        if (i != 0) {
                            br.followConnection();
                            break;
                        }
                    } finally {
                        try {
                            con.disconnect();
                        } catch (Throwable e) {
                        }
                    }
                }
            }

            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(>Error \\(404\\)<|>Dropbox \\- 404<|>We can\\'t find the page you\\'re looking for|>The file you're looking for has been)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.br.containsHTML("images/sharing/error_")) {
                /* Offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.br.containsHTML("/images/precaution")) {
                /* A previously public shared url is now private (== offline) */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            final String json_source = jd.plugins.decrypter.DropBoxCom.getJsonSource(this.br);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
            entries = (LinkedHashMap<String, Object>) jd.plugins.decrypter.DropBoxCom.getFilesList(entries).get(0);
            final String filename = (String) entries.get("filename");

            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("bytes"), 0);
            if ("zip".equals(link.getProperty("type"))) {
                link.setFinalFileName("Folder " + Encoding.htmlDecode(filename) + ".zip");
            }

            if (filesize > 0) {
                link.setDownloadSize(filesize);
            }
            link.setName(filename);
            if (!this.br.getURL().matches(".+/s/[^/]+/[^/]+")) {
                url = this.br.getURL();
                if (!url.endsWith("/") && !Encoding.htmlDecode(url).contains(filename)) {
                    url += "/";
                    url += Encoding.urlEncode_light(filename);
                }
                url += "?dl=1";
            }
            return AvailableStatus.TRUE;
        } else {
            return AvailableStatus.UNCHECKABLE;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        br.setDebug(true);
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setStatus("Registered (free) user");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    protected String generateNonce() {
        return Long.toString(new Random().nextLong());
    }

    protected String generateTimestamp() {
        return Long.toString(System.currentTimeMillis() / 1000L);
    }

    @Override
    public String getAGBLink() {
        return "https://www.dropbox.com/terms";
    }

    // TODO: Move into Utilities (It's here for a hack)
    // public class OAuth {

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        String passCode = link.getStringProperty("pass", null);

        String t1 = new Regex(link.getPluginPatternMatcher(), "://(.*?):.*?@").getMatch(0);
        String t2 = new Regex(link.getPluginPatternMatcher(), "://.*?:(.*?)@").getMatch(0);
        if (t1 != null && t2 != null) {
            handlePremium(link, null);
            return;
        } else if (link.getBooleanProperty("decrypted")) {
            requestFileInformation(link);
            if (temp_unavailable_file_generates_too_much_traffic) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error 429: 'This account's links are generating too much traffic and have been temporarily disabled!'", 60 * 60 * 1000l);
            }
            if (this.passwordProtected) {
                final Form pwform = this.br.getFormbyProperty("id", "password-form");
                if (pwform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                pwform.setAction("https://www.dropbox.com/sm/auth");
                if (passCode == null) {
                    passCode = getUserInput("Password?", link);
                }
                pwform.put("t", br.getCookie(getHost(), "t"));
                pwform.put("password", passCode);
                this.br.submitForm(pwform);
                if (this.br.getURL().contains("/password") || PluginJSonUtils.getJsonValue(br, "error") != null) {
                    link.setProperty("pass", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                }
                this.br.getPage(link.getPluginPatternMatcher());
                link.setProperty("pass", passCode);
                url = br.getURL("?dl=1").toString();
            } else {
                if (url == null) {
                    url = URLHelper.parseLocation(new URL(link.getPluginPatternMatcher()), "?dl=1");
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, false, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("Directlink leads to HTML code...");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_FATAL, "You can only download files from your own account!");
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String dlURL = downloadLink.getPluginPatternMatcher();
        boolean resume = true;
        if (dlURL.matches(".*api-content.dropbox.com.*")) {
            /* api downloads via tokens */
            resume = false;
            try {
                /* Decrypt oauth token and secret */
                byte[] crypted_oauth_consumer_key = org.appwork.utils.encoding.Base64.decode("1lbl8Ts5lNJPxMOBzazwlg==");
                byte[] crypted_oauth_consumer_secret = org.appwork.utils.encoding.Base64.decode("cqqyvFx1IVKNPennzVKUnw==");
                byte[] iv = new byte[] { (byte) 0xF0, 0x0B, (byte) 0xAA, (byte) 0x69, 0x42, (byte) 0xF0, 0x0B, (byte) 0xAA };
                byte[] secretKey = (new Regex(dlURL, "passphrase=([^&]+)").getMatch(0).substring(0, 8)).getBytes("UTF-8");

                SecretKey key = new SecretKeySpec(secretKey, "DES");
                AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
                Cipher dcipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
                dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
                String oauth_consumer_key = new String(dcipher.doFinal(crypted_oauth_consumer_key), "UTF-8");
                String oauth_token_secret = new String(dcipher.doFinal(crypted_oauth_consumer_secret), "UTF-8");

                /* remove existing tokens from url */
                dlURL = dlURL.replaceFirst("://[\\w:]+@", "://");
                /* remove passphrase from url */
                dlURL = dlURL.replaceFirst("[\\?&]passphrase=[^&]+", "");
                String t1 = new Regex(downloadLink.getPluginPatternMatcher(), "://(.*?):.*?@").getMatch(0);
                String t2 = new Regex(downloadLink.getPluginPatternMatcher(), "://.*?:(.*?)@").getMatch(0);
                if (t1 == null) {
                    t1 = account.getUser();
                }
                if (t2 == null) {
                    t2 = account.getPass();
                }
                dlURL = signOAuthURL(dlURL, oauth_consumer_key, oauth_token_secret, t1, t2);
            } catch (PluginException e) {
                throw e;
            } catch (Exception e) {
                logger.log(e);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            /* website downloads */
            login(account, false);
            if (!dlURL.contains("?dl=1") && !dlURL.contains("&dl=1")) {
                dlURL = dlURL + "&dl=1";
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlURL, resume, 1);
        final URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200) {
            con.disconnect();
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (con.getResponseCode() == 401) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /* is only used for website logins */
    private void login(final Account account, boolean refresh) throws IOException, PluginException {
        boolean ok = false;
        synchronized (LOCK) {
            setBrowserExclusive();
            br.setFollowRedirects(true);
            this.br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");
            // this.br.setCookie("dropbox.com", "puc", "");
            this.br.setCookie("dropbox.com", "goregular", "");
            if (refresh == false) {
                Cookies accCookies = accountMap.get(account.getUser());
                if (accCookies != null) {
                    br.getCookies("https://www.dropbox.com").add(accCookies);
                    return;
                }
            }
            try {
                br.getPage("https://www.dropbox.com/login");
                final String lang = System.getProperty("user.language");
                String t = br.getRegex("type=\"hidden\" name=\"t\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (t == null) {
                    t = this.br.getCookie("dropbox.com", "t");
                }
                if (t == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Accept", "text/plain, */*; q=0.01");
                br.getHeaders().put("Accept-Language", "en-US;q=0.7,en;q=0.3");
                br.postPage("/needs_captcha", "is_xhr=true&t=" + t + "&email=" + Encoding.urlEncode(account.getUser()));
                br.postPage("/sso_state", "is_xhr=true&t=" + t + "&email=" + Encoding.urlEncode(account.getUser()));
                String postdata = "is_xhr=true&t=" + t + "&cont=%2F&require_role=&signup_data=&third_party_auth_experiment=CONTROL&signup_tag=&login_email=" + Encoding.urlEncode(account.getUser()) + "&login_password=" + Encoding.urlEncode(account.getPass()) + "&remember_me=True";
                postdata += "&login_sd=";
                postdata += "";
                br.postPage("/ajax_login", postdata);
                if (br.getCookie("https://www.dropbox.com", "jar") == null || !"OK".equals(PluginJSonUtils.getJsonValue(br, "status"))) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                ok = true;
            } finally {
                if (ok) {
                    accountMap.put(account.getUser(), br.getCookies("https://www.dropbox.com"));
                } else {
                    accountMap.remove(account.getUser());
                }
            }
        }

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /**
     * Sign an OAuth GET request with HMAC-SHA1 according to OAuth Core spec 1.0
     *
     * @return new url including signature
     * @throws PluginException
     */
    public/* static */String signOAuthURL(String url, String oauth_consumer_key, String oauth_consumer_secret, String oauth_token, String oauth_token_secret) throws PluginException {
        // At first, we remove all OAuth parameters from the url. We add
        // them
        // all manually.
        url = url.replaceAll("[\\?&]oauth_\\w+?=[^&]+", "");
        url += (url.contains("?") ? "&" : "?") + "oauth_consumer_key=" + oauth_consumer_key;
        url += "&oauth_nonce=" + generateNonce();

        url += "&oauth_signature_method=HMAC-SHA1";
        url += "&oauth_timestamp=" + generateTimestamp();
        url += "&oauth_token=" + oauth_token;
        url += "&oauth_version=1.0";

        String signatureBaseString = Encoding.urlEncode(url);
        signatureBaseString = signatureBaseString.replaceFirst("%3F", "&");
        // See OAuth 1.0 spec Appendix A.5.1
        signatureBaseString = "GET&" + signatureBaseString;

        String keyString = oauth_consumer_secret + "&" + oauth_token_secret;
        String signature = "";
        try {
            Mac mac = Mac.getInstance("HmacSHA1");

            SecretKeySpec secret = new SecretKeySpec(keyString.getBytes("UTF-8"), "HmacSHA1");
            mac.init(secret);
            byte[] digest = mac.doFinal(signatureBaseString.getBytes("UTF-8"));
            signature = new String(org.appwork.utils.encoding.Base64.encodeToString(digest, false)).trim();

        } catch (Exception e) {
            logger.log(e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        url += "&oauth_signature=" + Encoding.urlEncode(signature);
        return url;
    }

}