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
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "DirectHTTP", "http links" }, urls = { "directhttp://.+", "https?viajd://[\\d\\w\\.:\\-@]*/.*\\.(3gp|7zip|7z|abr|ac3|ai|aiff|aif|aifc|au|avi|bin|bz2|cbr|cbz|ccf|cue|deb|divx|djvu|dlc|dmg|doc|docx|dot|eps|exe|ff|flv|f4v|gif|gz|iwd|iso|java|jar|jpg|jpeg|jdeatme|load|m4v|m4a|mkv|mp2|mp3|mp4|mov|movie|mpeg|mpe|mpg|msi|msu|nfo|oga|ogg|ogv|otrkey|pkg|png|pdf|ppt|pptx|pps|ppz|pot|psd|qt|rmvb|rar|r\\d+|rpm|run|rsdf|rtf|sh|srt|snd|sfv|swf|tar|tif|tiff|ts|txt|viv|vivo|vob|wav|wmv|xla|xls|xpi|zip|z\\d+|_[_a-z]{2}|\\d+)" }, flags = { 0, 0 })
public class DirectHTTP extends PluginForHost {

    public static final String ENDINGS = "\\.(3gp|7zip|7z|abr|ac3|ai|aiff|aif|aifc|au|avi|bin|bz2|cbr|cbz|ccf|cue|deb|divx|djvu|dlc|dmg|doc|docx|dot|eps|exe|ff|flv|f4v|gif|gz|iwd|iso|java|jar|jpg|jpeg|jdeatme|load|m4v|m4a|mkv|mp2|mp3|mp4|mov|movie|mpeg|mpe|mpg|msi|msu|nfo|oga|ogg|ogv|otrkey|pkg|png|pdf|ppt|pptx|pps|ppz|pot|psd|qt|rmvb|rar|r\\d+|rpm|run|rsdf|rtf|sh|srt|snd|sfv|swf|tar|tif|tiff|ts|txt|viv|vivo|vob|wav|wmv|xla|xls|xpi|zip|z\\d+|_[_a-z]{2}|\\d+)";

