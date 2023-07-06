package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "freediscussions.com" }, urls = { "" })
public class FreediscussionsCom extends UseNet {
    public FreediscussionsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.freediscussions.com/packages");
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || host.equalsIgnoreCase("usenet.nl")) {
            return this.getHost();
        } else {
            return super.rewriteHost(host);
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.freediscussions.com/terms";
    }

    public static interface UseNetNLConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("");
        try {
            boolean loggedin = false;
            final String userIDFromUsername = new Regex(account.getUser(), "\\w*-(\\d+)").getMatch(0);
            Map<String, Object> userInfo = null;
            if (cookies != null) {
                logger.info("Attempting cookie login");
                br.setCookies(getHost(), cookies);
                userInfo = this.getUserInfo(br);
                if (br.containsHTML("\"username\"\\s*:\"" + Pattern.quote(account.getUser())) || (userIDFromUsername != null && br.containsHTML("\"uid\"\\s*:" + userIDFromUsername))) {
                    logger.info("Cookie login successful");
                    loggedin = true;
                } else {
                    logger.info("Cookie login failed");
                }
            }
            if (!loggedin) {
                logger.info("Performing full login");
                account.clearCookies(null);
                br.getPage("https://auth." + getHost() + "/login?culture=en&CustomCSS=https%3A%2F%2Fwww.freediscussions.com%2Fauth-css%2Fauth.override.css");
                final Form login = br.getFormbyProperty("class", "form-signin p-2");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("username", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                if (br.getCookie(br.getHost(), "UsenetNL.AuthorizationServerApp_Auth", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new AccountInvalidException();
                }
                userInfo = this.getUserInfo(br);
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            br.postPageRaw("https://janus.freediscussions.com/graphql", "{\"operationName\":\"DashboardInformation\",\"variables\":{},\"query\":\"query DashboardInformation {  radiusData {    volume {      remaining      total      unitResourceStringKey    }    extraBoost {      remaining      total      unitResourceStringKey    }  }  userData {    id  }  radiusName  radiusAccount {    isEnabled  }}\"}");
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> data = (Map<String, Object>) entries.get("data");
            final Map<String, Object> radiusAccount = (Map<String, Object>) data.get("radiusAccount");
            if (!Boolean.TRUE.equals(radiusAccount.get("isEnabled"))) {
                throw new AccountInvalidException("Radius account is disabled");
            }
            final Map<String, Object> radiusData = (Map<String, Object>) data.get("radiusData");
            final Map<String, Object> volume = (Map<String, Object>) radiusData.get("volume");
            final Map<String, Object> extraBoost = (Map<String, Object>) radiusData.get("extraBoost");
            final List<Map<String, Object>> trafficmaps = new ArrayList<Map<String, Object>>();
            trafficmaps.add(volume);
            trafficmaps.add(extraBoost);
            final Map<String, String> unitReplaceMap = new HashMap<String, String>();
            unitReplaceMap.put("UNX_UNIT_TERABYTES", "TB");
            unitReplaceMap.put("UNIT_GIGABYTES", "GB");
            unitReplaceMap.put("UNIT_MEGABYTES", "MB");
            unitReplaceMap.put("UNIT_KILOBYTES", "KB");
            unitReplaceMap.put("UNIT_BYTES", "B");
            long trafficleft = 0;
            long trafficmax = 0;
            long remainingExtraTraffic = 0;
            for (final Map<String, Object> trafficmap : trafficmaps) {
                final String unitResourceStringKey = trafficmap.get("unitResourceStringKey").toString();
                final String realUnit = unitReplaceMap.get(unitResourceStringKey);
                final long trafficleftHere = SizeFormatter.getSize(trafficmap.get("remaining") + realUnit);
                trafficleft += trafficleftHere;
                trafficmax += SizeFormatter.getSize(trafficmap.get("total") + realUnit);
                remainingExtraTraffic += trafficleftHere;
            }
            ai.setTrafficLeft(trafficleft);
            ai.setTrafficMax(trafficmax);
            ai.setStatus(account.getType().getLabel() + " | Extra traffic: " + SizeFormatter.formatBytes(remainingExtraTraffic));
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
        return ai;
    }

    private Map<String, Object> getUserInfo(final Browser br) throws IOException {
        br.postPageRaw("https://janus.freediscussions.com/graphql?", "{\"operationName\":\"SessionQuery\",\"variables\":{},\"query\":\"query SessionQuery {  session {    misc    tdata  }}\"}");
        return restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("power.usenet.nl", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("power.usenet.nl", true, 563));
        ret.addAll(UsenetServer.createServerList("eco.usenet.nl", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("eco.usenet.nl", true, 563));
        return ret;
    }
}
