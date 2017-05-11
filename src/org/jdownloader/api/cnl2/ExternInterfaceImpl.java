package org.jdownloader.api.cnl2;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.PackageInfo;
import jd.controlling.linkcrawler.UnknownCrawledLinkHandler;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;
import net.sf.image4j.codec.ico.ICOEncoder;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.simplejson.JSonObject;
import org.appwork.storage.simplejson.JSonValue;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.PublicSuffixList;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.api.RemoteAPIConfig;
import org.jdownloader.api.cnl2.translate.ExternInterfaceTranslation;
import org.jdownloader.api.myjdownloader.MyJDownloaderRequestInterface;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class ExternInterfaceImpl implements Cnl2APIBasics, Cnl2APIFlash {
    private final static String jdpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + File.separator + "JDownloader.jar";

    public void crossdomainxml(RemoteAPIResponse response) throws InternalApiException {
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\r\n");
        sb.append("<!DOCTYPE cross-domain-policy SYSTEM \"http://www.macromedia.com/xml/dtds/cross-domain-policy.dtd\">\r\n");
        sb.append("<cross-domain-policy>\r\n");
        sb.append("<allow-access-from domain=\"*\" />\r\n");
        sb.append("</cross-domain-policy>\r\n");
        writeString(response, null, sb.toString(), false);
    }

    /**
     * writes given String to response and sets content-type to text/html
     *
     * @param response
     * @param string
     * @throws InternalApiException
     */
    private void writeString(RemoteAPIResponse response, RemoteAPIRequest request, String string, boolean wrapCallback) throws InternalApiException {
        OutputStream out = null;
        try {
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html", false));
            out = RemoteAPI.getOutputStream(response, request, false, true);
            if (wrapCallback && request.getJqueryCallback() != null) {
                if (string == null) {
                    string = "";
                }
                string = "{\"content\": \"" + string.trim() + "\"}";
            }
            out.write(string.getBytes("UTF-8"));
        } catch (Throwable e) {
            throw new InternalApiException(e);
        } finally {
            try {
                out.close();
            } catch (final Throwable e) {
            }
        }
    }

    public void jdcheckjs(RemoteAPIRequest request, RemoteAPIResponse response) throws InternalApiException {
        final StringBuilder sb = new StringBuilder();
        sb.append("jdownloader=true;\r\n");
        sb.append("var version='" + JDUtilities.getRevision() + "';\r\n");
        writeString(response, null, sb.toString(), false);
    }

    public void jdcheckjson(RemoteAPIRequest request, RemoteAPIResponse response) throws InternalApiException {
        final MyJDownloaderSettings set = CFG_MYJD.CFG;
        final JSonObject obj = new JSonObject();
        obj.put("version", new JSonValue(JDUtilities.getRevision()));
        obj.put("deviceId", new JSonValue(set.getUniqueDeviceIDV2()));
        obj.put("name", new JSonValue(set.getDeviceName()));
        writeString(response, null, obj.toString(), false);
    }

    public void addcrypted2(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException {
        try {
            final CnlQueryStorable cnl = new CnlQueryStorable();
            cnl.setCrypted(request.getParameterbyKey("crypted"));
            cnl.setJk(request.getParameterbyKey("jk"));
            cnl.setKey(request.getParameterbyKey("k"));
            if (StringUtils.isEmpty(cnl.getCrypted()) || (StringUtils.isEmpty(cnl.getJk()) && StringUtils.isEmpty(cnl.getKey()))) {
                writeString(response, request, "failed\r\n", true);
                return;
            }
            String passwords[] = request.getParametersbyKey("passwords[]");
            if (passwords == null) {
                passwords = request.getParametersbyKey("passwords");
            }
            if (passwords != null) {
                cnl.setPasswords(Arrays.asList(passwords));
            }
            cnl.setSource(request.getParameterbyKey("source"));
            addcnl(response, request, cnl);
        } catch (Throwable e) {
            e.printStackTrace();
            writeString(response, request, "failed " + e.getMessage() + "\r\n", true);
        }
    }

    private String sourceWorkaround(final String source) {
        if (source != null) {
            try {
                return new URL(source).toString();
            } catch (MalformedURLException e) {
            }
            if (source.contains("filecrypt.cc")) {
                return "http://filecrypt.cc";
            }
        }
        return source;
    }

    // For My JD API
    public void addcrypted2Remote(RemoteAPIResponse response, RemoteAPIRequest request, String crypted, String jk, String source) {
        try {
            source = sourceWorkaround(source);
            askPermission(request, source, null);
            if (StringUtils.isEmpty(crypted) || StringUtils.isEmpty(jk)) {
                return;
            }
            final String dummyCNL = createDummyCNL(crypted, jk, null);
            final LinkCollectingJob job = new LinkCollectingJob(LinkOriginDetails.getInstance(LinkOrigin.CNL, request.getRequestHeaders().getValue("user-agent")), dummyCNL);
            job.setCustomSourceUrl(source);
            LinkCollector.getInstance().addCrawlerJob(job);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void addcnl(final RemoteAPIResponse response, final RemoteAPIRequest request, final CnlQueryStorable cnl) throws InternalApiException {
        try {
            final String packageName = request.getParameterbyKey("package");
            if (packageName != null) {
                // Workaround: "package" can't be used as a field name in CnlQueryStorable as it's a reserved name
                cnl.setPackageName(packageName);
            }
            askPermission(request, cnl.getSource(), cnl.isPermission());
            final List<LinkCollectingJob> jobs = new ArrayList<LinkCollectingJob>();
            if (StringUtils.isNotEmpty(cnl.getCrypted()) && (StringUtils.isNotEmpty(cnl.getJk()) || StringUtils.isNotEmpty(cnl.getKey()))) {
                String jk = cnl.getJk();
                if (StringUtils.isNotEmpty(jk) && jk.matches(".*[0-9];}")) {
                    // TODO: remove after firefox addon version 2.0.16 was published
                    jk = jk.replace(";}", "';}");
                }
                final String dummyCNL = createDummyCNL(cnl.getCrypted(), jk, cnl.getKey());
                jobs.add(new LinkCollectingJob(LinkOriginDetails.getInstance(LinkOrigin.CNL, request.getRequestHeaders().getValue("user-agent")), dummyCNL));
            }
            if (StringUtils.isNotEmpty(cnl.getUrls())) {
                final String urls = cnl.getUrls();
                jobs.add(new LinkCollectingJob(LinkOriginDetails.getInstance(LinkOrigin.CNL, request.getRequestHeaders().getValue("user-agent")), urls));
            }
            if (jobs.size() > 0) {
                final CrawledLinkModifier modifier = new CrawledLinkModifier() {
                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        final DownloadLink dl = link.getDownloadLink();
                        if (dl != null) {
                            if (!StringUtils.isEmpty(cnl.getComment())) {
                                dl.setComment(cnl.getComment());
                            }
                            if (!StringUtils.isEmpty(cnl.getReferrer())) {
                                dl.setReferrerUrl(cnl.getReferrer());
                            }
                            if (!StringUtils.isEmpty(cnl.getSource())) {
                                dl.setOriginUrl(cnl.getSource());
                            }
                        }
                        if (cnl.getPasswords() != null && cnl.getPasswords().size() > 0) {
                            link.getArchiveInfo().getExtractionPasswords().addAll(cnl.getPasswords());
                        }
                        if (StringUtils.isNotEmpty(cnl.getPackageName()) || !StringUtils.isEmpty(cnl.getDir())) {
                            PackageInfo existing = link.getDesiredPackageInfo();
                            if (existing == null) {
                                existing = new PackageInfo();
                            }
                            boolean writeChanges = false;
                            if (!StringUtils.isEmpty(cnl.getPackageName())) {
                                existing.setName(cnl.getPackageName());
                                writeChanges = true;
                            }
                            if (!StringUtils.isEmpty(cnl.getDir())) {
                                existing.setDestinationFolder(cnl.getDir());
                                writeChanges = true;
                            }
                            if (writeChanges) {
                                link.setDesiredPackageInfo(existing);
                            }
                        }
                    }
                };
                for (final LinkCollectingJob job : jobs) {
                    job.setCrawledLinkModifierPrePackagizer(modifier);
                    LinkCollector.getInstance().addCrawlerJob(job);
                }
            }
        } catch (Throwable e) {
        }
    }

    private String getLongerString(String a, String b) {
        if (a != null && b != null) {
            if (a.length() > b.length()) {
                return a;
            }
            return b;
        } else if (a != null && b == null) {
            return a;
        } else if (b != null && a == null) {
            return b;
        }
        return null;
    }

    private void clickAndLoad2Add(LinkOriginDetails origin, String urls, RemoteAPIRequest request) throws IOException {
        final String finalPasswords = request.getParameterbyKey("passwords");
        String source = request.getParameterbyKey("source");
        final String referer = request.getRequestHeaders().getValue(HTTPConstants.HEADER_REQUEST_REFERER);
        String linkComment = request.getParameterbyKey("comment");
        final LinkCollectingJob job = new LinkCollectingJob(origin, urls);
        final String finalDestination = request.getParameterbyKey("dir");
        String packageName = request.getParameterbyKey("package");
        String packageComment = null;
        if (source != null && !(StringUtils.startsWithCaseInsensitive(source, "http://") || StringUtils.startsWithCaseInsensitive(source, "https://"))) {
            final PublicSuffixList psl = PublicSuffixList.getInstance();
            if (psl == null || psl.getDomain(source.toLowerCase(Locale.ENGLISH)) == null) {
                packageComment = source;
            }
            source = null;
        }
        if (source != null) {
            job.setCustomSourceUrl(getLongerString(source, referer));
        } else {
            job.setCustomSourceUrl(referer);
        }
        final String finalPackageName = packageName;
        final String finalComment = linkComment;
        final String finalPackageComment;
        if (linkComment != null) {
            finalPackageComment = linkComment;
        } else {
            finalPackageComment = packageComment;
        }
        final CrawledLinkModifier modifier = new CrawledLinkModifier() {
            private HashSet<String> pws = null;
            {
                if (StringUtils.isNotEmpty(finalPasswords)) {
                    pws = new HashSet<String>();
                    pws.add(finalPasswords);
                }
            }

            @Override
            public void modifyCrawledLink(CrawledLink link) {
                if (StringUtils.isNotEmpty(finalDestination)) {
                    PackageInfo packageInfo = link.getDesiredPackageInfo();
                    if (packageInfo == null) {
                        packageInfo = new PackageInfo();
                    }
                    packageInfo.setDestinationFolder(finalDestination);
                    packageInfo.setIgnoreVarious(true);
                    packageInfo.setUniqueId(null);
                    link.setDesiredPackageInfo(packageInfo);
                }
                if (StringUtils.isNotEmpty(finalPackageName)) {
                    PackageInfo packageInfo = link.getDesiredPackageInfo();
                    if (packageInfo == null) {
                        packageInfo = new PackageInfo();
                    }
                    packageInfo.setName(finalPackageName);
                    packageInfo.setIgnoreVarious(true);
                    packageInfo.setUniqueId(null);
                    link.setDesiredPackageInfo(packageInfo);
                }
                if (StringUtils.isNotEmpty(finalPackageComment)) {
                    PackageInfo packageInfo = link.getDesiredPackageInfo();
                    if (packageInfo == null) {
                        packageInfo = new PackageInfo();
                    }
                    packageInfo.setComment(finalPackageComment);
                    packageInfo.setIgnoreVarious(true);
                    packageInfo.setUniqueId(null);
                    link.setDesiredPackageInfo(packageInfo);
                }
                final DownloadLink dlLink = link.getDownloadLink();
                if (dlLink != null) {
                    if (StringUtils.isNotEmpty(finalComment)) {
                        dlLink.setComment(finalComment);
                    }
                }
                if (pws != null && pws.size() > 0) {
                    link.getArchiveInfo().getExtractionPasswords().addAll(pws);
                }
            }
        };
        job.setCrawledLinkModifierPrePackagizer(modifier);
        if (StringUtils.isNotEmpty(finalPackageName) || StringUtils.isNotEmpty(finalDestination)) {
            job.setCrawledLinkModifierPostPackagizer(modifier);
        }
        LinkCollector.getInstance().addCrawlerJob(job);
    }

    private String createDummyCNL(String crypted, final String jk, String k) throws UnsupportedEncodingException {
        final HashMap<String, String> infos = new HashMap<String, String>();
        infos.put("crypted", crypted);
        if (jk != null) {
            infos.put("jk", jk);
        }
        if (k != null) {
            infos.put("k", k);
        }
        final String json = JSonStorage.toString(infos);
        return "http://dummycnl.jdownloader.org/" + HexFormatter.byteArrayToHex(json.getBytes("UTF-8"));
    }

    public void add(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException {
        try {
            askPermission(request, null, null);
            String urls = request.getParameterbyKey("urls");
            if (StringUtils.isEmpty(urls) && request.getParameters() != null && request.getParameters().length >= 4) {
                urls = request.getParameters()[3];
            }
            if (StringUtils.isNotEmpty(urls)) {
                clickAndLoad2Add(LinkOriginDetails.getInstance(LinkOrigin.CNL, request.getRequestHeaders().getValue("user-agent")), urls, request);
            }
            writeString(response, request, "success\r\n", true);
        } catch (Throwable e) {
            writeString(response, request, "failed " + e.getMessage() + "\r\n", true);
        }
    }

    // For My JD API
    @Override
    public void add(RemoteAPIRequest request, RemoteAPIResponse response, String fromFallback, String password, String source, String url) throws InternalApiException {
        add(request, response, password, source, url);
    }

    // For My JD API
    @Override
    public void add(RemoteAPIRequest request, RemoteAPIResponse response, String passwordParam, String sourceParam, String urlParam) throws InternalApiException {
        try {
            String source = null;
            boolean keyValueParams = request.getParameterbyKey("urls") != null;
            try {
                if (keyValueParams) {
                    source = request.getParameterbyKey("source");
                } else if (request.getParameters() != null && request.getParameters().length >= 3) {
                    source = request.getParameters()[2];
                }
            } catch (IOException e) {
            }
            if (source == null) {
                source = sourceParam;
            }
            askPermission(request, source, null);
            String urls = null;
            try {
                if (keyValueParams) {
                    urls = request.getParameterbyKey("urls");
                } else if (request.getParameters() != null && request.getParameters().length >= 4) {
                    urls = request.getParameters()[3];
                }
            } catch (IOException e) {
            }
            if (urls == null) {
                urls = urlParam;
            }
            String passwords = null;
            try {
                if (keyValueParams) {
                    passwords = request.getParameterbyKey("passwords");
                } else if (request.getParameters() != null && request.getParameters().length >= 2) {
                    passwords = request.getParameters()[1];
                }
            } catch (IOException e) {
            }
            if (passwords == null) {
                passwords = passwordParam;
            }
            final String finalPasswords = passwords;
            final String finalComment = request.getParameterbyKey("comment");
            LinkCollectingJob job = new LinkCollectingJob(LinkOriginDetails.getInstance(LinkOrigin.CNL, request.getRequestHeaders().getValue("user-agent")), urls);
            final String finalDestination = request.getParameterbyKey("dir");
            job.setCustomSourceUrl(source);
            final String finalPackageName = request.getParameterbyKey("package");
            final CrawledLinkModifier modifier = new CrawledLinkModifier() {
                private HashSet<String> pws = null;
                {
                    if (StringUtils.isNotEmpty(finalPasswords)) {
                        pws = new HashSet<String>();
                        pws.add(finalPasswords);
                    }
                }

                @Override
                public void modifyCrawledLink(CrawledLink link) {
                    if (StringUtils.isNotEmpty(finalDestination)) {
                        PackageInfo packageInfo = link.getDesiredPackageInfo();
                        if (packageInfo == null) {
                            packageInfo = new PackageInfo();
                        }
                        packageInfo.setDestinationFolder(finalDestination);
                        packageInfo.setIgnoreVarious(true);
                        packageInfo.setUniqueId(null);
                        link.setDesiredPackageInfo(packageInfo);
                    }
                    if (StringUtils.isNotEmpty(finalPackageName)) {
                        PackageInfo packageInfo = link.getDesiredPackageInfo();
                        if (packageInfo == null) {
                            packageInfo = new PackageInfo();
                        }
                        packageInfo.setName(finalPackageName);
                        packageInfo.setIgnoreVarious(true);
                        packageInfo.setUniqueId(null);
                        link.setDesiredPackageInfo(packageInfo);
                    }
                    DownloadLink dlLink = link.getDownloadLink();
                    if (dlLink != null) {
                        if (StringUtils.isNotEmpty(finalComment)) {
                            dlLink.setComment(finalComment);
                        }
                    }
                    if (pws != null && pws.size() > 0) {
                        link.getArchiveInfo().getExtractionPasswords().addAll(pws);
                    }
                }
            };
            job.setCrawledLinkModifierPrePackagizer(modifier);
            if (StringUtils.isNotEmpty(finalPackageName) || StringUtils.isNotEmpty(finalDestination)) {
                job.setCrawledLinkModifierPostPackagizer(modifier);
            }
            LinkCollector.getInstance().addCrawlerJob(job);
        } catch (Throwable e) {
            writeString(response, request, "failed " + e.getMessage() + "\r\n", true);
        }
    }

    public void addcrypted(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException {
        try {
            askPermission(request, null, null);
            final String dlcContent = request.getParameterbyKey("crypted");
            if (dlcContent == null) {
                throw new IllegalArgumentException("no DLC Content available");
            }
            final String dlc = dlcContent.trim().replace(" ", "+");
            final File tmp = Application.getTempResource("jd_" + System.currentTimeMillis() + ".dlc");
            IO.writeToFile(tmp, dlc.getBytes("UTF-8"));
            final String url = tmp.toURI().toString();
            clickAndLoad2Add(LinkOriginDetails.getInstance(LinkOrigin.CNL, request.getRequestHeaders().getValue("user-agent")), url, request);
            writeString(response, request, "success\r\n", true);
        } catch (Throwable e) {
            writeString(response, request, "failed " + e.getMessage() + "\r\n", true);
        }
    }

    private synchronized void askPermission(final RemoteAPIRequest request, final String fallbackSource, final Boolean byPassPermission) throws IOException, DialogNoAnswerException {
        if (!Boolean.FALSE.equals(byPassPermission) && request.getHttpRequest() instanceof MyJDownloaderRequestInterface) {
            // valid
            return;
        }
        HTTPHeader jdrandomNumber = request.getRequestHeaders().get("jd.randomnumber");
        if (jdrandomNumber != null && jdrandomNumber.getValue() != null && jdrandomNumber.getValue().equalsIgnoreCase(System.getProperty("jd.randomNumber"))) {
            /*
             * request knows secret jd.randomnumber, it is okay to handle this request
             */
            return;
        }
        final HTTPHeader referer = request.getRequestHeaders().get(HTTPConstants.HEADER_REQUEST_REFERER);
        String check = null;
        if (referer != null && (check = referer.getValue()) != null) {
            if (check.equalsIgnoreCase("http://localhost:9666/flashgot") || check.equalsIgnoreCase("http://127.0.0.1:9666/flashgot")) {
                /*
                 * security check for flashgot referer, skip asking if we find valid flashgot referer
                 */
                return;
            }
        }
        String app = "unknown application";
        final HTTPHeader agent = request.getRequestHeaders().get(HTTPConstants.HEADER_REQUEST_USER_AGENT);
        if (agent != null && agent.getValue() != null) {
            /* try to parse application name from user agent header */
            app = agent.getValue().replaceAll("\\(.*\\)", "");
        }
        String url = null;
        if (referer != null) {
            /* lets use the referer as source of the request */
            url = referer.getValue();
        }
        if (url == null) {
            /* no referer available, maybe a source variable is? */
            url = request.getParameterbyKey("source");
        }
        if (url == null) {
            url = fallbackSource;
        }
        if (url != null) {
            url = Browser.getHost(url);
        }
        ArrayList<String> allowed = JsonConfig.create(RemoteAPIConfig.class).getExternInterfaceAuth();
        if (allowed != null && url != null && allowed.contains(url)) {
            /* the url is already allowed to add links */
            return;
        }
        final String from = url != null ? url : app;
        try {
            final ConfirmDialog d = new ConfirmDialog(0, ExternInterfaceTranslation.T.jd_plugins_optional_interfaces_jdflashgot_security_title(from), ExternInterfaceTranslation.T.jd_plugins_optional_interfaces_jdflashgot_security_message(), null, ExternInterfaceTranslation.T.jd_plugins_optional_interfaces_jdflashgot_security_btn_allow(), ExternInterfaceTranslation.T.jd_plugins_optional_interfaces_jdflashgot_security_btn_deny()) {
                @Override
                public ModalityType getModalityType() {
                    return ModalityType.MODELESS;
                }
            };
            UIOManager.I().show(ConfirmDialogInterface.class, d).throwCloseExceptions();
        } catch (DialogNoAnswerException e) {
            throw e;
        }
        if (url != null) {
            /* we can only save permission if an url is available */
            if (allowed == null) {
                allowed = new ArrayList<String>();
            }
            allowed.add(url);
            JsonConfig.create(RemoteAPIConfig.class).setExternInterfaceAuth(allowed);
        }
    }

    public void alive(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException {
        writeString(response, request, "JDownloader\r\n", true);
    }

    public void favicon(RemoteAPIResponse response) throws InternalApiException {
        OutputStream out = null;
        try {
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "image/x-icon", false));
            out = RemoteAPI.getOutputStream(response, null, false, false);
            Icon logo = new AbstractIcon(IconKey.ICON_LOGO_JD_LOGO_128_128, 32);
            ICOEncoder.write(IconIO.toBufferedImage(logo), out);
        } catch (Throwable e) {
            throw new InternalApiException(e);
        } finally {
            try {
                out.close();
            } catch (final Throwable e) {
            }
        }
    }

    private String getValue(final String[] values, final int index) {
        if (values != null && index >= 0 && index < values.length && StringUtils.isNotEmpty(values[index])) {
            return values[index];
        } else {
            return null;
        }
    }

    public void flashgot(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException {
        try {
            askPermission(request, null, null);
            StringBuilder sb = new StringBuilder();
            sb.append(jdpath + "\r\n");
            sb.append("java -Xmx512m -jar " + jdpath + "\r\n");
            final String urls[] = Regex.getLines(request.getParameterbyKey("urls"));
            if (urls != null && urls.length > 0) {
                final String desc[] = Regex.getLines(request.getParameterbyKey("descriptions"));
                final String fnames[] = Regex.getLines(request.getParameterbyKey("fnames"));
                final String cookies = request.getParameterbyKey("cookies");
                final String post = request.getParameterbyKey("postData");
                final String referer = request.getParameterbyKey("referer");
                final String downloadPasswords[] = Regex.getLines(request.getParameterbyKey("dpass"));
                final String archivePasswords[] = Regex.getLines(request.getParameterbyKey("apass"));
                final Boolean finalAutostart;
                if (request.getParameterbyKey("autostart") != null) {
                    finalAutostart = "1".equals(request.getParameterbyKey("autostart"));
                } else {
                    finalAutostart = null;
                }
                /*
                 * create LinkCollectingJob to forward general Information like directory, autostart...
                 */
                final LinkCollectingJob job = new LinkCollectingJob(LinkOriginDetails.getInstance(LinkOrigin.FLASHGOT, request.getRequestHeaders().getValue("user-agent")), null);
                final String finalPackageName = request.getParameterbyKey("package");
                String dir = request.getParameterbyKey("dir");
                if (dir != null && dir.matches("^[a-zA-Z]{1}:$")) {
                    /* flashgot seems unable to set x:/ <-> only x: is possible */
                    dir = dir + "/";
                }
                final String finalDestination = dir;
                final HashSet<String> pws = new HashSet<String>();
                if (archivePasswords != null) {
                    for (String p : archivePasswords) {
                        if (StringUtils.isNotEmpty(p)) {
                            pws.add(p);
                        }
                    }
                }
                if (finalAutostart != null || pws.size() > 0) {
                    final CrawledLinkModifier preModifier = new CrawledLinkModifier() {
                        @Override
                        public void modifyCrawledLink(CrawledLink link) {
                            if (pws.size() > 0) {
                                link.getArchiveInfo().getExtractionPasswords().addAll(pws);
                            }
                            if (finalAutostart != null) {
                                link.setAutoConfirmEnabled(finalAutostart);
                                link.setAutoStartEnabled(finalAutostart);
                            }
                        }
                    };
                    job.setCrawledLinkModifierPrePackagizer(preModifier);
                }
                if (StringUtils.isNotEmpty(finalPackageName) || StringUtils.isNotEmpty(finalDestination)) {
                    final CrawledLinkModifier postModifier = new CrawledLinkModifier() {
                        @Override
                        public void modifyCrawledLink(CrawledLink link) {
                            if (StringUtils.isNotEmpty(finalPackageName)) {
                                PackageInfo packageInfo = link.getDesiredPackageInfo();
                                if (packageInfo == null) {
                                    packageInfo = new PackageInfo();
                                }
                                packageInfo.setName(finalPackageName);
                                packageInfo.setUniqueId(null);
                                packageInfo.setIgnoreVarious(true);
                                link.setDesiredPackageInfo(packageInfo);
                            }
                            if (StringUtils.isNotEmpty(finalDestination)) {
                                PackageInfo packageInfo = link.getDesiredPackageInfo();
                                if (packageInfo == null) {
                                    packageInfo = new PackageInfo();
                                }
                                packageInfo.setDestinationFolder(finalDestination);
                                packageInfo.setIgnoreVarious(true);
                                packageInfo.setUniqueId(null);
                                link.setDesiredPackageInfo(packageInfo);
                            }
                        }
                    };
                    job.setCrawledLinkModifierPostPackagizer(postModifier);
                }
                final UnknownCrawledLinkHandler unknownCrawledLinkHandler = new UnknownCrawledLinkHandler() {
                    public void unhandledCrawledLink(CrawledLink link, LinkCrawler lc) {
                        final DownloadLink dl = link.getDownloadLink();
                        if (dl != null && !StringUtils.startsWithCaseInsensitive(dl.getPluginPatternMatcher(), "directhttp://")) {
                            dl.setPluginPatternMatcher("directhttp://" + dl.getPluginPatternMatcher());
                        }
                    }
                };
                final List<CrawledLink> links = new ArrayList<CrawledLink>();
                for (int index = 0; index < urls.length; index++) {
                    final DownloadLink downloadLink = new DownloadLink(null, null, null, urls[index], true);
                    downloadLink.setProperty("referer", referer);
                    downloadLink.setDownloadPassword(getValue(downloadPasswords, index));
                    downloadLink.setComment(getValue(desc, index));
                    downloadLink.setForcedFileName(getValue(fnames, index));
                    if (StringUtils.isNotEmpty(cookies)) {
                        downloadLink.setProperty("cookies", cookies);
                    }
                    if (StringUtils.isNotEmpty(post)) {
                        downloadLink.setProperty("post", post);
                    }
                    final CrawledLink crawledLink = new CrawledLink(downloadLink);
                    crawledLink.setOrigin(job.getOrigin());
                    crawledLink.setSourceJob(job);
                    if (StringUtils.isNotEmpty(referer)) {
                        crawledLink.setCustomCrawledLinkModifier(new CrawledLinkModifier() {
                            public void modifyCrawledLink(CrawledLink link) {
                                final DownloadLink dl = link.getDownloadLink();
                                if (dl != null) {
                                    if (StringUtils.isEmpty(dl.getReferrerUrl())) {
                                        dl.setReferrerUrl(referer);
                                    }
                                }
                            }
                        });
                    }
                    crawledLink.setUnknownHandler(unknownCrawledLinkHandler);
                    links.add(crawledLink);
                }
                LinkCollector.getInstance().addCrawlerJob(links, job);
            }
            writeString(response, request, sb.toString(), true);
        } catch (final Throwable e) {
            e.printStackTrace();
            throw new InternalApiException(e);
        }
    }
}
