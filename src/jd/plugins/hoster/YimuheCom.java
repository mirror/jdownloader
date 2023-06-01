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

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "yimuhe.com" }, urls = { "https?://(?:www\\.)?yimuhe\\.com/(?:file|down)\\-(\\d+)\\.html" })
public class YimuheCom extends PluginForHost {
    public YimuheCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://" + getHost() + "/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 1;
    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* 72bbb.com definitly returns 404 on url offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String sha1 = this.br.getRegex("(?i)SHA1:([^<>]+)<").getMatch(0);
        final String md5 = this.br.getRegex("(?i)MD5:([^<>]+)<").getMatch(0);
        String filename = br.getRegex("\\.AddFavorite\\(\\'[^\\']*?\\',\\'([^<>\"\\']+)\\'\\)").getMatch(0);
        String filesize = br.getRegex("文件大小：\\s*</span><span class=\"rightnone\">([^<>]+)<").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("文件大小：\\s*</span><span class=\"rightnone\">([^<>]+)<").getMatch(0);
        }
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (sha1 != null) {
            link.setSha1Hash(sha1);
        }
        if (md5 != null) {
            link.setSha1Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            final String fid = this.getFID(link);
            this.br.getPage("/down-" + fid + ".html");
            final String js_source = this.br.getRegex("down_file\\(([^<>\"]+)\\)").getMatch(0);
            final String[] js_params = js_source.split("','");
            if (js_params.length < 6) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Correct values */
            for (int i = 0; i < js_params.length; i++) {
                js_params[i] = js_params[i].replace("'", "");
            }
            /*
             * 2022-01-31: This captcha can be very hard to read sometimes! Also it seems like the answer is also verified case-sensitive!
             */
            final String code = this.getCaptchaCode("/n_downcode.php", link);
            if (code == null || !code.matches("[A-Za-z0-9]{4}")) {
                logger.info("Invalid captcha-answer format");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final UrlQuery query1 = new UrlQuery();
            query1.add("action", "yz");
            query1.add("id", fid);
            query1.add("code", code);
            this.br.postPage("/n_downcode.php", query1);
            if (!this.br.toString().equals("1")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            final String userlogin = js_params[1];
            final String p = js_params[5];
            final String file_key = js_params[2];
            final String file_name = js_params[3];
            final String ser = js_params[4];
            final UrlQuery query2 = new UrlQuery();
            query2.add("file_id", fid);
            query2.add("userlogin", Encoding.urlEncode(userlogin));
            query2.add("p", p);
            query2.add("types", "http");
            query2.add("file_key", file_key);
            query2.add("file_name", Encoding.urlEncode(file_name));
            query2.add("ser", ser);
            this.br.getPage("/n_dd.php?" + query2.toString());
            dllink = br.getRegex("(http[^<>\"]+/downfile/[^<>\"]+)").getMatch(0);
            if (dllink == null) {
                if (br.containsHTML("(?i)alert\\('此文件已被删除")) {
                    /* Super rare case. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PhpDisk;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}