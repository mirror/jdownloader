package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.requests.PostRequest;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linkifier.com" }, urls = { "" })
public class LinkifierCom extends PluginForHost {
    private static MultiHosterManagement mhm     = new MultiHosterManagement("linkifier.com");
    private static final String          API_KEY = "d046c4309bb7cabd19f49118a2ab25e0";

    public LinkifierCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://www.linkifier.com");
    }

    @Override
    public String getAGBLink() {
        return "https://www.linkifier.com/terms-of-use/";
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final String username = account.getUser();
        if (username == null || !username.matches("^.+?@.+?\\.[^\\.]+")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please use your email address to login", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final AccountInfo ai = new AccountInfo();
        final HashMap<String, Object> userJson = new HashMap<String, Object>();
        userJson.put("login", username);
        userJson.put("md5Pass", Hash.getMD5(account.getPass()));
        userJson.put("apiKey", API_KEY);
        final PostRequest userRequest = new PostRequest("https://api.linkifier.com/downloadapi.svc/user");
        userRequest.setContentType("application/json; charset=utf-8");
        userRequest.setPostBytes(JSonStorage.serializeToJsonByteArray(userJson));
        final HashMap<String, Object> userResponse = JSonStorage.restoreFromString(br.getPage(userRequest), TypeRef.HASHMAP);
        if (Boolean.TRUE.equals(userResponse.get("isActive")) && !Boolean.TRUE.equals(userResponse.get("hasErrors"))) {
            if ("unlimited".equalsIgnoreCase(String.valueOf(userResponse.get("extraTraffic")))) {
                ai.setUnlimitedTraffic();
            }
            final Number expiryDate = (Number) userResponse.get("expirydate");
            if (expiryDate != null) {
                ai.setValidUntil(expiryDate.longValue());
                if (!ai.isExpired()) {
                    final PostRequest hosterRequest = new PostRequest("https://api.linkifier.com/DownloadAPI.svc/hosters");
                    hosterRequest.setContentType("application/json; charset=utf-8");
                    hosterRequest.setPostBytes(JSonStorage.serializeToJsonByteArray(userJson));
                    final HashMap<String, Object> hosterResponse = JSonStorage.restoreFromString(br.getPage(hosterRequest), TypeRef.HASHMAP);
                    final List<Map<String, Object>> hosters = (List<Map<String, Object>>) hosterResponse.get("hosters");
                    if (hosters != null) {
                        final List<String> supportedHosts = new ArrayList<String>();
                        for (Map<String, Object> host : hosters) {
                            final String hostername = host.get("hostername") != null ? String.valueOf(host.get("hostername")) : null;
                            if (Boolean.TRUE.equals(host.get("isActive")) && StringUtils.isNotEmpty(hostername)) {
                                supportedHosts.add(hostername);
                            }
                        }
                        ai.setMultiHostSupport(this, supportedHosts);
                    }
                    account.setType(AccountType.PREMIUM);
                    return ai;
                }
            }
        }
        final String errorMsg = userResponse.get("ErrorMSG") != null ? String.valueOf(userResponse.get("ErrorMSG")) : null;
        if (StringUtils.containsIgnoreCase(errorMsg, "Error verifying api key")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (StringUtils.containsIgnoreCase(errorMsg, "Could not find a customer with those credentials")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (StringUtils.isNotEmpty(errorMsg)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, errorMsg, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (errorMsg == null) {
            final Number expiryDate = (Number) userResponse.get("expirydate");
            if (expiryDate != null && expiryDate.longValue() < System.currentTimeMillis()) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account expired", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        /* 2017-01-05: Free accounts cannot download anything. */
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(1);
        account.setConcurrentUsePossible(false);
        ai.setStatus("Free Account");
        ai.setTrafficLeft(0);
        return ai;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return account != null && account.getType() == AccountType.PREMIUM;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleMultiHost(DownloadLink downloadLink, Account account) throws Exception {
        mhm.runCheck(account, downloadLink);
        final HashMap<String, Object> downloadJson = new HashMap<String, Object>();
        downloadJson.put("login", account.getUser());
        downloadJson.put("md5Pass", Hash.getMD5(account.getPass()));
        downloadJson.put("apiKey", API_KEY);
        downloadJson.put("url", downloadLink.getDefaultPlugin().buildExternalDownloadURL(downloadLink, this));
        final PostRequest downloadRequest = new PostRequest("https://api.linkifier.com/downloadapi.svc/stream");
        downloadRequest.setContentType("application/json; charset=utf-8");
        downloadRequest.setPostBytes(JSonStorage.serializeToJsonByteArray(downloadJson));
        final HashMap<String, Object> downloadResponse = JSonStorage.restoreFromString(br.getPage(downloadRequest), TypeRef.HASHMAP);
        if (Boolean.FALSE.equals(downloadResponse.get("hasErrors"))) {
            String url = downloadResponse.get("url") != null ? String.valueOf(downloadResponse.get("url")) : null;
            if (StringUtils.isEmpty(url)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setConnectTimeout(120 * 1000);
            br.setReadTimeout(120 * 1000);
            final Number con_max = (Number) downloadResponse.get("con_max");
            final Boolean con_resume = (Boolean) downloadResponse.get("con_resume");
            final boolean resume;
            final int maxChunks;
            if (Boolean.FALSE.equals(con_resume)) {
                maxChunks = 1;
                resume = false;
            } else {
                resume = true;
                if (con_max != null) {
                    if (con_max.intValue() <= 1) {
                        maxChunks = 1;
                    } else {
                        maxChunks = -con_max.intValue();
                    }
                } else {
                    maxChunks = 1;
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, resume, maxChunks);
            if (StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "json") || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text")) {
                try {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    br.followConnection();
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!dl.getConnection().isContentDisposition()) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
            return;
        }
        final String errorMsg = downloadResponse.get("ErrorMSG") != null ? String.valueOf(downloadResponse.get("ErrorMSG")) : null;
        if (errorMsg != null) {
            if (StringUtils.containsIgnoreCase(errorMsg, "Error verifying api key")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (StringUtils.containsIgnoreCase(errorMsg, "Could not find a customer with those credentials")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errorMsg, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (StringUtils.containsIgnoreCase(errorMsg, "Account expired")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errorMsg, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (StringUtils.containsIgnoreCase(errorMsg, "Customer reached daily limit for current hoster")) {
                mhm.putError(account, downloadLink, 60 * 60 * 1000l, errorMsg);
            }
            if (StringUtils.containsIgnoreCase(errorMsg, "Accounts are maxed out for current hoster")) {
                mhm.putError(account, downloadLink, 60 * 60 * 1000l, errorMsg);
            }
            if (StringUtils.containsIgnoreCase(errorMsg, "Downloads blocked until")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errorMsg, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        } else {
            final Number expiryDate = (Number) downloadResponse.get("expirydate");
            if (expiryDate != null && expiryDate.longValue() < System.currentTimeMillis()) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account expired", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, errorMsg);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }
}
