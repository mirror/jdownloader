package jd.plugins.decrypter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawlerDeepHelperInterface;
import jd.controlling.linkcrawler.LinkCrawlerRule;
import jd.http.Authentication;
import jd.http.AuthenticationFactory;
import jd.http.Browser;
import jd.http.CallbackAuthenticationFactory;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.DefaultAuthenticanFactory;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.URLUserInfoAuthentication;
import jd.http.requests.GetRequest;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.swing.dialog.LoginDialog;
import org.appwork.utils.swing.dialog.LoginDialogInterface;
import org.jdownloader.auth.AuthenticationController;
import org.jdownloader.auth.AuthenticationInfo;
import org.jdownloader.auth.AuthenticationInfo.Type;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.translate._JDT;

@DecrypterPlugin(revision = "$Revision: 40857 $", interfaceVersion = 3, names = { "linkcrawlerdeephelper" }, urls = { "" })
public class LinkCrawlerDeepHelper extends antiDDoSForDecrypt implements LinkCrawlerDeepHelperInterface {
    public LinkCrawlerDeepHelper(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public URLConnectionAdapter openConnection(LinkCrawlerRule matchingRule, Browser br, CrawledLink source) throws Exception {
        br.addAllowedResponseCodes(500);
        final List<String[]> setCookies = matchingRule != null ? getCrawler().getLinkCrawlerRuleCookies(matchingRule.getId()) : null;
        if (setCookies != null) {
            for (final String cookie[] : setCookies) {
                if (cookie != null) {
                    if (cookie.length == 1) {
                        br.setCookie(source.getURL(), cookie[0], null);
                    } else if (cookie.length > 1) {
                        br.setCookie(source.getURL(), cookie[0], cookie[1]);
                    }
                }
            }
        }
        final URLConnectionAdapter ret = openCrawlDeeperConnection(br, source, 0);
        if (matchingRule != null && matchingRule.isUpdateCookies()) {
            final Cookies cookies = br.getCookies(source.getURL());
            final List<String[]> currentCookies = new ArrayList<String[]>();
            for (final Cookie cookie : cookies.getCookies()) {
                if (!cookie.isExpired()) {
                    currentCookies.add(new String[] { cookie.getKey(), cookie.getValue() });
                }
            }
            getCrawler().setLinkCrawlerRuleCookies(matchingRule.getId(), currentCookies);
        }
        return ret;
    }

    protected URLConnectionAdapter openCrawlDeeperConnection(Browser br, CrawledLink source, int round) throws Exception {
        final HashSet<String> loopAvoid = new HashSet<String>();
        if (round == 0) {
            final CrawledLink sourceLink = source.getSourceLink();
            if (sourceLink != null && StringUtils.startsWithCaseInsensitive(sourceLink.getURL(), "http")) {
                br.setCurrentURL(sourceLink.getURL());
            }
        }
        Request request = new GetRequest(source.getURL());
        loopAvoid.add(request.getUrl());
        URLConnectionAdapter connection = null;
        for (int i = 0; i < 10; i++) {
            final List<AuthenticationFactory> authenticationFactories = new ArrayList<AuthenticationFactory>();
            if (request.getURL().getUserInfo() != null) {
                authenticationFactories.add(new URLUserInfoAuthentication());
            }
            authenticationFactories.addAll(AuthenticationController.getInstance().getSortedAuthenticationFactories(request.getURL(), null));
            authenticationFactories.add(new CallbackAuthenticationFactory() {
                protected Authentication remember = null;

                @Override
                protected Authentication askAuthentication(Browser browser, Request request, final String realm) {
                    final LoginDialog loginDialog = new LoginDialog(UIOManager.LOGIC_COUNTDOWN, _GUI.T.AskForPasswordDialog_AskForPasswordDialog_title_(), _JDT.T.Plugin_requestLogins_message(), new AbstractIcon(IconKey.ICON_PASSWORD, 32));
                    loginDialog.setTimeout(60 * 1000);
                    final LoginDialogInterface handle = UIOManager.I().show(LoginDialogInterface.class, loginDialog);
                    if (handle.getCloseReason() == CloseReason.OK) {
                        final Authentication ret = new DefaultAuthenticanFactory(request.getURL().getHost(), realm, handle.getUsername(), handle.getPassword()).buildAuthentication(browser, request);
                        addAuthentication(ret);
                        if (handle.isRememberSelected()) {
                            remember = ret;
                        }
                        return ret;
                    } else {
                        return null;
                    }
                }

                @Override
                public boolean retry(Authentication authentication, Browser browser, Request request) {
                    if (containsAuthentication(authentication) && remember == authentication && request.getAuthentication() == authentication && !requiresAuthentication(request)) {
                        final AuthenticationInfo auth = new AuthenticationInfo();
                        auth.setRealm(authentication.getRealm());
                        auth.setUsername(authentication.getUsername());
                        auth.setPassword(authentication.getPassword());
                        auth.setHostmask(authentication.getHost());
                        auth.setType(Type.HTTP);
                        AuthenticationController.getInstance().add(auth);
                    }
                    return super.retry(authentication, browser, request);
                }
            });
            authLoop: for (AuthenticationFactory authenticationFactory : authenticationFactories) {
                if (connection != null) {
                    try {
                        br.followConnection(true);
                    } catch (IOException e) {
                        getLogger().log(e);
                    }
                }
                br.setCustomAuthenticationFactory(authenticationFactory);
                connection = openAntiDDoSRequestConnection(br, request);
                if (connection.getResponseCode() == 401 || connection.getResponseCode() == 403) {
                    if (connection.getHeaderField(HTTPConstants.HEADER_RESPONSE_WWW_AUTHENTICATE) == null) {
                        return openCrawlDeeperConnection(source, br, connection, round);
                    } else {
                        continue authLoop;
                    }
                } else if (connection.isOK()) {
                    break authLoop;
                } else {
                    return openCrawlDeeperConnection(source, br, connection, round);
                }
            }
            final String location = request.getLocation();
            if (location != null) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    getLogger().log(e);
                }
                if (loopAvoid.add(location) == false) {
                    return openCrawlDeeperConnection(source, br, connection, round);
                } else {
                    request = br.createRedirectFollowingRequest(request);
                }
            } else {
                return openCrawlDeeperConnection(source, br, connection, round);
            }
        }
        return openCrawlDeeperConnection(source, br, connection, round);
    }

    protected URLConnectionAdapter openCrawlDeeperConnection(CrawledLink source, Browser br, URLConnectionAdapter urlConnection, int round) throws Exception {
        if (urlConnection != null && getCrawler().getDeepInspector().looksLikeDownloadableContent(urlConnection)) {
            return urlConnection;
        } else if (round <= 2 && urlConnection != null) {
            if (round < 2 && (urlConnection.isOK() || urlConnection.getResponseCode() == 404) && br != null && !br.getCookies(br.getBaseURL()).isEmpty()) {
                final Cookies cookies = br.getCookies(br.getBaseURL());
                for (final Cookie cookie : cookies.getCookies()) {
                    if (StringUtils.contains(cookie.getKey(), "incap_ses")) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            urlConnection.disconnect();
                            throw new IOException(e);
                        }
                        break;
                    }
                }
                if (round < 1) {
                    br.setCurrentURL(source.getURL());
                } else {
                    br.setCurrentURL(URLHelper.parseLocation(new URL(source.getURL()), "/"));
                }
                urlConnection.disconnect();
                return openCrawlDeeperConnection(br, source, round + 1);
            }
            final LinkCollectingJob job = source.getSourceJob();
            if (job != null && job.getCustomSourceUrl() != null) {
                br.setCurrentURL(job.getCustomSourceUrl());
                urlConnection.disconnect();
                return openCrawlDeeperConnection(br, source, round + 1);
            }
        }
        return urlConnection;
    }
}
