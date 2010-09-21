//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.HTACCESSController;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

/**
 * TODO: Remove after next big update of core to use the public static methods!
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "DirectHTTP", "http links" }, urls = { "directhttp://.+", "https?viajd://[\\d\\w\\.:\\-@]*/.*\\.(3gp|7zip|7z|abr|ac3|ai|aiff|aif|aifc|au|avi|bin|bz2|cbr|cbz|ccf|cue|dta|deb|divx|djvu|dlc|dmg|doc|docx|dot|eps|exe|ff|flv|f4v|gif|gz|iwd|iso|java|jar|jpg|jpeg|jdeatme|load|mws|mv|m4v|m4a|mkv|mp2|mp3|mp4|mov|movie|mpeg|mpe|mpg|msi|msu|nfo|oga|ogg|ogv|otrkey|pkg|png|pdf|ppt|pptx|pps|ppz|pot|psd|qt|rm|rmvb|rar|rnd|r\\d+|rpm|run|rsdf|rtf|sh|srt|snd|sfv|swf|tar|tif|tiff|ts|txt|viv|vivo|vob|wav|wmv|xla|xls|xpi|zip|z\\d+|_[_a-z]{2}|\\d+$)" }, flags = { 0, 0 })
public class DirectHTTP extends PluginForHost {

    public static class Recaptcha {

        private final Browser br;
        private String        challenge;
        private String        server;
        private String        captchaAddress;
        private String        id;
        private Browser       rcBr;
        private Form          form;

        public Recaptcha(final Browser br) {
            this.br = br;
        }

        public File downloadCaptcha(final File captchaFile) throws IOException {
            /* follow redirect needed as google redirects to another domain */
            this.rcBr.setFollowRedirects(true);
            Browser.download(captchaFile, this.rcBr.openGetConnection(this.captchaAddress));
            return captchaFile;
        }

        public String getCaptchaAddress() {
            return this.captchaAddress;
        }

        public String getChallenge() {
            return this.challenge;
        }

        public Form getForm() {
            return this.form;
        }

        public String getId() {
            return this.id;
        }

        public String getServer() {
            return this.server;
        }

