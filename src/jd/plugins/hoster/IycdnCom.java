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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class IycdnCom extends PluginForHost {
    public IycdnCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://www." + getHost() + "/vip.php");
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "iycdn.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public String rewriteHost(final String host) {
        /* This filehost is frequently changing its domain which is why we need this. */
        /* 2023-12-01: Main domain has changed to xunniuwp.com */
        /* 2024-03-18: New main domain: xunniufxpan.com */
        return this.rewriteHost(getPluginDomains(), host);
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:file|down)\\-(\\d+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/about.php?action=help";
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return false;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    protected String getDownloadModeDirectlinkProperty(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return "freelink2";
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return "premlink";
        } else {
            /* Free(anonymous) and unknown account type */
            return "freelink";
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // if (account != null) {
        // this.login(account, false);
        // }
        br.getPage("https://www." + this.getHost() + "/file-" + fid + ".html");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*文件不存在或已删除")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("/file-" + fid + "\\.html\\]([^\"<]+)\\[/url\\]").getMatch(0);
        String filesize = br.getRegex(">\\s*文件大小\\s*:([^<]+)").getMatch(0);
        if (filename != null) {
            /* Set final filename here because server filenames are bad. */
            link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            filesize = Encoding.htmlDecode(filesize);
            filesize += "b";
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        final String fid = getFID(link);
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        final HashSet<String> mirrors = new HashSet<String>();
        if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl: " + storedDirecturl);
            mirrors.add(storedDirecturl);
            /*
             * Important! Correct Referer header needs to be present otherwise previously generated directurls will just redirect to
             * main-page.
             */
            br.getHeaders().put("Referer", link.getPluginPatternMatcher());
        } else {
            requestFileInformation(link, account);
            if (br.containsHTML("action=get_vip_fl")) {
                /* Premium account */
                br.postPage("/ajax.php", "action=get_vip_fl&file_id=" + this.getFID(link));
                final String urls_text = br.getRegex("true\\|(http.+)").getMatch(0);
                if (urls_text == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String[] thismirrors = urls_text.split("\\|");
                for (final String mirror : thismirrors) {
                    mirrors.add(mirror);
                }
            } else {
                /* Free account / No account */
                final boolean skipWaittime = true;
                final boolean skipCaptcha = true;
                if (!skipWaittime) {
                    /* 2019-09-12: Defaultvalue = 50 */
                    int wait = 0;
                    final String waittime = br.getRegex("var\\s*secs\\s*=\\s*(\\d+);").getMatch(0);
                    if (waittime != null) {
                        wait = Integer.parseInt(waittime);
                    } else {
                        logger.warning("Failed to find any pre download waittime value");
                    }
                    if (wait > 180) {
                        /* High waittime --> Reconnect is faster than waiting :) */
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
                    }
                    if (wait > 0) {
                        this.sleep(wait * 1001l, link);
                    } else {
                        logger.info("No pre download waittime needed");
                    }
                }
                if (br.containsHTML("/down-" + fid)) {
                    br.getPage("/down-" + fid + ".html");
                }
                String action = br.getRegex("url\\s*:\\s*'([^\\']+)'").getMatch(0);
                if (action == null) {
                    action = "ajax.php";
                }
                if (!action.startsWith("/")) {
                    action = "/" + action;
                }
                final Browser ajax = this.br.cloneBrowser();
                ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                boolean failed = true;
                int counter = -1;
                if ((br.containsHTML("imagecode\\.php") || true) && !skipCaptcha) {
                    do {
                        counter++;
                        final String code = getCaptchaCode("/imagecode.php?t=" + System.currentTimeMillis(), link);
                        ajax.postPage(action, "action=check_code&code=" + Encoding.urlEncode(code));
                        if (ajax.toString().equals("false")) {
                            continue;
                        }
                        failed = false;
                        break;
                    } while (failed && counter <= 10);
                    if (failed) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    // if (down2_url != null) {
                    // this.br.getHeaders().put("Referer", down2_url);
                    // }
                    // /* If we don't wait for some seconds here, the continue_url will redirect us to the main url!! */
                    // this.sleep(5 * 1001l, downloadLink);
                }
                ajax.postPage(action, "action=load_down_addr2&file_id=" + fid);
                final String[] thismirrors = ajax.getRegex("\"([^\"]*dl2?\\.php[^\"]+)\"").getColumn(0);
                if (thismirrors == null || thismirrors.length == 0) {
                    if (br.containsHTML("vip\\.php")) {
                        /* 2019-09-12: They might even display 4-5 mirrors here but none of them is for freeusers! */
                        throw new AccountRequiredException();
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                for (final String mirror : thismirrors) {
                    mirrors.add(mirror);
                }
            }
            if (mirrors.isEmpty()) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        int index = 0;
        for (final String mirror : mirrors) {
            logger.info("Trying mirror " + (index + 1) + "/" + mirrors.size() + " : " + mirror);
            final boolean isLastMirror = index == mirrors.size() - 1;
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, mirror, this.isResumeable(link, account), this.getMaxChunks(account));
                if (this.looksLikeDownloadableContent(dl.getConnection())) {
                    /* Success */
                    break;
                } else {
                    br.followConnection(true);
                    if (br.getURL().endsWith("/503.html")) {
                        /* Errorcode 503 with http code 200 */
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You are only allowed to download one file at the same time. Please complete the current download before trying to download other files.", 5 * 60 * 1000l);
                    }
                    if (dl.getConnection().getResponseCode() == 403) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 503) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 5 * 60 * 1000l);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Mirror " + (index + 1) + " is not downloadable");
                    }
                }
            } catch (final Exception e) {
                if (storedDirecturl != null) {
                    logger.log(e);
                    link.removeProperty(directlinkproperty);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
                } else if (isLastMirror) {
                    throw e;
                }
            }
            /* Try next mirror */
            index++;
            continue;
        }
        if (storedDirecturl == null) {
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }
    // @Override
    // public int getMaxSimultanPremiumDownloadNum() {
    // return Integer.MAX_VALUE;
    // }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PhpDisk;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}