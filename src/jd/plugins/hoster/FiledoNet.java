package jd.plugins.hoster;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FiledoNet extends PluginForHost {
    public FiledoNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.API_KEY_LOGIN };
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filedo.net" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/d/([A-Fa-f0-9\\-]{32,}).*");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    public static final String API_BASE = "https://api.filedo.net";

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/tos";
    }

    public String getPurchasePremiumURL() {
        return "https://" + getHost() + "/premium";
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 0;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String fileId = this.getFID(link);
        if (!isValidFileID(fileId)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final UrlQuery query = UrlQuery.parse(link.getPluginPatternMatcher());
        String key = query.get("key");
        String counters = query.get("counters");
        String counterFileName = query.get("counterFileName");
        br.getPage(API_BASE + "/file?fileId=" + fileId);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fileName = PluginJSonUtils.getJson(br, "fileName");
        final String fileSize = PluginJSonUtils.getJson(br, "fileSize");
        final String fileHash = PluginJSonUtils.getJson(br, "fileHash");
        final String downloadUrl = PluginJSonUtils.getJson(br, "downloadUrl");
        final boolean hasCaptcha = PluginJSonUtils.parseBoolean(PluginJSonUtils.getJson(br, "hasCaptcha"));
        if (fileSize != null) {
            link.setVerifiedFileSize(Long.parseLong(fileSize) * 1024);
        }
        byte[] decodedFileNameIv = Base64.getDecoder().decode(URLDecoder.decode(counterFileName, "UTF-8"));
        byte[] decoded = Base64.getDecoder().decode(URLDecoder.decode(key, "UTF-8"));
        /* Set final filename here because Content-Disposition header contains crypted filename. */
        final String decryptedFileName = decryptFileName(fileName, decoded, decodedFileNameIv);
        link.setFinalFileName(decryptedFileName);
        link.setMD5Hash(fileHash);
        link.setProperty("dl3", downloadUrl);
        // link.setProperty("hashedFileName", fileName);
        link.setProperty("decryptedFileName", decryptedFileName);
        link.setProperty("key", key);
        link.setProperty("counters", counters);
        link.setProperty("counterFileName", counterFileName);
        // link.setProperty("filedo-file-id", fileId);
        link.setProperty("hasCatpcha", hasCaptcha);
        return AvailableStatus.TRUE;
    }

    private boolean isValidFileID(final String str) {
        if (str.replace("-", "").matches("[a-f0-9]{32}")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        String dlUrl = (String) link.getProperty("dl3");
        doFreePrivate(link, "free_directlink", dlUrl + "?secret=" + account.getPass());
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        requestFileInformation(link);
        String dlUrl = link.getStringProperty("dl3");
        doFreePrivate(link, "free_directlink", dlUrl);
    }

    private String decryptFileName(String encryptedFileName, byte[] keyData, byte[] iv) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyData, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedFileName));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private void decryptFile(String encryptedFilePath, String decryptedFilePath, byte[] keyData, ArrayList<byte[]> iv, String decryptedFileName) throws Exception {
        FileInputStream encryptedFileInputStream = new FileInputStream(encryptedFilePath);
        FileOutputStream decryptedFileOutputStream = new FileOutputStream(decryptedFilePath);
        SecretKey secretKey = new SecretKeySpec(keyData, "AES");
        for (int i = 0; i < iv.size(); i++) {
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.get(i));
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] buffer = new byte[10485760]; // 10mb
            int bytesRead;
            int count = 0;
            while ((bytesRead = encryptedFileInputStream.read(buffer)) != -1) {
                byte[] decryptedBytes = cipher.update(buffer, 0, bytesRead);
                decryptedFileOutputStream.write(decryptedBytes);
                if (count == 9) {
                    break;
                }
                count++;
            }
            byte[] finalDecryptedBytes = cipher.doFinal();
            decryptedFileOutputStream.write(finalDecryptedBytes);
        }
        encryptedFileInputStream.close();
        decryptedFileOutputStream.close();
    }

    private void doFreePrivate(final DownloadLink link, final String directlinkproperty, final String dlUrl) throws Exception, PluginException {
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dlUrl, this.isResumeable(link, null), this.getMaxChunks(link, null));
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Got unexpected non-file content");
        }
        if (dl.startDownload()) {
            /* Decrypt downloaded file if download was successful. */
            logger.info("Decrypting file...");
            String downloadPath = dl.getDownloadable().getFileOutput();
            String destinationPath = (new File(downloadPath)).getParent();
            destinationPath = destinationPath + "\\" + link.getStringProperty("decryptedFileName");
            String countersString = URLDecoder.decode(link.getStringProperty("counters"), "UTF-8");
            String[] test = countersString.split(",");
            ArrayList<byte[]> decodedIvs = new ArrayList<byte[]>();
            for (int i = 0; i < test.length; i++) {
                decodedIvs.add(Base64.getDecoder().decode(test[i]));
            }
            byte[] decoded = Base64.getDecoder().decode(URLDecoder.decode((String) link.getProperty("key"), "UTF-8"));
            decryptFile(downloadPath, destinationPath, decoded, decodedIvs, link.getStringProperty("decryptedFileName"));
            File tempFile = new File(downloadPath);
            tempFile.delete();
        }
    }

    private void loginAPI(final Account account, boolean verifySession) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final String apikey = account.getPass();
            if (StringUtils.isEmpty(apikey)) {
                throw new AccountInvalidException();
            } else if (!verifySession) {
                logger.info("Trust apikey without verification");
                return;
            } else {
                logger.info("Performing full login");
                br.setHeader("authorization", apikey);
                br.getPage(API_BASE + "/user");
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        loginAPI(account, true);
        if (br.getURL() == null || !br.getURL().contains("user")) {
            br.getPage(API_BASE + "/user");
        }
        final AccountInfo ai = new AccountInfo();
        final String premiumUntil = PluginJSonUtils.getJson(br, "premiumUntil");
        int responseCode = br.getHttpConnection().getResponseCode();
        if (responseCode == 401) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Falsche Filedo-ID", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        String accountStatus;
        if (premiumUntil == null || OffsetDateTime.parse(premiumUntil).isBefore(OffsetDateTime.now())) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account abgelaufen", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        OffsetDateTime dtOffset = OffsetDateTime.parse(premiumUntil);
        String trafficleftStr = PluginJSonUtils.getJson(br, "dailyDownloadLimit");
        String trafficleftMaxStr = PluginJSonUtils.getJson(br, "maxDailyDownloadLimit");
        ai.setTrafficLeft(Long.parseLong(trafficleftStr) * 1024);
        ai.setTrafficMax(Long.parseLong(trafficleftMaxStr) * 1024);
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(6);
        accountStatus = "Premium Account";
        ai.setValidUntil(dtOffset.toInstant().toEpochMilli());
        final String user = PluginJSonUtils.getJson(br, "secret");
        if (!StringUtils.isEmpty(user)) {
            account.setUser(user);
        }
        ai.setStatus(accountStatus);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    /* API docs: TODO: add link to API docs */
    private Object callAPI(final Request req, final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        // setLoginHeaders(req, account);
        req.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, getApikey(account));
        br.getPage(req);
        return checkErrors(account, link);
    }

    private Object checkErrors(final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        try {
            final Object jsonO = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
            if (jsonO == null || !(jsonO instanceof Map)) {
                return jsonO;
            }
            // TODO: Implement errorhandling
            if (jsonO instanceof Map) {
                final Map<String, Object> thismap = (Map<String, Object>) jsonO;
                return thismap.get("data");
            }
            return jsonO;
        } catch (final JSonMapperException jme) {
            final String errortext = "Bad API response";
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errortext);
            } else {
                throw new AccountUnavailableException(errortext, 1 * 60 * 1000l);
            }
        }
    }

    private String getApikey(final Account account) {
        return correctPassword(account.getPass());
    }

    private static String correctPassword(final String pw) {
        if (pw != null) {
            return pw.trim();
        } else {
            return null;
        }
    }

    @Override
    protected String getAPILoginHelpURL() {
        return "https://" + getHost() + "/user-info";
    }

    @Override
    protected boolean looksLikeValidAPIKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches(".+")) {
            // TODO: Add proper validation
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        if (acc != null && AccountType.PREMIUM.equals(acc.getType())) {
            /* Premium accounts don't have captchas */
            return false;
        } else {
            return link.getBooleanProperty("hasCaptcha", false);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}