    public static final String NORESUME = "nochunkload";
    public static final String NOCHUNKS = "nochunk";
    public static final String FORCE_NORESUME = "forcenochunkload";
    public static final String FORCE_NOCHUNKS = "forcenochunk";

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
        return new String[] { "directhttp://.+", "https?viajd://[\\d\\w\\.:\\-@]*/.*" + ENDINGS };
    }

    /**
     * Returns the annotations flags array
     */
    public static int[] getAnnotationFlags() {
        return new int[] { 0, 0 };
    }

    private String contentType = "";

    private String customFavIconHost = null;

    public DirectHTTP(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    @Override
    public String getFileInformationString(DownloadLink parameter) {
        return "(" + contentType + ")" + parameter.getName();
    }

    public void setDownloadLink(DownloadLink link) {
        try {
            super.setDownloadLink(link);
            customFavIconHost = Browser.getHost(new URL(link.getDownloadURL()));
        } catch (Throwable e) {
        }
    }

    private String getBasicAuth(DownloadLink link) {
        String username = null;
        String password = null;
        try {
            username = getUserInput("Username(BasicAuth)", link);
            password = getUserInput("Password(BasicAuth)", link);
        } catch (Exception e) {
            return null;
        }
        return "Basic " + Encoding.Base64Encode(username + ":" + password);
    }

    private void BasicAuthfromURL(DownloadLink link) {
        String url = null;
        String basicauth = new Regex(link.getDownloadURL(), "http.*?/([^/]{1}.*?)@").getMatch(0);
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
                    logger.severe("Could not parse basicAuth from " + link.getDownloadURL());
                }
            }
            HTACCESSController.getInstance().add(link.getDownloadURL(), Encoding.Base64Encode(basicauth));
        }
    }

    private void setCustomHeaders(Browser br, DownloadLink downloadLink) {
        /* allow customized headers, eg useragent */
        ArrayList<String[]> custom = downloadLink.getGenericProperty("customHeader", new ArrayList<String[]>());
        if (custom != null && custom.size() > 0) {
            for (String[] header : custom) {
                br.getHeaders().put(header[0], header[1]);
            }
        }
        if (downloadLink.getStringProperty("referer", null) != null) {
            br.getHeaders().put("Referer", downloadLink.getStringProperty("referer", null));
        }
        if (downloadLink.getStringProperty("cookies", null) != null) {
            br.getCookies(downloadLink.getDownloadURL()).add(Cookies.parseCookies(downloadLink.getStringProperty("cookies", null), Browser.getHost(downloadLink.getDownloadURL()), null));
        }
    }

    private URLConnectionAdapter prepareConnection(Browser br, DownloadLink downloadLink) throws IOException {
        URLConnectionAdapter urlConnection = null;
        setCustomHeaders(br, downloadLink);
        if (downloadLink.getStringProperty("post", null) != null) {
            urlConnection = br.openPostConnection(downloadLink.getDownloadURL(), downloadLink.getStringProperty("post", null));
        } else {
            urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
        }
        return urlConnection;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        this.setBrowserExclusive();
        /* disable gzip, because current downloadsystem cannot handle it correct */
        br.getHeaders().put("Accept-Encoding", "");
        String basicauth = HTACCESSController.getInstance().get(downloadLink.getDownloadURL());
        if (basicauth == null) {
            basicauth = downloadLink.getStringProperty("pass", null);
            if (basicauth != null) basicauth = "Basic " + Encoding.Base64Encode(basicauth);
        }
        if (basicauth != null) {
            br.getHeaders().put("Authorization", basicauth);
        }
        br.setFollowRedirects(true);
        URLConnectionAdapter urlConnection = null;
        try {
            urlConnection = prepareConnection(br, downloadLink);
            if (urlConnection.getResponseCode() == 401 || urlConnection.getResponseCode() == 403) {
                if (basicauth != null) {
                    HTACCESSController.getInstance().remove(downloadLink.getDownloadURL());
                }
                urlConnection.disconnect();
                basicauth = getBasicAuth(downloadLink);
                if (basicauth == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.httplinks.errors.basicauthneeded", "BasicAuth needed"));
                br.getHeaders().put("Authorization", basicauth);
                urlConnection = prepareConnection(br, downloadLink);
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
            if (downloadLink.getFinalFileName() == null) downloadLink.setFinalFileName(Plugin.getFileNameFromHeader(urlConnection));
            downloadLink.setDownloadSize(urlConnection.getLongContentLength());
            this.contentType = urlConnection.getContentType();
            if (contentType.startsWith("text/html")) {
                /* jd does not want to download html content! */
                /* if this page does redirect via js/html, try to follow */
                br.followConnection();
                /* search urls */
                /*
                 * TODO: Change to org.appwork.utils.parser.HTMLParser.findUrls
                 * with next major-udpate
                 */
                ArrayList<String> follow = findUrls(br.toString());
                /*
                 * if we already tried htmlRedirect or not exactly one link
                 * found, throw File not available
                 */
                if (follow.size() != 1 || downloadLink.getBooleanProperty("htmlRedirect", false)) return AvailableStatus.FALSE;
                /* found one valid url */
                downloadLink.setUrlDownload(follow.get(0).trim());
                /* we set property here to avoid loops */
                downloadLink.setProperty("htmlRedirect", true);
                return downloadLink.getAvailableStatus();
            } else {
                urlConnection.disconnect();
            }
            return AvailableStatus.TRUE;
        } catch (PluginException e2) {
            throw e2;
        } catch (Exception e) {
            JDLogger.exception(e);
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        if (link.getDownloadURL().startsWith("directhttp")) {
            link.setUrlDownload(link.getDownloadURL().replaceAll("^directhttp://", ""));
        } else {
            link.setUrlDownload(link.getDownloadURL().replaceAll("httpviajd://", "http://").replaceAll("httpsviajd://", "https://"));
            /* this extension allows to manually add unknown extensions */
            link.setUrlDownload(link.getDownloadURL().replaceAll("\\.jdeatme$", ""));
        }
        BasicAuthfromURL(link);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String auth = br.getHeaders().get("Authorization");
        /*
         * replace with br.setCurrentURL(null); in future (after 0.9)
         */
        br = new Browser();/* needed to clean referer */
        if (auth != null) br.getHeaders().put("Authorization", auth);
        /* workaround to clear referer */
        br.setFollowRedirects(true);
        br.setDebug(true);
        boolean resume = true;
        int chunks = 0;

        if (downloadLink.getBooleanProperty(NORESUME, false) || downloadLink.getBooleanProperty(FORCE_NORESUME, false)) resume = false;
        if (downloadLink.getBooleanProperty(NOCHUNKS, false) || downloadLink.getBooleanProperty(FORCE_NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }
        setCustomHeaders(br, downloadLink);
        if (downloadLink.getStringProperty("post", null) != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), downloadLink.getStringProperty("post", null), resume, chunks);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), resume, chunks);
        }
        if (!dl.startDownload()) {
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(NOCHUNKS, false) == false) {
                    downloadLink.setProperty(NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LISTCONTROLLED, HTACCESSController.getInstance(), JDL.L("plugins.http.htaccess", "List of all HTAccess passwords. Each line one password.")));
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty(NORESUME, false);
        link.setProperty(NOCHUNKS, false);
    }

    @Override
    public void resetPluginGlobals() {
    }

    public String getCustomFavIconURL() {
        if (customFavIconHost != null) return customFavIconHost;
        return null;
    }

    public String getSessionInfo() {
        if (customFavIconHost != null) return customFavIconHost;
        return "";
    }

    /**
     * TODO: can be removed with next major update cause of recaptcha change
     */
    public Recaptcha getReCaptcha(final Browser br) {
        return new Recaptcha(br);
    }

    public static class Recaptcha {

        private final Browser br;
        private String challenge;
        private String server;
        private String captchaAddress;
        private String id;
        private Browser rcBr;
        private Form form;

        public Recaptcha(final Browser br) {
            this.br = br;
        }

        public String getChallenge() {
            return challenge;
        }

        public void setChallenge(final String challenge) {
            this.challenge = challenge;
        }

        public String getServer() {
            return server;
        }

        public void setServer(final String server) {
            this.server = server;
        }

        public String getCaptchaAddress() {
            return captchaAddress;
        }

        public void setCaptchaAddress(final String captchaAddress) {
            this.captchaAddress = captchaAddress;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public Form getForm() {
            return form;
        }

        public void setForm(final Form form) {
            this.form = form;
        }

        public void parse() throws IOException, PluginException {
            final Form[] forms = br.getForms();
            form = null;
            for (final Form f : forms) {
                if (f.getInputField("recaptcha_challenge_field") != null) {
                    form = f;
                    break;
                }
            }
            if (form == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                id = form.getRegex("k=(.*?)\"").getMatch(0);
            }
        }

        public void load() throws IOException, PluginException {
            rcBr = br.cloneBrowser();
            /* follow redirect needed as google redirects to another domain */
            rcBr.setFollowRedirects(true);
            rcBr.getPage("http://api.recaptcha.net/challenge?k=" + id);
            challenge = rcBr.getRegex("challenge.*?:.*?'(.*?)',").getMatch(0);
            server = rcBr.getRegex("server.*?:.*?'(.*?)',").getMatch(0);
            if (challenge == null || server == null) {
                JDLogger.getLogger().severe("Recaptcha Module fails: " + br.getHttpConnection());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            captchaAddress = server + "image?c=" + challenge;
        }

        public File downloadCaptcha(final File captchaFile) throws IOException {
            /* follow redirect needed as google redirects to another domain */
            rcBr.setFollowRedirects(true);
            Browser.download(captchaFile, rcBr.openGetConnection(captchaAddress));
            return captchaFile;
        }

        public Browser setCode(final String code) throws Exception {
            // <textarea name="recaptcha_challenge_field" rows="3"
            // cols="40"></textarea>\n <input type="hidden"
            // name="recaptcha_response_field" value="manual_challenge"/>
            form.put("recaptcha_challenge_field", challenge);
            form.put("recaptcha_response_field", Encoding.urlEncode(code));
            br.submitForm(form);
            return br;
        }
    }

    /**
     * TODO: Remove with next major-update!
     */
    public static ArrayList<String> findUrls(String source) {
        /* TODO: better parsing */
        /* remove tags!! */
        final ArrayList<String> ret = new ArrayList<String>();
        try {

            for (String link : new Regex(source, "((https?|ftp):((//)|(\\\\\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)(\n|\r|$|<|\")").getColumn(0)) {
                try {
                    new URL(link);
                    if (!ret.contains(link)) ret.add(link);
                } catch (MalformedURLException e) {

                }
            }
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return removeDuplicates(ret);
    }

    /**
     * TODO: Remove with next major-update!
     */
    public static ArrayList<String> removeDuplicates(ArrayList<String> links) {
        ArrayList<String> tmplinks = new ArrayList<String>();
        if (links == null || links.size() == 0) return tmplinks;
        for (String link : links) {
            if (link.contains("...")) {
                String check = link.substring(0, link.indexOf("..."));
                String found = link;
                for (String link2 : links) {
                    if (link2.startsWith(check) && !link2.contains("...")) {
                        found = link2;
                        break;
                    }
                }
                if (!tmplinks.contains(found)) tmplinks.add(found);
            } else {
                tmplinks.add(link);
            }
        }
        return tmplinks;
    }

}
