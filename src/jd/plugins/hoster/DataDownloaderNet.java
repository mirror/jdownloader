package jd.plugins.hoster;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision: 35357 $", interfaceVersion = 3, names = { "datadownloader.net" }, urls = { "" })
public class DataDownloaderNet extends PluginForHost {
    public DataDownloaderNet(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://datadownloader.net/license");
    }

    @Override
    public String getAGBLink() {
        return "https://datadownloader.net/terms";
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, ai);
        // no list available, update manually
        ai.setMultiHostSupport(this, Arrays.asList(new String[] { "rapidgator.net", "datafile.com", "alfafile.net" }));
        return ai;
    }

    private final String APIKEY = "APIKEY";

    private Boolean getInfo(final String apiKey, Account account, AccountInfo ai) throws Exception {
        if (apiKey != null) {
            br.getPage("https://datadownloader.net/api/v1/get-info?apikey=" + apiKey + "&sign=" + sign(apiKey));
            final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                final Map<String, Object> ret = (Map<String, Object>) response.get("return");
                if (StringUtils.equalsIgnoreCase((String) ret.get("type"), "premium")) {
                    final Map<String, Object> premium = (Map<String, Object>) ret.get("premium");
                    final Number till_timestamp = (Number) premium.get("till_timestamp");
                    final Number bytes_left = (Number) premium.get("bytes_left");
                    if (bytes_left != null && till_timestamp != null && till_timestamp.longValue() * 1000 > System.currentTimeMillis()) {
                        if (ai != null) {
                            ai.setValidUntil(till_timestamp.longValue() * 1000);
                            ai.setTrafficLeft(bytes_left.longValue());
                        }
                        return true;
                    }
                }
            }
            handleError(apiKey, response, account);
        }
        return null;
    }

    private String login(Account account, AccountInfo ai) throws Exception {
        synchronized (account) {
            try {
                String apiKey = account.getStringProperty(APIKEY, null);
                try {
                    if (apiKey != null && getInfo(apiKey, account, ai)) {
                        account.setProperty(APIKEY, apiKey);
                        return apiKey;
                    }
                } catch (PluginException e) {
                    if (e.getLinkStatus() != LinkStatus.ERROR_PREMIUM) {
                        throw e;
                    }
                }
                br.getPage("https://datadownloader.net/api/v1/login?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&sign=" + sign(account.getUser() + account.getPass()));
                final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                    apiKey = (String) ((Map<String, Object>) response.get("return")).get("apikey");
                    if (apiKey != null && getInfo(apiKey, account, ai)) {
                        account.setProperty(APIKEY, apiKey);
                        return apiKey;
                    }
                }
                handleError(apiKey, response, account);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.removeProperty(APIKEY);
                }
                throw e;
            }
        }
    }

    private String sign(String value) throws IOException {
        return Hash.getMD5(new String(Base64.decode("MTJkYXMzNHZ6eHN3ZDN3cmY="), "UTF-8") + value);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return account != null;
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
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleMultiHost(DownloadLink downloadLink, Account account) throws Exception {
        final String apiKey;
        synchronized (account) {
            final String key = account.getStringProperty(APIKEY, null);
            if (key == null) {
                apiKey = login(account, account.getAccountInfo());
            } else {
                apiKey = key;
            }
        }
        final String url = downloadLink.getDefaultPlugin().buildExternalDownloadURL(downloadLink, this);
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("https://datadownloader.net/api/v1/download?apikey=" + apiKey + "&url=" + URLEncoder.encode(url, "UTF-8") + "&sign=" + sign(apiKey + url));
        final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        if (response != null && Boolean.TRUE.equals(response.get("success"))) {
            final String error = (String) ((Map<String, Object>) response.get("return")).get("error");
            if (error != null) {
                if ("No provider".equalsIgnoreCase(error)) {
                    synchronized (account) {
                        final AccountInfo ai = account.getAccountInfo();
                        if (ai != null) {
                            ai.removeMultiHostSupport(downloadLink.getHost());
                        }
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "error", 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, error);
            }
            final String downloadURL = (String) ((Map<String, Object>) response.get("return")).get("url");
            if (downloadURL != null) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadURL, true, 1);
                if (StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text")) {
                    br.followConnection();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.startDownload();
                return;
            }
        }
        handleError(apiKey, response, account);
    }

    private void handleError(final String apiKey, Map<String, Object> response, Account account) throws PluginException {
        final Object message = response != null ? response.get("message") : null;
        if (message != null) {
            if ("Signature validation error".equalsIgnoreCase(message.toString())) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if ("Account not found".equalsIgnoreCase(message.toString())) {
                synchronized (account) {
                    if (apiKey.equals(account.getStringProperty(APIKEY, null))) {
                        account.removeProperty(APIKEY);
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, message.toString(), PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
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
