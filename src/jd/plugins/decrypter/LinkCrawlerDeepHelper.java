package jd.plugins.decrypter;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.auth.AuthenticationController;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerDeepHelperInterface;
import jd.controlling.linkcrawler.LinkCrawlerRule;
import jd.http.AuthenticationFactory;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linkcrawlerdeephelper" }, urls = { "" })
@Deprecated
public class LinkCrawlerDeepHelper extends PluginForDecrypt implements LinkCrawlerDeepHelperInterface {
    public LinkCrawlerDeepHelper(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    @Deprecated
    public URLConnectionAdapter openConnection(final LinkCrawlerRule matchingRule, Browser br, CrawledLink source) throws Exception {
        br.addAllowedResponseCodes(500);
        final String sourceURL = source.getURL();
        final List<String[]> setCookies = matchingRule != null ? LinkCrawler.getLinkCrawlerRuleCookies(matchingRule.getId()) : null;
        if (setCookies != null) {
            for (final String cookie[] : setCookies) {
                if (cookie != null) {
                    switch (cookie.length) {
                    case 1:
                        br.setCookie(sourceURL, cookie[0], null);
                        break;
                    case 2:
                        br.setCookie(sourceURL, cookie[0], cookie[1]);
                        break;
                    case 3:
                        try {
                            if (cookie[2] != null && sourceURL.matches(cookie[2])) {
                                br.setCookie(sourceURL, cookie[0], cookie[1]);
                            }
                        } catch (Exception e) {
                            logger.log(e);
                        }
                        break;
                    default:
                        break;
                    }
                }
            }
        }
        final URLConnectionAdapter ret = openCrawlDeeperConnection(br, source, 0);
        if (matchingRule != null && matchingRule.isUpdateCookies()) {
            final Cookies cookies = br.getCookies(sourceURL);
            final List<String[]> currentCookies = new ArrayList<String[]>();
            for (final Cookie cookie : cookies.getCookies()) {
                if (!cookie.isExpired()) {
                    currentCookies.add(new String[] { cookie.getKey(), cookie.getValue() });
                }
            }
            // TODO: add support for length==3, url pattern matching support
            getCrawler().setLinkCrawlerRuleCookies(matchingRule.getId(), currentCookies);
        }
        return ret;
    }

    /** Connection handling with basic authentication handling. */
    @Deprecated
    protected URLConnectionAdapter openCrawlDeeperConnection(final Browser br, CrawledLink source, int round) throws Exception {
        if (round == 0) {
            final CrawledLink sourceLink = source.getSourceLink();
            if (sourceLink != null && StringUtils.startsWithCaseInsensitive(sourceLink.getURL(), "http")) {
                /* Use current link as referer */
                br.setCurrentURL(sourceLink.getURL());
            }
        }
        final HashSet<String> loopAvoid = new HashSet<String>();
        Request request = new GetRequest(source.getURL());
        loopAvoid.add(request.getUrl());
        URLConnectionAdapter connection = null;
        for (int i = 0; i < 10; i++) {
            final List<AuthenticationFactory> authenticationFactories = AuthenticationController.getInstance().buildAuthenticationFactories(request.getURL(), null);
            authloop: for (AuthenticationFactory authenticationFactory : authenticationFactories) {
                if (connection != null) {
                    br.followConnection(true);
                }
                br.setCustomAuthenticationFactory(authenticationFactory);
                connection = br.openRequestConnection(request);
                if ((connection.getResponseCode() == 401 || connection.getResponseCode() == 403) && connection.getHeaderField(HTTPConstants.HEADER_RESPONSE_WWW_AUTHENTICATE) != null) {
                    /* Invalid or missing auth */
                    continue authloop;
                } else {
                    break authloop;
                }
            }
            final String location = request.getLocation();
            if (location != null) {
                /* Redirect */
                br.followConnection(true);
                if (loopAvoid.add(location) == false) {
                    return openCrawlDeeperConnection(source, br, connection, round);
                } else {
                    /* Continue */
                    request = br.createRedirectFollowingRequest(request);
                }
            } else {
                return openCrawlDeeperConnection(source, br, connection, round);
            }
        }
        return openCrawlDeeperConnection(source, br, connection, round);
    }

    @Deprecated
    protected URLConnectionAdapter openCrawlDeeperConnection(CrawledLink source, Browser br, URLConnectionAdapter urlConnection, int round) throws Exception {
        if (round > 2) {
            /* Prevent infinite loop */
            return urlConnection;
        }
        if (urlConnection != null && getCrawler().getDeepInspector().looksLikeDownloadableContent(urlConnection)) {
            /* Looks like downloadable file -> Do not load connection. */
            return urlConnection;
        } else {
            if (round < 2 && (urlConnection.isOK() || urlConnection.getResponseCode() == 404) && br != null && !br.getCookies(br.getBaseURL()).isEmpty()) {
                if (round < 1) {
                    /* First round */
                    br.setCurrentURL(source.getURL());
                } else {
                    br.setCurrentURL(URLHelper.parseLocation(new URL(source.getURL()), "/"));
                }
                urlConnection.disconnect();
                return openCrawlDeeperConnection(br, source, round + 1);
            }
            final LinkCollectingJob job = source.getSourceJob();
            if (job != null && job.getCustomSourceUrl() != null) {
                /* Sets custom referer header */
                br.setCurrentURL(job.getCustomSourceUrl());
                urlConnection.disconnect();
                return openCrawlDeeperConnection(br, source, round + 1);
            }
        }
        return urlConnection;
    }
}
