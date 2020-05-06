package org.jdownloader.plugins.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class ImgShotCore extends antiDDoSForHost {
    public ImgShotCore(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(super.getPurchasePremiumURL());
    }

    // public static List<String[]> getPluginDomains() {
    // final List<String[]> ret = new ArrayList<String[]>();
    // // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
    // ret.add(new String[] { "imgdew.com" });
    // return ret;
    // }
    //
    // public static String[] getAnnotationNames() {
    // return buildAnnotationNames(getPluginDomains());
    // }
    //
    // @Override
    // public String[] siteSupportedNames() {
    // return buildSupportedNames(getPluginDomains());
    // }
    //
    // public static String[] getAnnotationUrls() {
    // return ImgShotCore.buildAnnotationUrls(getPluginDomains());
    // }
    // @Override
    // public String rewriteHost(String host) {
    // return this.rewriteHost(getPluginDomains(), host, new String[0]);
    // }
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
        String fid = new Regex(link.getPluginPatternMatcher(), "dlimg\\.php\\?id=([a-z0-9]+)$").getMatch(0);
        if (fid == null) {
            fid = new Regex(link.getPluginPatternMatcher(), "/img\\-([a-z0-9]+)").getMatch(0);
        }
        return fid;
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String fid_of_official_downloadurl = new Regex(link.getPluginPatternMatcher(), "dlimg\\.php\\?id=([a-z0-9]+)$").getMatch(0);
        if (fid_of_official_downloadurl != null) {
            /* Respect original protocol & host */
            final String urlpart = new Regex(link.getPluginPatternMatcher(), "(https?://[^/]+)/.+").getMatch(0);
            /*
             * All hosts require html ending. Some will also provide URLs without html ending but all of them will redirect to URL with html
             * ending.
             */
            link.setPluginPatternMatcher(urlpart + "/img-" + fid_of_official_downloadurl + ".html");
        }
    }

    public static final String getDefaultAnnotationPatternPart() {
        return "/(?:img\\-[a-z0-9\\-_]+(?:\\.jpe?g)?(?:\\.html)?|dlimg\\.php\\?id=[a-z0-9]+)";
    }

    public static String[] buildAnnotationUrls(List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + ImgShotCore.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://" + this.getHost() + "/page-TOS.html";
    }

    /**
     * Override to disable filesize-check! </br> default = true
     */
    protected boolean checkFilesize() {
        return true;
    }

    /**
     * Enforce official download via "dlimg.php"? </br> A lot of hosts have it enabled although they do not display a download button on
     * their website! </br> default = true </br> Example official download supported but broken serverside: imagedecode.com, imageteam.org
     * </br> Example official download working fine: imgwallet.com, damimage.com, imgadult.com, imgtornado.com, acidimg.com
     */
    protected boolean enforceOfficialDownloadURL() {
        return true;
    }

    protected boolean supportsResume() {
        return false;
    }

    protected String getDefaultFileExtension() {
        return ".jpg";
    }

    protected int maxChunks() {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    protected AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        br.setFollowRedirects(true);
        this.setBrowserExclusive();
        /* TODO: Check whether or not we need a random User-Agent */
        // br.getHeaders().put("User-Agent", jd.plugins.components.UserAgents.stringUserAgent());
        getPage(link.getPluginPatternMatcher());
        if (this.isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = this.getFID(link);
        String filename = regexFilename();
        if (StringUtils.isEmpty(filename) || filename.equalsIgnoreCase(br.getHost())) {
            /* Fallback */
            filename = fid;
        } else {
            filename = filename.trim();
            filename = fid + "_" + filename;
        }
        if (checkFilesize() && !isDownload) {
            final String dllink = this.getDllink(link, filename, false);
            if (!StringUtils.isEmpty(dllink) && link.getView().getBytesTotal() <= 0) {
                logger.info("Final downloadurl: " + dllink);
                URLConnectionAdapter con = null;
                try {
                    con = openAntiDDoSRequestConnection(br, br.createHeadRequest(dllink));
                    if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                        // server_issues = true;
                    } else {
                        link.setDownloadSize(con.getLongContentLength());
                        /*
                         * Set filename with correct extension. Most content will be .jpg but sometimes there will be .png and .gif as well.
                         */
                        final String final_file_extension;
                        final String file_extension_from_server = getFileNameExtensionFromString(getFileNameFromHeader(con));
                        if (file_extension_from_server != null) {
                            final_file_extension = file_extension_from_server;
                        } else {
                            final_file_extension = getDefaultFileExtension();
                        }
                        if (!filename.endsWith(final_file_extension)) {
                            filename += final_file_extension;
                        }
                        link.setFinalFileName(filename);
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        } else {
            /* Sometimes extension is already given e.g. imgwallet.com */
            if (!filename.endsWith(getDefaultFileExtension())) {
                filename += getDefaultFileExtension();
            }
            /* Do not set final filename as we cannot know the real fileextension without final downloadlink! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    /**
     * Finds filename inside html code. </br>
     */
    protected String regexFilename() {
        /* 2019-10-29: E.g. imgadult.com, imgwallet.com */
        // return br.getRegex("<title>([^<>\"]*) \\|[^<>]+</title>").getMatch(0);
        /* '?' is required [tested with imgwallet.com "<title>016.jpg | ImgWallet.com | Upload & Earn Money Sharing Images</title>"] */
        return br.getRegex("<title>([^<>\"]*?) \\|[^<>]+</title>").getMatch(0);
    }

    protected boolean isOffline(final Browser br) {
        if (br.containsHTML(">Image Removed or Bad Link<|>This image has been removed") || br.getURL().contains("/noimage.php") || br.getHttpConnection().getResponseCode() == 404) {
            return true;
        }
        return false;
    }

    protected void handleContinueStep(final Browser br) throws Exception {
        /* general */
        if (br.containsHTML("imgContinue") || br.containsHTML("continue_to_image")) {
            postPage(br, br.getURL(), "imgContinue=Continue+to+image+...+");
        } else if (br.containsHTML("id=\"redirect\\-wait\"")) {
            /* E.g. imgadult.com, imgwallet.com, imgdrive.net */
            // br.getHeaders().put("Referer", "http://www.imgwallet.com/url.php?i=5");
            postPage(br, br.getURL(), "cti=1&ref=-&rc=0&bt=0&bw=gecko");
            /* Make sure that Referer is correct. */
            br.getHeaders().put("Referer", br.getURL());
            getPage(br, br.getURL());
        }
    }

    /**
     * Finds final downloadurl. This will prefer official downloadurls if present and valid. On successful check, this may also set final
     * filesize and filename.
     */
    private String getDllink(final DownloadLink link, String filename, final boolean isDownload) throws IOException, Exception {
        final String fid = this.getFID(this.getDownloadLink());
        /* Try to get official downloadurl first */
        String dllink = br.getRegex("(dlimg\\.php\\?id=" + fid + ")").getMatch(0);
        if (dllink != null) {
            logger.info("Found official downloadlink");
        } else if (enforceOfficialDownloadURL()) {
            logger.info("Failed to find official downloadlink but trying forced official downloadlink");
            dllink = "dlimg.php?id=" + fid;
        }
        if (!StringUtils.isEmpty(dllink)) {
            /*
             * Now check if that one is valid because even if a website does display an official downloadlink it does not necessarily mean
             * that it will work
             */
            URLConnectionAdapter con = null;
            final Browser brc = br.cloneBrowser();
            try {
                if (isDownload) {
                    this.dl = jd.plugins.BrowserAdapter.openDownload(brc, link, dllink, this.supportsResume(), this.maxChunks());
                    con = dl.getConnection();
                } else {
                    con = openAntiDDoSRequestConnection(brc, br.createHeadRequest(dllink));
                }
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    // server_issues = true;
                    dllink = null;
                    this.dl = null;
                    /* Some might redirect to '/noimage.php', some just display 404 or an empty page */
                    logger.info("Official downloadlink does NOT work");
                } else {
                    logger.info("Official downloadlink works");
                    link.setVerifiedFileSize(con.getLongContentLength());
                    if (filename != null) {
                        /*
                         * Set filename with correct extension. Most content will be .jpg but sometimes there will be .png and .gif as well.
                         */
                        final String final_file_extension;
                        final String file_extension_from_server = getFileNameExtensionFromString(getFileNameFromHeader(con));
                        if (file_extension_from_server != null) {
                            final_file_extension = file_extension_from_server;
                        } else {
                            final_file_extension = getDefaultFileExtension();
                        }
                        if (!filename.endsWith(final_file_extension)) {
                            filename += final_file_extension;
                        }
                        link.setFinalFileName(filename);
                    }
                    return dllink;
                }
            } catch (final Throwable e) {
                logger.info("Something went wrong during official downloadlink check");
            } finally {
                if (!isDownload) {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        handleContinueStep(br);
        dllink = br.getRegex("(\\'|\")(https?://([\\w\\-]+\\.)?" + Pattern.quote(br.getHost()) + "((?:/upload)?/big/|(?:/uploads)?/images/big)[^<>\"\\']*?)\\1").getMatch(1);
        return dllink;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        final String dllink = this.getDllink(link, null, true);
        if (dllink == null) {
            logger.warning("Failed to find final downloadurl");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl == null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.supportsResume(), this.maxChunks());
        }
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.ImageHosting_ImgShot;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}