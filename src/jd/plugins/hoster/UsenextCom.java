package jd.plugins.hoster;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.requests.GetRequest;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "usenext.com" }, urls = { "" })
public class UsenextCom extends UseNet {
    public UsenextCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.usenext.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://www.usenext.com/terms";
    }

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    protected String getUseNetUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    public static interface UsenextConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public void update(final DownloadLink downloadLink, final Account account, long bytesTransfered) throws PluginException {
        final UsenetServer server = getLastUsedUsenetServer();
        if (server == null || !StringUtils.equalsIgnoreCase("flat.usenext.de", server.getHost())) {
            super.update(downloadLink, account, bytesTransfered);
        }
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account, AbstractProxySelectorImpl proxy) {
        if (account != null) {
            final UsenetAccountConfigInterface config = getAccountJsonConfig(account);
            if (config != null && StringUtils.equalsIgnoreCase("flat.usenext.de", config.getHost())) {
                return 4;
            }
        }
        return super.getMaxSimultanDownload(link, account, proxy);
    }

    private long parseNumber(Map<String, Object> map, String id, long def) throws Exception {
        final Object value = map.get(id);
        if (value == null) {
            if (def == -1) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "not available:" + id);
            } else {
                return def;
            }
        }
        String unit = (String) map.get("unitResourceStringKey");
        if (StringUtils.contains(unit, "GIGA") || StringUtils.equals(unit, "UNX_UNIT_GIGABYTES")) {
            unit = "GB";
        } else if (StringUtils.contains(unit, "MEGA")) {
            unit = "MB";
        } else if (StringUtils.contains(unit, "TERA")) {
            unit = "TB";
        } else if (StringUtils.equals(unit, "UNX_UNIT_BYTES")) {
            unit = "B";
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "unsupported:" + unit);
        }
        return SizeFormatter.getSize(value + unit);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("com");
        try {
            boolean freshLogin = true;
            String UNX_API_KEY = null;
            String API_URL = null;
            if (cookies != null) {
                br.setCookies("usenext.com", cookies);
                final Cookies de = account.loadCookies("de");
                if (de != null) {
                    br.setCookies("usenext.de", de);
                }
                getPage(br, "https://www.usenext.com/en-US/ma/dashboard");
                if (br.getHostCookie("UseNeXT.AuthorizationServerApp_Auth", Cookies.NOTDELETEDPATTERN) == null) {
                    freshLogin = true;
                } else if (br.getHostCookie("UseNeXT.AuthorizationServerApp_Session", Cookies.NOTDELETEDPATTERN) == null) {
                    freshLogin = true;
                } else {
                    UNX_API_KEY = br.getRegex("UNX_API_KEY\\s*:\\s*\"(.*?)\"").getMatch(0);
                    API_URL = br.getRegex("API_URL\\s*:\\s*\"(https?://.*?)\"").getMatch(0);
                    if (!StringUtils.isAllNotEmpty(UNX_API_KEY, API_URL)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        freshLogin = false;
                    }
                }
            }
            if (freshLogin) {
                br.getCookies("usenext.com").clear();
                br.getCookies("usenext.de").clear();
                account.clearCookies("");
                getPage(br, "https://www.usenext.com/");
                String clientID = br.getRegex("src\\s*=\\s*\"(/App[^\"]*\\.js)\"").getMatch(0);
                if (clientID != null) {
                    final Browser brc = br.cloneBrowser();
                    brc.getPage(clientID);
                    clientID = brc.getRegex("client_id\\s*:\\s*\"(.*?)\"").getMatch(0);
                }
                if (clientID == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                getPage(br, "https://auth.usenext.com/login?ReturnUrl=%2Fauth%3Fredirect_uri%3Dhttps%253A%252F%252Fwww.usenext.com%252Fma%252Fsignin-usenext%26client_id%3D" + URLEncode.encodeURIComponent(clientID) + "%26scope%3Duser%252Cradius%252Cshop%26response_type%3Dcode");
                final Form login = br.getFormbyKey("username");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("username", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                submitForm(br, login);
                if (br.getHostCookie("UseNeXT.AuthorizationServerApp_Auth", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getHostCookie("UseNeXT.AuthorizationServerApp_Session", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                getPage(br, "https://www.usenext.com/en-US/ma/dashboard");
                UNX_API_KEY = br.getRegex("UNX_API_KEY\\s*:\\s*\"(.*?)\"").getMatch(0);
                API_URL = br.getRegex("API_URL\\s*:\\s*\"(https?://.*?)\"").getMatch(0);
                if (!StringUtils.isAllNotEmpty(UNX_API_KEY, API_URL)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            account.saveCookies(br.getCookies("usenext.com"), "com");
            account.saveCookies(br.getCookies("usenext.de"), "de");
            if (false) {
                final GetRequest getRequest = br.createGetRequest(URLHelper.parseLocation(new URL(API_URL), "/session?ref="));
                getRequest.getHeaders().put("Origin", "https://www.usenext.com");
                br.setCurrentURL("https://www.usenext.com/");
                sendRequest(getRequest);
            }
            final PostRequest postRequest = br.createJSonPostRequest(URLHelper.parseLocation(new URL(API_URL), "/graphql"),
                    "{\"operationName\":\"DashboardInformation\",\"variables\":{},\"query\":\"query DashboardInformation {\\n  radiusData {\\n    volume {\\n      remaining\\n      total\\n      unitResourceStringKey\\n    }\\n    extraBoost {\\n      remaining\\n      total\\n      unitResourceStringKey\\n    }\\n  }\\n  cancellationInformation {\\n    isContractLocked\\n    hasWithdrawableCancellation\\n    isServiceDenied\\n    isInCancellationPeriod\\n    cancellationProcess {\\n      createDate\\n    }\\n  }\\n  currentServiceRoundUpgradeData {\\n    hasPendingUpgrade\\n    isLastUpgrade\\n    accountingPeriod {\\n      remaining\\n      total\\n      unitResourceStringKey\\n    }\\n  }\\n  serviceInformation {\\n    currentServiceRound {\\n      currEndDate\\n      startDate\\n      article {\\n        id\\n        name\\n        articleTypeId\\n        priceNet\\n        priceGross\\n        volumeGb\\n        runtime\\n        runtimeUnit\\n      }\\n      invoice {\\n        id\\n        createDate\\n        uuid\\n        invoiceStatePaths {\\n          invoiceStateId\\n          isCurrent\\n        }\\n      }\\n    }\\n    nextServiceRoundBeginDate\\n    nextArticle {\\n      id\\n      name\\n      articleTypeId\\n      priceNet\\n      priceGross\\n      volumeGb\\n      runtime\\n      runtimeUnit\\n    }\\n  }\\n}\\n\"}");
            postRequest.getHeaders().put("X-UNX-API-KEY", UNX_API_KEY);
            postRequest.getHeaders().put("x-ui-language", "en-US");
            postRequest.getHeaders().put("Origin", "https://www.usenext.com");
            br.setCurrentURL("https://www.usenext.com/");
            sendRequest(postRequest);
            final Map<String, Object> json = restoreFromString(br.toString(), TypeRef.MAP);
            final Map<String, Object> volume = (Map<String, Object>) JavaScriptEngineFactory.walkJson(json, "data/radiusData/volume");
            final long trafficTotal = parseNumber(volume, "total", -1);
            final long trafficRemaining = parseNumber(volume, "remaining", -1);
            final Map<String, Object> extraBoost = (Map<String, Object>) JavaScriptEngineFactory.walkJson(json, "data/radiusData/extraBoost");
            if (extraBoost != null) {
                final long boostTotal = parseNumber(extraBoost, "total", -1);
                final long boostRemaining = parseNumber(extraBoost, "remaining", -1);
                ai.setTrafficMax(trafficTotal + boostTotal);
                ai.setTrafficLeft(trafficRemaining + boostRemaining);
            } else {
                ai.setTrafficMax(trafficTotal);
                ai.setTrafficLeft(trafficRemaining);
            }
            final Map<String, Object> currentServiceRound = (Map<String, Object>) JavaScriptEngineFactory.walkJson(json, "data/serviceInformation/currentServiceRound");
            final String currEndDate = (String) currentServiceRound.get("currEndDate");
            final Date expireDate = TimeFormatter.parseDateString(currEndDate);
            if (expireDate != null) {
                ai.setValidUntil(expireDate.getTime());
                ai.setStatus((String) JavaScriptEngineFactory.walkJson(currentServiceRound, "article/name"));
            }
            account.setMaxSimultanDownloads(30);
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("com");
                account.clearCookies("de");
            }
            throw e;
        }
        account.setRefreshTimeout(2 * 60 * 60 * 1000l);
        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
        return ai;
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.usenext.de", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("news.usenext.de", true, 563));
        ret.addAll(UsenetServer.createServerList("flat.usenext.de", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("flat.usenext.de", true, 563));
        ret.addAll(UsenetServer.createServerList("high.usenext.de", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("high.usenext.de", true, 563));
        return ret;
    }
}