        public void load() throws IOException, PluginException {
            this.rcBr = this.br.cloneBrowser();
            /* follow redirect needed as google redirects to another domain */
            this.rcBr.setFollowRedirects(true);
            this.rcBr.getPage("http://api.recaptcha.net/challenge?k=" + this.id);
            this.challenge = this.rcBr.getRegex("challenge.*?:.*?'(.*?)',").getMatch(0);
            this.server = this.rcBr.getRegex("server.*?:.*?'(.*?)',").getMatch(0);
            if (this.challenge == null || this.server == null) {
                JDLogger.getLogger().severe("Recaptcha Module fails: " + this.rcBr.getHttpConnection());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.captchaAddress = this.server + "image?c=" + this.challenge;
        }

        public void parse() throws IOException, PluginException {
            final Form[] forms = this.br.getForms();
            this.form = null;
            for (final Form f : forms) {
                if (f.getInputField("recaptcha_challenge_field") != null) {
                    this.form = f;
                    break;
                }
            }
            if (this.form == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                this.id = this.form.getRegex("k=(.*?)\"").getMatch(0);
                if (this.id == null || this.id.equals("")) id = br.getRegex("\\?k=([A-Za-z0-9%]+)\"").getMatch(0);
                if (this.id == null || this.id.equals("")) {
                    Plugin.logger.warning("reCaptcha ID couldn't be found...");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    this.id = this.id.replace("&amp;error=1", "");
                }
            }
        }

        public void setCaptchaAddress(final String captchaAddress) {
            this.captchaAddress = captchaAddress;
        }

        public void setChallenge(final String challenge) {
            this.challenge = challenge;
        }

        public Browser setCode(final String code) throws Exception {
            // <textarea name="recaptcha_challenge_field" rows="3"
            // cols="40"></textarea>\n <input type="hidden"
            // name="recaptcha_response_field" value="manual_challenge"/>
            this.form.put("recaptcha_challenge_field", this.challenge);
            this.form.put("recaptcha_response_field", Encoding.urlEncode(code));
            this.br.submitForm(this.form);
            return this.br;
        }

        public void setForm(final Form form) {
            this.form = form;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public void setServer(final String server) {
            this.server = server;
        }
    }

    private static final String JDL_PREFIX     = "jd.plugins.hoster.DirectHTTP.";

    public static final String  ENDINGS        = "\\.(3gp|7zip|7z|abr|ac3|ai|aiff|aif|aifc|au|avi|bin|bz2|cbr|cbz|ccf|cue|dta|deb|divx|djvu|dlc|dmg|doc|docx|dot|eps|exe|ff|flv|f4v|gif|gz|iwd|iso|java|jar|jpg|jpeg|jdeatme|load|mws|mw|m4v|m4a|mkv|mp2|mp3|mp4|mov|movie|mpeg|mpe|mpg|msi|msu|nfo|oga|ogg|ogv|otrkey|pkg|png|pdf|ppt|pptx|pps|ppz|pot|psd|qt|rm|rmvb|rar|rnd|r\\d+|rpm|run|rsdf|rtf|sh|srt|snd|sfv|swf|tar|tif|tiff|ts|txt|viv|vivo|vob|wav|wmv|xla|xls|xpi|zip|z\\d+|_[_a-z]{2}|\\d+$)";
    public static final String  NORESUME       = "nochunkload";
    public static final String  NOCHUNKS       = "nochunk";
    public static final String  FORCE_NORESUME = "forcenochunkload";
    public static final String  FORCE_NOCHUNKS = "forcenochunk";
    public static final String  TRY_ALL        = "tryall";

    /**
     * TODO: Remove with next major-update!
     */
    public static ArrayList<String> findUrls(final String source) {
        /* TODO: better parsing */
        /* remove tags!! */
        final ArrayList<String> ret = new ArrayList<String>();
        try {

            for (final String link : new Regex(source, "((https?|ftp):((//)|(\\\\\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)(\n|\r|$|<|\")").getColumn(0)) {
                try {
                    new URL(link);
                    if (!ret.contains(link)) {
                        ret.add(link);
                    }
                } catch (final MalformedURLException e) {

                }
            }
        } catch (final Exception e) {
            JDLogger.exception(e);
        }
        return DirectHTTP.removeDuplicates(ret);
    }

    /**
     * Returns the annotations flags array
     */
    public static int[] getAnnotationFlags() {
        return new int[] { 0, 0 };
    }

    /**
     * Returns the annotations names array
     */
    public static String[] getAnnotationNames() {
        return new String[] { "DirectHTTP", "http links" };
    }

    /**
     * Returns the annotation pattern array
     */
    public static String[] getAnnotationUrls() {
        return new String[] { "directhttp://.+", "https?viajd://[\\d\\w\\.:\\-@]*/.*" + DirectHTTP.ENDINGS };
    }

    /**
     * TODO: Remove with next major-update!
     */
    public static ArrayList<String> removeDuplicates(final ArrayList<String> links) {
        final ArrayList<String> tmplinks = new ArrayList<String>();
        if (links == null || links.size() == 0) { return tmplinks; }
        for (final String link : links) {
            if (link.contains("...")) {
                final String check = link.substring(0, link.indexOf("..."));
                String found = link;
                for (final String link2 : links) {
                    if (link2.startsWith(check) && !link2.contains("...")) {
                        found = link2;
                        break;
                    }
                }
                if (!tmplinks.contains(found)) {
                    tmplinks.add(found);
                }
            } else {
                tmplinks.add(link);
            }
        }
        return tmplinks;
    }

    private String contentType       = "";

    private String customFavIconHost = null;

    public DirectHTTP(final PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    private void BasicAuthfromURL(final DownloadLink link) {
        String url = null;
        final String basicauth = new Regex(link.getDownloadURL(), "http.*?/([^/]{1}.*?)@").getMatch(0);
        if (basicauth != null && basicauth.contains(":")) {
            /* https */
            url = new Regex(link.getDownloadURL(), "https.*?@(.+)").getMatch(0);
            if (url != null) {
                link.setUrlDownload("https://" + url);
            } else {
                /* http */
                url = new Regex(link.getDownloadURL(), "http.*?@(.+)").getMatch(0);
                if (url != null) {
                    link.setUrlDownload("http://" + url);
                } else {
                    Plugin.logger.severe("Could not parse basicAuth from " + link.getDownloadURL());
                }
            }
            HTACCESSController.getInstance().add(link.getDownloadURL(), Encoding.Base64Encode(basicauth));
        }
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().startsWith("directhttp")) {
            link.setUrlDownload(link.getDownloadURL().replaceAll("^directhttp://", ""));
        } else {
            link.setUrlDownload(link.getDownloadURL().replaceAll("httpviajd://", "http://").replaceAll("httpsviajd://", "https://"));
            /* this extension allows to manually add unknown extensions */
            link.setUrlDownload(link.getDownloadURL().replaceAll("\\.jdeatme$", ""));
        }
        this.BasicAuthfromURL(link);
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    private String getBasicAuth(final DownloadLink link) {
        String url;
        if (link.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
            url = link.getHost();
        } else {
            url = link.getBrowserUrl();
        }
        String username = null;
        String password = null;
        try {
            username = Plugin.getUserInput(JDL.LF(DirectHTTP.JDL_PREFIX + "username", "Username (BasicAuth) for %s", url), link);
            password = Plugin.getUserInput(JDL.LF(DirectHTTP.JDL_PREFIX + "password", "Password (BasicAuth) for %s", url), link);
        } catch (final Exception e) {
            return null;
        }
        return "Basic " + Encoding.Base64Encode(username + ":" + password);
    }

    public String getCustomFavIconURL() {
        if (this.customFavIconHost != null) { return this.customFavIconHost; }
        return null;
    }

    @Override
    public String getFileInformationString(final DownloadLink parameter) {
        return "(" + this.contentType + ")" + parameter.getName();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /**
     * TODO: can be removed with next major update cause of recaptcha change
     */
    public Recaptcha getReCaptcha(final Browser br) {
        return new Recaptcha(br);
    }

    public String getSessionInfo() {
        if (this.customFavIconHost != null) { return this.customFavIconHost; }
        return "";
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.requestFileInformation(downloadLink);
        final String auth = this.br.getHeaders().get("Authorization");
        /*
         * replace with br.setCurrentURL(null); in future (after 0.9)
         */
        this.br = new Browser();/* needed to clean referer */
        if (auth != null) {
            this.br.getHeaders().put("Authorization", auth);
        }
        /* workaround to clear referer */
        this.br.setFollowRedirects(true);
        this.br.setDebug(true);
        boolean resume = true;
        int chunks = 0;

        if (downloadLink.getBooleanProperty(DirectHTTP.NORESUME, false) || downloadLink.getBooleanProperty(DirectHTTP.FORCE_NORESUME, false)) {
            resume = false;
        }
        if (downloadLink.getBooleanProperty(DirectHTTP.NOCHUNKS, false) || downloadLink.getBooleanProperty(DirectHTTP.FORCE_NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }
        this.setCustomHeaders(this.br, downloadLink);
        if (downloadLink.getStringProperty("post", null) != null) {
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL(), downloadLink.getStringProperty("post", null), resume, chunks);
        } else {
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL(), resume, chunks);
        }
        if (!this.dl.startDownload()) {
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(DirectHTTP.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(DirectHTTP.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(DirectHTTP.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(DirectHTTP.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    private URLConnectionAdapter prepareConnection(final Browser br, final DownloadLink downloadLink) throws IOException {
        URLConnectionAdapter urlConnection = null;
        this.setCustomHeaders(br, downloadLink);
        if (downloadLink.getStringProperty("post", null) != null) {
            urlConnection = br.openPostConnection(downloadLink.getDownloadURL(), downloadLink.getStringProperty("post", null));
        } else {
            urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
        }
        return urlConnection;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException {
        this.setBrowserExclusive();
        /* disable gzip, because current downloadsystem cannot handle it correct */
        this.br.getHeaders().put("Accept-Encoding", "");
        String basicauth = HTACCESSController.getInstance().get(downloadLink.getDownloadURL());
        if (basicauth == null) {
            basicauth = downloadLink.getStringProperty("pass", null);
            if (basicauth != null) {
                basicauth = "Basic " + Encoding.Base64Encode(basicauth);
            }
        }
        if (basicauth != null) {
            this.br.getHeaders().put("Authorization", basicauth);
        }
        this.br.setFollowRedirects(true);
        URLConnectionAdapter urlConnection = null;
        try {
            urlConnection = this.prepareConnection(this.br, downloadLink);
            if (urlConnection.getResponseCode() == 401) {
                if (basicauth != null) {
                    HTACCESSController.getInstance().remove(downloadLink.getDownloadURL());
                }
                urlConnection.disconnect();
                basicauth = this.getBasicAuth(downloadLink);
                if (basicauth == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.httplinks.errors.basicauthneeded", "BasicAuth needed")); }
                this.br.getHeaders().put("Authorization", basicauth);
                urlConnection = this.prepareConnection(this.br, downloadLink);
                if (urlConnection.getResponseCode() == 401) {
                    urlConnection.disconnect();
                    HTACCESSController.getInstance().remove(downloadLink.getDownloadURL());
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.httplinks.errors.basicauthneeded", "BasicAuth needed"));
                } else {
                    HTACCESSController.getInstance().add(downloadLink.getDownloadURL(), basicauth);
                }
            }
            if (urlConnection.getResponseCode() == 404 || !urlConnection.isOK()) {
                urlConnection.disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* if final filename already set, do not change */
            if (downloadLink.getFinalFileName() == null) {
                downloadLink.setFinalFileName(Plugin.getFileNameFromHeader(urlConnection));
            }
            downloadLink.setDownloadSize(urlConnection.getLongContentLength());
            this.contentType = urlConnection.getContentType();
            if (this.contentType.startsWith("text/html") && downloadLink.getBooleanProperty(TRY_ALL, false) == false) {
                /* jd does not want to download html content! */
                /* if this page does redirect via js/html, try to follow */
                this.br.followConnection();
                /* search urls */
                /*
                 * TODO: Change to org.appwork.utils.parser.HTMLParser.findUrls
                 * with next major-udpate
                 */
                final ArrayList<String> follow = DirectHTTP.findUrls(this.br.toString());
                /*
                 * if we already tried htmlRedirect or not exactly one link
                 * found, throw File not available
                 */
                if (follow.size() != 1 || downloadLink.getBooleanProperty("htmlRedirect", false)) { return AvailableStatus.FALSE; }
                /* found one valid url */
                downloadLink.setUrlDownload(follow.get(0).trim());
                /* we set property here to avoid loops */
                downloadLink.setProperty("htmlRedirect", true);
                return downloadLink.getAvailableStatus();
            } else {
                urlConnection.disconnect();
            }
            return AvailableStatus.TRUE;
        } catch (final PluginException e2) {
            throw e2;
        } catch (final Exception e) {
            JDLogger.exception(e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.setProperty(DirectHTTP.NORESUME, false);
        link.setProperty(DirectHTTP.NOCHUNKS, false);
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        this.config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LISTCONTROLLED, HTACCESSController.getInstance(), JDL.L("plugins.http.htaccess", "List of all HTAccess passwords. Each line one password.")));
    }

    private void setCustomHeaders(final Browser br, final DownloadLink downloadLink) {
        /* allow customized headers, eg useragent */
        final ArrayList<String[]> custom = downloadLink.getGenericProperty("customHeader", new ArrayList<String[]>());
        if (custom != null && custom.size() > 0) {
            for (final String[] header : custom) {
                br.getHeaders().put(header[0], header[1]);
            }
        }
        /*
         * seems like flashgot catches the wrong referer and some downloads do
         * not work then, we do not set referer as a workaround
         */
        // if (downloadLink.getStringProperty("referer", null) != null) {
        // br.getHeaders().put("Referer",
        // downloadLink.getStringProperty("referer", null));
        // }
        if (downloadLink.getStringProperty("cookies", null) != null) {
            br.getCookies(downloadLink.getDownloadURL()).add(Cookies.parseCookies(downloadLink.getStringProperty("cookies", null), Browser.getHost(downloadLink.getDownloadURL()), null));
        }
    }

    public void setDownloadLink(final DownloadLink link) {
        try {
            super.setDownloadLink(link);
            this.customFavIconHost = Browser.getHost(new URL(link.getDownloadURL()));
        } catch (final Throwable e) {
        }
    }

}
