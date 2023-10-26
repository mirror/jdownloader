package jd.plugins.decrypter;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.PasswordUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.PastebinCrawlerSettings;
import org.jdownloader.settings.PastebinCrawlerSettings.PastebinPlaintextCrawlMode;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class AbstractPastebinCrawler extends PluginForDecrypt {
    public AbstractPastebinCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.PASTEBIN };
    }

    protected String getContentURL(final CryptedLink link) {
        return link.getCryptedUrl();
    }

    /**
     * Use this to control which URLs should be returned and which should get skipped. </br>
     * Default = Allow all results
     */
    protected boolean allowResult(final String url) {
        return true;
    }

    /** Returns unique contentID which is expected to be in the given url. */
    abstract String getFID(final String url);

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        /* TODO: Implement logic of pastebin settings once available: https://svn.jdownloader.org/issues/90043 */
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final PastebinMetadata metadata = this.crawlMetadata(param, br);
        if (metadata.getPastebinText() == null) {
            /* This should never happen. Either crawler plugin is broken or paste is offline. */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Could not find pastebin textfield");
        }
        /**
         * TODO: Maybe differentiate between URLs that we support (= have plugin for) and those we don't support. </br>
         * This way we could e.g. only download plaintext as .txt file if no plugin-SUPPORTED items are found.
         */
        final Set<String> pws = PasswordUtils.getPasswords(metadata.getPastebinText());
        ArrayList<String> pwsList = null;
        final String[] links = HTMLParser.getHttpLinks(metadata.getPastebinText(), "");
        logger.info("Found " + links.length + " URLs in plaintext");
        for (final String link : links) {
            final DownloadLink dl = createDownloadlink(link);
            if (pws != null && pws.size() > 0) {
                if (pwsList == null) {
                    // share same instance
                    pwsList = new ArrayList<String>(pws);
                }
                dl.setSourcePluginPasswordList(pwsList);
            }
            ret.add(dl);
        }
        /* Handling for pastebin text download */
        final PastebinPlaintextCrawlMode mode = JsonConfig.create(PastebinCrawlerSettings.class).getPastebinPlaintextCrawlMode();
        final LazyHostPlugin lazyHostPlugin = HostPluginController.getInstance().get(getHost());
        final boolean isAllowedByMode = mode == PastebinPlaintextCrawlMode.ALWAYS || (mode == PastebinPlaintextCrawlMode.ONLY_IF_NO_HTTP_URLS_WERE_FOUND && links.length == 0);
        final String directurl = metadata.getOfficialDirectDownloadlink();
        if (directurl != null && isAllowedByMode || true) {
            final DownloadLink textfile = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl));
            textfile.setFinalFileName(metadata.getFilename());
            textfile.setAvailable(true);
            if (metadata.getPassword() != null) {
                textfile.setDownloadPassword(metadata.getPassword(), true);
            }
            try {
                textfile.setDownloadSize(metadata.getPastebinText().getBytes("UTF-8").length);
            } catch (final UnsupportedEncodingException ignore) {
                ignore.printStackTrace();
            }
            ret.add(textfile);
        } else if (lazyHostPlugin != null && isAllowedByMode) {
            final PluginForHost sisterPlugin = getNewPluginInstance(lazyHostPlugin);
            if (sisterPlugin != null && sisterPlugin.canHandle(param.getCryptedUrl())) {
                final DownloadLink textfile = getDownloadlinkForHosterplugin(param, metadata);
                ret.add(textfile);
            }
        }
        ret.addAll(crawlAdditionalURLs(br, param, metadata));
        if (metadata.getTitle() != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(metadata.getTitle());
            fp.addLinks(ret);
        }
        return ret;
    }

    protected DownloadLink getDownloadlinkForHosterplugin(final CryptedLink link, final PastebinMetadata metadata) {
        if (StringUtils.isEmpty(metadata.getPastebinText())) {
            return null;
        }
        final DownloadLink textfile = this.createDownloadlink(link.getCryptedUrl());
        try {
            textfile.setDownloadSize(metadata.getPastebinText().getBytes("UTF-8").length);
        } catch (final UnsupportedEncodingException ignore) {
            ignore.printStackTrace();
        }
        /* TODO: Set filename according to user preference */
        textfile.setFinalFileName(metadata.getFilename());
        textfile.setAvailable(true);
        if (metadata.getPassword() != null) {
            textfile.setDownloadPassword(metadata.getPassword());
        }
        return textfile;
    }

    /** Use this to crawl additional stuff such as a direct URL to the paste that is not included in the plaintext of the paste itself. */
    protected ArrayList<DownloadLink> crawlAdditionalURLs(final Browser br, final CryptedLink param, final PastebinMetadata metadata) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        return ret;
    }

    /**
     * Collects metadata which will be used later. </br>
     * Handling for password protected pastebins and so on goes in here.
     *
     * @throws Exception
     */
    public PastebinMetadata crawlMetadata(final CryptedLink param, final Browser br) throws Exception {
        final PastebinMetadata metadata = new PastebinMetadata(this.getFID(param.getCryptedUrl()));
        metadata.setPassword(param.getDecrypterPassword());
        return metadata;
    }

    public class PastebinMetadata {
        private String contentID                  = null;
        private String title                      = null;
        private Date   date                       = null;
        private String username                   = null;
        private String description                = null;
        private String pastebinText               = null;
        private String password                   = null;
        private String fileExtension              = ".txt";
        private String officialDirectDownloadlink = null;

        public String getContentID() {
            return contentID;
        }

        public void setContentID(String contentID) {
            this.contentID = contentID;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Date getDate() {
            return date;
        }

        public String getDateFormatted() {
            if (this.date == null) {
                return null;
            } else {
                return new SimpleDateFormat("yyyy-MM-dd").format(this.date);
            }
        }

        public void setDate(final Date date) {
            this.date = date;
        }

        public PastebinMetadata() {
        }

        public PastebinMetadata(final String contentID) {
            this.contentID = contentID;
        }

        public PastebinMetadata(final CryptedLink param) {
            this.setPassword(param.getDecrypterPassword());
        }

        public PastebinMetadata(final CryptedLink param, final String contentID) {
            this.setPassword(param.getDecrypterPassword());
            this.contentID = contentID;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getPastebinText() {
            return pastebinText;
        }

        public void setPastebinText(String pastebinText) {
            this.pastebinText = pastebinText;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public void setFileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public String getFilename() {
            /* contentID needs to be always given! */
            if (this.contentID == null) {
                return null;
            }
            if (this.date != null && this.username != null && this.title != null) {
                return this.getDateFormatted() + "_" + this.username + "_" + this.title + "_" + this.contentID + this.getFileExtension();
            } else if (this.date != null && this.username != null) {
                return this.getDateFormatted() + "_" + this.username + "_" + this.contentID + this.getFileExtension();
            } else if (this.date != null) {
                return this.getDateFormatted() + "_" + this.contentID + this.getFileExtension();
            } else if (this.title != null) {
                return this.title + this.getFileExtension();
            } else {
                return this.contentID + this.getFileExtension();
            }
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getOfficialDirectDownloadlink() {
            return officialDirectDownloadlink;
        }

        /**
         * URL which can be used to download the paste. </br>
         * Needs to be a direct-downloadable URL.
         */
        public void setOfficialDirectDownloadlink(String officialDirectDownloadlink) {
            this.officialDirectDownloadlink = officialDirectDownloadlink;
        }
    }
}
