package jd.plugins.hoster;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Request;
import jd.http.requests.FormData;
import jd.http.requests.GetRequest;
import jd.http.requests.PostFormDataRequest;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "premiumlinkgenerator.com" }, urls = { "" })
public class PremiumLinkGeneratorCom extends antiDDoSForHost {
    public PremiumLinkGeneratorCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://premiumlinkgenerator.com/prices");
    }

    @Override
    public String getAGBLink() {
        return "https://premiumlinkgenerator.com/terms-of-use";
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final String token = login(account);
        final Map<String, Object> userPlan = callAPI(account, token, "user-plan");
        final String product_Type = (String) ((Map<String, Object>) userPlan.get("plan")).get("product_type");
        ai.setStatus(product_Type);
        if ("DAILY".equalsIgnoreCase(product_Type)) {
            final Number expire = (Number) ((Map<String, Object>) userPlan.get("plan")).get("expire");
            if (expire == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                ai.setUnlimitedTraffic();
                ai.setValidUntil(expire.longValue() * 1000l);
            }
        } else if ("LIMITED".equalsIgnoreCase(product_Type)) {
            final Number limit = (Number) ((Map<String, Object>) userPlan.get("plan")).get("limit");
            final Number limit_left = (Number) ((Map<String, Object>) userPlan.get("plan")).get("limit_left");
            if (limit == null || limit_left == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                ai.setValidUntil(-1);
                ai.setTrafficMax(limit.longValue());
                ai.setTrafficLeft(limit_left.longValue());
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> supportedHosts = callAPI(account, token, "supported-hosts");
        // TODO: evaluate "maxConcurentDownloads"
        final List<Map<String, Object>> hosts = (List<Map<String, Object>>) supportedHosts.get("hosts");
        final List<String> hostList = new ArrayList<String>();
        for (final Map<String, Object> host : hosts) {
            final String name = (String) host.get("name");
            if (name != null) {
                hostList.add(name);
            }
        }
        ai.setMultiHostSupport(this, hostList);
        return ai;
    }

    private Number getNumber(Map<String, Object> map, final String key) {
        final Object ret = map.get(key);
        if (ret instanceof Number) {
            return (Number) ret;
        } else if (ret instanceof String) {
            return Long.parseLong(ret.toString());
        } else {
            return null;
        }
    }

    private String login(Account account) throws Exception {
        synchronized (account) {
            final Map<String, Object> login = callAPI(account, null, "login");
            final String token = (String) login.get("token");
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                account.setProperty(TOKEN_PROPERTY, token);
                return token;
            }
        }
    }

    private final String API            = "https://ww.premiumlinkgenerator.com/api/jdownloader/";
    private final String TOKEN_PROPERTY = "token";

    private Map<String, Object> callAPI(final Account account, final String token, final String method, Object[]... params) throws Exception {
        final Request request;
        if ("user-plan".equalsIgnoreCase(method) || "supported-hosts".equalsIgnoreCase(method)) {
            request = new GetRequest(URLHelper.parseLocation(new URL(API), method + "?_format=json"));
        } else {
            request = new PostFormDataRequest(URLHelper.parseLocation(new URL(API), method + "?_format=json"));
        }
        final String authToken;
        if (token != null) {
            authToken = token;
        } else {
            synchronized (account) {
                authToken = account.getStringProperty(TOKEN_PROPERTY, null);
            }
        }
        if ("login".equalsIgnoreCase(method)) {
            ((PostFormDataRequest) request).addFormData(new FormData("username", account.getUser()));
            ((PostFormDataRequest) request).addFormData(new FormData("password", account.getPass()));
        } else if (authToken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            request.getHeaders().put("Authorization", "Bearer " + authToken);
        }
        if (params != null && params.length > 0) {
            for (final Object[] postParam : params) {
                if (postParam != null && postParam.length == 2) {
                    ((PostFormDataRequest) request).addFormData(new FormData(String.valueOf(postParam[0]), String.valueOf(postParam[1])));
                }
            }
        }
        sendRequest(request);
        final Map<String, Object> response = JSonStorage.restoreFromString(request.getHtmlCode(), TypeRef.HASHMAP, null);
        final Number status = getNumber(response, "status");
        if (status == null || status.intValue() != 200) {
            try {
                final String message = (String) response.get("message");
                if (status != null && status.intValue() == 401) {
                    if ("login".equalsIgnoreCase(method)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message, 5 * 60 * 1000l);
                    }
                }
                final Number code = getNumber(response, "code");
                if (code == null || status == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, message);
                }
                switch (code.intValue()) {
                case 502:// Failed to create new token. Please try again later
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                case 501:// Invalid username or password
                case 503: // No active plan or plan is expired
                case 506: // Plan limitations are exceeded
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    synchronized (account) {
                        if (authToken != null && authToken.equals(account.getStringProperty(TOKEN_PROPERTY, null))) {
                            account.removeProperty(TOKEN_PROPERTY);
                        }
                    }
                } else if (e.getLinkStatus() == LinkStatus.ERROR_RETRY) {
                    synchronized (account) {
                        synchronized (account) {
                            if (authToken != null && authToken.equals(account.getStringProperty(TOKEN_PROPERTY, null))) {
                                account.removeProperty(TOKEN_PROPERTY);
                            }
                        }
                    }
                }
                throw e;
            }
        }
        return response;
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
        final String token = login(account);
        final String hosterURL = downloadLink.getDefaultPlugin().buildExternalDownloadURL(downloadLink, this);
        final Map<String, Object> getDownloadLink = callAPI(account, token, "get-download-link", new String[] { "link", hosterURL });
        final Number status = getNumber(getDownloadLink, "status");
        if (status == null || status.intValue() != 200) {
            final String message = (String) getDownloadLink.get("message");
            final Number code = getNumber(getDownloadLink, "code");
            if (code == null || status == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final AccountInfo ai = account.getAccountInfo();
            switch (code.intValue()) {
            case 504: // Currently there are no avaliable services to handle request
                if (ai != null) {
                    ai.removeMultiHostSupport(downloadLink.getHost());
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message, 5 * 60 * 1000l);
            case 505: // Failed to create download ticket
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message, 30 * 60 * 1000l);
            case 507:// Failed to get download link
                if (ai != null) {
                    ai.removeMultiHostSupport(downloadLink.getHost());
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message, 5 * 60 * 1000l);
            default:
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, message);
            }
        }
        final Map<String, Object> link = (Map<String, Object>) getDownloadLink.get("link");
        final String url = (String) link.get("url");
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final boolean resumeFlag = Boolean.TRUE.equals(link.get("resumable"));
        final Number maxConnections = getNumber(link, "maxConnections");
        final int maxChunks;
        if (maxConnections == null || maxConnections.intValue() <= 0 || resumeFlag == false) {
            maxChunks = 1;
        } else {
            if (maxConnections.intValue() == 1) {
                maxChunks = 1;
            } else {
                maxChunks = -maxConnections.intValue();
            }
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, resumeFlag, maxChunks);
        if (!dl.getConnection().isContentDisposition() || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
