//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class EvoloadIo extends PluginForHost {
    public EvoloadIo(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://evoload.io/terms.html";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "evoload.io" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:f|v)/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fid = this.getFID(link);
        if (fid != null) {
            /* Embed URLs --> Normal URLs */
            link.setPluginPatternMatcher("https://" + this.getHost() + "/f/" + fid);
            link.setLinkID(getHost() + "://" + fid);
        }
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = -2;
    private static final int     FREE_MAXDOWNLOADS = 1;
    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

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

    private boolean usedAPIDuringAvailablecheck = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* They're hosting video content only. */
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 500 });
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2020-12-14: API works without apikey though a key is required according to their docs lol: https://evoload.dev/ */
        final boolean tryAPI = true;
        if (tryAPI) {
            try {
                br.getPage("https://" + this.getHost() + "/v1/EvoAPI/-/file-check/" + this.getFID(link));
                usedAPIDuringAvailablecheck = true;
                /* 2020-12-14: E.g. offline: {"status":400,"msg":"File does not exists!"} */
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final String status = (String) entries.get("status");
                if (status != null && !status.equals("Online")) {
                    return AvailableStatus.FALSE;
                } else {
                    final String filename = (String) entries.get("original_name");
                    final long size = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
                    if (!StringUtils.isEmpty(filename)) {
                        link.setFinalFileName(filename);
                    }
                    if (size > 0) {
                        link.setDownloadSize(size);
                    }
                    return AvailableStatus.TRUE;
                }
            } catch (final Throwable e) {
                logger.log(e);
                logger.info("API Availablecheck failed");
            }
        }
        final boolean canDetermineRealStatusWithoutCaptcha = false;
        // String filename = br.getRegex("title>Evoload - Download ([^<>\"]+)</title>").getMatch(0);
        // if (StringUtils.isEmpty(filename)) {
        // filename = br.getRegex("class=\"kt-subheader__title\">([^<>\"]+)<").getMatch(0);
        // }
        // if (!StringUtils.isEmpty(filename)) {
        // filename = Encoding.htmlDecode(filename).trim();
        // link.setName(filename);
        // }
        String filesize = br.getRegex("File Size\\s*:\\s*<small>\\s*(\\d+[^<>\"]+)<").getMatch(0);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (canDetermineRealStatusWithoutCaptcha) {
            if (!link.isNameSet()) {
                link.setName(this.getFID(link));
            }
            return AvailableStatus.TRUE;
        } else {
            return AvailableStatus.UNCHECKABLE;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        final AvailableStatus status = requestFileInformation(link);
        if (status == AvailableStatus.FALSE) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (this.usedAPIDuringAvailablecheck) {
                br.getPage(link.getPluginPatternMatcher());
            }
            final String recaptchaV2Response;
            final String reCaptchaKey = br.getRegex("recaptcha/api\\.js\\?render=([^<>\"]+)\"").getMatch(0);
            if (reCaptchaKey != null) {
                recaptchaV2Response = getCaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey).getToken();
            } else {
                recaptchaV2Response = getCaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            }
            final Map<String, Object> postdata = new HashMap<String, Object>();
            postdata.put("code", this.getFID(link));
            postdata.put("token", recaptchaV2Response);
            br.getHeaders().put("Accept", "application/json, text/plain, */*");
            final boolean doExtraRequest = false;
            if (doExtraRequest) {
                br.postPageRaw("/SecureMeta", JSonStorage.serializeToJson(postdata));
                final String xstatus = PluginJSonUtils.getJson(this.br, "xstatus");
                if (!StringUtils.isEmpty(xstatus)) {
                    /* E.g. status "del" */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String original_name = PluginJSonUtils.getJson(this.br, "original_name");
                if (!StringUtils.isEmpty(original_name)) {
                    link.setFinalFileName(original_name);
                }
            }
            br.postPageRaw("/SecurePlayer", JSonStorage.serializeToJson(postdata));
            final String xstatus = PluginJSonUtils.getJson(this.br, "xstatus");
            if (!StringUtils.isEmpty(xstatus)) {
                /* E.g. status "del" */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = PluginJSonUtils.getJson(br, "src");
            /* 2020-12-14: We are unable to get any file information during linkcheck which is why we try to set the name here. */
            final String name = PluginJSonUtils.getJson(this.br, "name");
            if (!StringUtils.isEmpty(name) && link.getFinalFileName() == null) {
                link.setFinalFileName(name);
            }
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503 too many connections", 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    protected CaptchaHelperHostPluginRecaptchaV2 getCaptchaHelperHostPluginRecaptchaV2(PluginForHost plugin, Browser br, final String reCaptchaKey) throws PluginException {
        return new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey) {
            @Override
            public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                return TYPE.INVISIBLE;
            }
        };
    }

    protected CaptchaHelperHostPluginRecaptchaV2 getCaptchaHelperHostPluginRecaptchaV2(PluginForHost plugin, Browser br) throws PluginException {
        return this.getCaptchaHelperHostPluginRecaptchaV2(plugin, br, null);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    link.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        }
        /* Premium accounts do not have captchas */
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}