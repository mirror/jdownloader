package jd.plugins.decrypter;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.proxy.ProxyController;
import jd.http.Browser;
import jd.http.BrowserSettingsThread;
import jd.http.NoGateWayException;
import jd.http.ProxySelectorInterface;
import jd.http.SocketConnectionFactory;
import jd.nutils.SimpleFTP;
import jd.nutils.SimpleFTP.SimpleFTPListEntry;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyException;
import org.jdownloader.auth.Login;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ftp" }, urls = { "ftp://.*?\\.[a-zA-Z0-9]{1,}(:\\d+)?/([^\"\r\n ]+|$)" })
public class Ftp extends PluginForDecrypt {
    private static HashMap<String, Integer> LOCKS = new HashMap<String, Integer>();

    public Ftp(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cLink, ProgressController progress) throws Exception {
        final String lockHost = Browser.getHost(cLink.getCryptedUrl());
        final Integer lock;
        synchronized (LOCKS) {
            lock = LOCKS.get(lockHost);
        }
        if (lock == null) {
            return internalDecryptIt(cLink, progress, -1);
        } else {
            synchronized (lock) {
                return internalDecryptIt(cLink, progress, lock);
            }
        }
    }

    private ArrayList<DownloadLink> internalDecryptIt(CryptedLink cLink, ProgressController progress, final int maxFTPConnections) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>() {
            @Override
            public boolean add(final DownloadLink link) {
                if (maxFTPConnections >= 1) {
                    link.setProperty("MAX_FTP_CONNECTIONS", maxFTPConnections);
                }
                return super.add(link);
            }
        };
        final URL url = new URL(cLink.getCryptedUrl());
        final List<HTTPProxy> proxies = selectProxies(url);
        final HTTPProxy proxy = proxies.get(0);
        final SimpleFTP ftp = new SimpleFTP(proxy, logger) {
            @Override
            protected Socket createSocket() {
                return SocketConnectionFactory.createSocket(getProxy());
            }
        };
        try {
            try {
                ftp.connect(url);
            } catch (IOException e) {
                final String message = e.getMessage();
                if ((StringUtils.containsIgnoreCase(message, "Sorry, the maximum number of clients") || StringUtils.startsWithCaseInsensitive(message, "421")) && maxFTPConnections == -1) {
                    final String lockHost = Browser.getHost(cLink.getCryptedUrl());
                    final String maxConnections = new Regex(e.getMessage(), "Sorry, the maximum number of clients \\((\\d+)\\)").getMatch(0);
                    synchronized (LOCKS) {
                        if (maxConnections != null) {
                            LOCKS.put(lockHost, Integer.parseInt(maxConnections));
                        } else {
                            LOCKS.put(lockHost, new Integer(1));
                        }
                    }
                    return decryptIt(cLink, progress);
                } else if (StringUtils.contains(message, "was unable to log in with the supplied") || StringUtils.contains(message, "530 Login or Password incorrect")) {
                    final DownloadLink dummyLink = new DownloadLink(null, null, url.getHost(), cLink.getCryptedUrl(), true);
                    final Login login = requestLogins(org.jdownloader.translate._JDT.T.DirectHTTP_getBasicAuth_message(), null, dummyLink);
                    if (login != null) {
                        final String host = url.getHost();
                        int port = url.getPort();
                        if (port <= 0) {
                            port = 21;
                        }
                        try {
                            ftp.connect(host, port, login.getUsername(), login.getPassword());
                        } catch (IOException e2) {
                            if (StringUtils.contains(message, "was unable to log in with the supplied") || StringUtils.contains(message, "530 Login or Password incorrect")) {
                                throw new DecrypterException(DecrypterException.PASSWORD, e2);
                            } else {
                                throw e2;
                            }
                        }
                    } else {
                        throw new DecrypterException(DecrypterException.PASSWORD, e);
                    }
                } else {
                    throw e;
                }
            }
            final String currentDir = ftp.getDir();
            String filePath = url.getPath();
            if (!filePath.startsWith("/")) {
                filePath = "/" + filePath;
            }
            if (!filePath.startsWith(currentDir)) {
                if (currentDir.endsWith("/")) {
                    filePath = currentDir + filePath.substring(1);
                } else {
                    filePath = currentDir + filePath;
                }
            }
            final String finalFilePath = filePath;
            String nameString = filePath.substring(filePath.lastIndexOf("/") + 1);
            String nameStringUpper = nameString.toUpperCase(Locale.ENGLISH);
            final byte[] nameBytes, nameBytesUpper;
            if (StringUtils.isEmpty(nameString)) {
                nameString = null;
                nameStringUpper = null;
                nameBytes = null;
                nameBytesUpper = null;
            } else {
                nameBytes = SimpleFTP.toRawBytes(nameString);
                nameBytesUpper = SimpleFTP.toRawBytes(nameStringUpper);
            }
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
            String packageName = new Regex(filePath, "/([^/]+)(/$|$)").getMatch(0);
            final String auth;
            if (!StringUtils.equals("anonymous", ftp.getUser()) || !StringUtils.equals("anonymous", ftp.getUser())) {
                auth = ftp.getUser() + ":" + ftp.getPass() + "@";
            } else {
                auth = "";
            }
            // ftp.listFeatures();
            // ftp.sendClientID("JDownloader");
            // ftp.setUTF8(true);
            if (ftp.cwd(filePath)) {
                SimpleFTPListEntry[] entries = ftp.listEntries();
                if (entries != null) {
                    /*
                     * logic for only adding a given file, ie. ftp://domain/directory/file.exe, you could also have subdirectory of the same
                     * name ftp.../file.exe/file.exe -raztoki
                     */
                    SimpleFTPListEntry found = null;
                    if (nameBytes != null && nameBytesUpper != null) {
                        for (final SimpleFTPListEntry entry : entries) {
                            // we compare bytes because of hex encoding
                            if (Arrays.equals(SimpleFTP.toRawBytes(entry.getName()), nameBytes)) {
                                found = entry;
                                break;
                            }
                        }
                        if (found == null) {
                            for (final SimpleFTPListEntry entry : entries) {
                                // we compare bytes because of hex encoding
                                if (Arrays.equals(SimpleFTP.toRawBytes(entry.getName()), nameBytesUpper)) {
                                    found = entry;
                                    break;
                                }
                            }
                        }
                    }
                    if (found != null) {
                        if (found.isFile()) {
                            final DownloadLink link = createDownloadlink("ftpviajd://" + auth + url.getHost() + (url.getPort() != -1 ? (":" + url.getPort()) : "") + found.getFullPath());
                            link.setAvailable(true);
                            if (found.getSize() >= 0) {
                                link.setVerifiedFileSize(found.getSize());
                            }
                            link.setFinalFileName(SimpleFTP.BestEncodingGuessingURLDecode(found.getName()));
                            ret.add(link);
                        } else {
                            if (ftp.cwd(found.getName())) {
                                entries = ftp.listEntries();
                            } else {
                                entries = new SimpleFTPListEntry[0];
                            }
                        }
                    }
                    /*
                     * logic for complete directory! will not walk aka recursive! - raztoki
                     */
                    if (ret.isEmpty()) {
                        /*
                         * if 'name' == file then packagename == correct, ELSE if name != file then it should be packagename! -raztoki
                         */
                        if (nameString != null) {
                            packageName = nameString;
                        }
                        for (final SimpleFTPListEntry entry : entries) {
                            if (entry.isFile()) {
                                final DownloadLink link = createDownloadlink("ftpviajd://" + auth + url.getHost() + (url.getPort() != -1 ? (":" + url.getPort()) : "") + entry.getFullPath());
                                link.setAvailable(true);
                                if (entry.getSize() >= 0) {
                                    link.setVerifiedFileSize(entry.getSize());
                                }
                                link.setFinalFileName(SimpleFTP.BestEncodingGuessingURLDecode(entry.getName()));
                                ret.add(link);
                            }
                        }
                    }
                }
            }
            if (ret.size() == 0 && nameString != null) {
                // sometimes dir listing/changing is not allowed but direct access is still possible
                if (ftp.bin()) {
                    final long size = ftp.getSize(finalFilePath);
                    if (size >= 0) {
                        final DownloadLink link = createDownloadlink("ftpviajd://" + auth + url.getHost() + (url.getPort() != -1 ? (":" + url.getPort()) : "") + finalFilePath);
                        link.setAvailable(true);
                        link.setVerifiedFileSize(size);
                        link.setFinalFileName(SimpleFTP.BestEncodingGuessingURLDecode(nameString));
                        ret.add(link);
                    }
                }
            }
            if (ret.size() > 0 && packageName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(SimpleFTP.BestEncodingGuessingURLDecode(packageName));
                fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, Boolean.TRUE);
                fp.addLinks(ret);
            }
        } catch (UnknownHostException e) {
            logger.log(e);
            ret.add(createOfflinelink(cLink.getCryptedUrl()));
            return ret;
        } catch (HTTPProxyException e) {
            ProxyController.getInstance().reportHTTPProxyException(proxy, url, e);
            throw e;
        } finally {
            ftp.disconnect();
        }
        return ret;
    }

    protected ProxySelectorInterface getProxySelector() {
        return BrowserSettingsThread.getThreadProxySelector();
    }

    protected List<HTTPProxy> selectProxies(URL url) throws IOException {
        final ProxySelectorInterface selector = getProxySelector();
        if (selector == null) {
            final ArrayList<HTTPProxy> ret = new ArrayList<HTTPProxy>();
            ret.add(HTTPProxy.NONE);
            return ret;
        }
        final List<HTTPProxy> list;
        try {
            list = selector.getProxiesByURL(url);
        } catch (Throwable e) {
            throw new NoGateWayException(selector, e);
        }
        if (list == null || list.size() == 0) {
            throw new NoGateWayException(selector, "No Gateway or Proxy Found: " + url);
        }
        return list;
    }

    @Override
    public Boolean siteTesterDisabled() {
        return true;
    }
}