//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.ProxySelectorInterface;
import jd.http.Request;
import jd.http.requests.GetRequest;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.encoding.Base64;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "redgifs.com" }, urls = { "https?://(?:www\\.)?redgifs\\.com/(?:watch|ifr)/([A-Za-z0-9]+)" })
public class RedGifsCom extends GfyCatCom {
    /**
     * 2022-12-27: different site/api
     *
     * @param wrapper
     */
    public RedGifsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static Map<ProxySelectorInterface, Object> TEMPORARYTOKENS             = new WeakHashMap<ProxySelectorInterface, Object>();
    private static AtomicBoolean                       failSafeTemporaryTokenIssue = new AtomicBoolean(false);

    // https://github.com/Redgifs/api/wiki/Temporary-tokens
    // tokens are bound to IP/UA
    // tokens seem to expire after 24 hours, see 'exp' field in token
    private String getTemporaryToken(final Browser br, final String renewIfToken) throws Exception {
        synchronized (TEMPORARYTOKENS) {
            Object tokenDetails[] = (Object[]) TEMPORARYTOKENS.get(br.getProxy());
            if (tokenDetails == null) {
                tokenDetails = new Object[] { null, -1l };
                TEMPORARYTOKENS.put(br.getProxy(), tokenDetails);
            }
            if (failSafeTemporaryTokenIssue.get()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String token = (String) tokenDetails[0];
            final long now = Time.systemIndependentCurrentJVMTimeMillis();
            if (StringUtils.equals(renewIfToken, token) || StringUtils.isEmpty(token) || ((Number) tokenDetails[1]).longValue() < now) {
                if (tokenDetails[0] == null) {
                    logger.info("fetch temporary token for the first time");
                } else {
                    logger.info("refresh temporary token because the old one might be expired");
                }
                final Browser brc = br.cloneBrowser();
                final GetRequest request = brc.createGetRequest("https://api.redgifs.com/v2/auth/temporary");
                request.getHeaders().put(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://www.redgifs.com");
                request.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, "https://www.redgifs.com/");
                brc.getPage(request);
                final Map<String, Object> entries = restoreFromString(brc.toString(), TypeRef.MAP);
                token = (String) entries.get("token");
                if (StringUtils.isEmpty(token)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    tokenDetails[0] = token;
                    tokenDetails[1] = now + TimeUnit.HOURS.toMillis(23);
                    try {
                        final String payload = new Regex(token, "^.*?\\.([^\\.]+)").getMatch(0);
                        final String jsonString = new String(Base64.decodeFast(payload.replace("-", "+").replace("_", "/")), "UTF-8");
                        final Map<String, Object> json = restoreFromString(jsonString, TypeRef.MAP);
                        final Number iat = (Number) json.get("iat");
                        final Number exp = (Number) json.get("exp");
                        if (iat != null && exp != null) {
                            // minimum 1 hour
                            final long expireIn = Math.max(60 * 60, exp.longValue() - iat.longValue());
                            tokenDetails[1] = now + expireIn * 1000l;
                        }
                    } catch (Exception e) {
                        logger.log(e);
                    }
                }
            }
            return token;
        }
    }

    private Request getView(final Browser br, final String token, String fid) throws Exception {
        final GetRequest request = br.createGetRequest("https://api.redgifs.com/v2/gifs/" + fid + "?views=yes");
        request.getHeaders().put(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://www.redgifs.com");
        request.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, "https://www.redgifs.com/");
        request.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + token);
        br.getPage(request);
        return request;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link, boolean isDownload) throws Exception {
        final String fid = getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, "JDownloader");
        br.setAllowedResponseCodes(new int[] { 500 });
        final String firstToken = getTemporaryToken(br, null);
        Request view = getView(br, firstToken, fid);
        if (view.getHttpConnection().getResponseCode() == 401) {
            final String nextToken = getTemporaryToken(br, firstToken);
            if (!StringUtils.equals(firstToken, nextToken)) {
                view = getView(br, nextToken, fid);
            }
        }
        if (view.getHttpConnection().getResponseCode() == 401) {
            failSafeTemporaryTokenIssue.set(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> response = restoreFromString(view.getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> gif = (Map<String, Object>) response.get("gif");
        final Map<String, Object> user = (Map<String, Object>) response.get("user");// must be requested via &users=yes in /gifs/ request
        final Map<String, Object> urls = (Map<String, Object>) gif.get("urls");
        String url = (String) urls.get("hd");
        if (url == null) {
            url = (String) urls.get("sd");
        }
        String ext = getFileNameExtensionFromURL(url, ".mp4");
        final String gfyName = (String) gif.get("id");
        final String username = (String) gif.get("userName");
        String filename = username;
        final Number createDate = (Number) gif.get("createDate");
        if (createDate != null) {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            filename = sdf.format(new Date(createDate.longValue() * 1000)) + "_" + filename;
        }
        /* fid is used as fallback-title so in this case we don't want to have it twice in our filename! */
        if (gfyName != null) {
            if (!StringUtils.equalsIgnoreCase(gfyName, fid)) {
                filename += " - " + fid;
            }
            filename += " - " + gfyName + ext;
        }
        if (url != null) {
            if (link.getFinalFileName() == null || (link.getFinalFileName() != null && !StringUtils.endsWithCaseInsensitive(link.getFinalFileName(), ext))) {
                filename = filename + ext;
                filename = filename.replaceFirst("(?i)((\\.(webm|mp4|gif|jpe?g)))?" + Pattern.quote(ext) + "$", ext);
                link.setFinalFileName(filename);
            }
        } else {
            link.setName(filename);
        }
        if (link.getComment() == null) {
            final List<Object> tags = (List<Object>) gif.get("tags");
            if (tags != null) {
                final String description = StringUtils.join(tags.toArray(new Object[0]), " ") + " Porn GIF by " + username;
                link.setComment(description);
            }
        }
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            this.dllink = url;
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_HOST, LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "redgifs.com" };
    }

    @Override
    public void resetDownloadlink(DownloadLink downloadLink) {
        super.resetDownloadlink(downloadLink);
        failSafeTemporaryTokenIssue.set(false);
    }

    @Override
    public String getAGBLink() {
        return "https://www.redgifs.com/terms";
    }
}