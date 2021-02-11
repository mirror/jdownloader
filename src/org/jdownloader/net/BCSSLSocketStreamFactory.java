/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.net.httpconnection
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.jdownloader.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.SSLSocketStreamFactory;
import org.appwork.utils.net.httpconnection.SSLSocketStreamInterface;
import org.appwork.utils.net.httpconnection.SSLSocketStreamOptions;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.IDNUtil;
import org.bouncycastle.tls.CertificateRequest;
import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.DefaultTlsClient;
import org.bouncycastle.tls.NameType;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.ServerName;
import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsClientProtocol;
import org.bouncycastle.tls.TlsCredentials;
import org.bouncycastle.tls.TlsExtensionsUtils;
import org.bouncycastle.tls.TlsServerCertificate;
import org.bouncycastle.tls.TlsUtils;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.bouncycastle.util.Strings;

/**
 * @author daniel
 *
 */
public class BCSSLSocketStreamFactory implements SSLSocketStreamFactory {
    protected final static Provider                 BC               = new BouncyCastleProvider();
    private final static String                     TLS13_ENABLED    = "BC_TLS1.3_ENABLED";
    // raymii.org/s/tutorials/Strong_SSL_Security_On_nginx.html
    // openssl.org/docs/man1.0.1/apps/ciphers.html
    // TODO: sort strength
    protected static final String                   CIPHERS          = "CHACHA20:EECDH+AESGCM:EDH+AESGCM:ECDHE-RSA-AES128-GCM-SHA256:AES256+EECDH:DHE-RSA-AES128-GCM-SHA256:AES256+EDH:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-SHA384:ECDHE-RSA-AES128-SHA256:ECDHE-RSA-AES256-SHA:ECDHE-RSA-AES128-SHA:DHE-RSA-AES256-SHA256:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-SHA:DHE-RSA-AES128-SHA:ECDHE-RSA-DES-CBC3-SHA:EDH-RSA-DES-CBC3-SHA:AES256-GCM-SHA384:AES128-GCM-SHA256:AES256-SHA256:AES128-SHA256:AES256-SHA:AES128-SHA:DES-CBC3-SHA:HIGH:!anon:!eNULL:!DHE:!SRP:!EXPORT:!DES:!MD5:!PSK:!RC4";
    protected static final HashMap<Integer, String> CIPHERSUITENAMES = new HashMap<Integer, String>();
    protected static int[]                          CIPHERSUITES;
    static {
        try {
            final Field[] fields = CipherSuite.class.getFields();
            for (Field field : fields) {
                final int cipherSuite = field.getInt(null);
                CIPHERSUITENAMES.put(cipherSuite, field.getName());
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        CIPHERSUITES = INIT_CIPHER_SUITES();
    }

    protected int[] modifyCipherSuites(int[] cipherSuites, final SSLSocketStreamOptions options) {
        if (cipherSuites != null && options != null) {
            final List<String> avoided = new ArrayList<String>();
            final List<String> preferred = new ArrayList<String>();
            final List<String> disabled = new ArrayList<String>();
            final ArrayList<String> list = new ArrayList<String>();
            for (final int cipherSuite : cipherSuites) {
                list.add(getCipherSuiteName(cipherSuite));
            }
            if (options.getDisabledCipherSuites().size() > 0) {
                for (final String disabledEntry : options.getDisabledCipherSuites()) {
                    final Iterator<String> it = list.iterator();
                    disableLoop: while (it.hasNext()) {
                        final String next = it.next();
                        if (StringUtils.containsIgnoreCase(next, disabledEntry)) {
                            for (String enabledEntry : options.getEnabledCipherSuites()) {
                                if (StringUtils.containsIgnoreCase(next, enabledEntry)) {
                                    continue disableLoop;
                                }
                            }
                            it.remove();
                            disabled.add(next);
                        }
                    }
                }
            }
            if (options.getAvoidedCipherSuites().size() > 0) {
                for (final String avoidedEntry : options.getAvoidedCipherSuites()) {
                    final Iterator<String> it = list.iterator();
                    while (it.hasNext()) {
                        final String next = it.next();
                        if (StringUtils.containsIgnoreCase(next, avoidedEntry)) {
                            it.remove();
                            avoided.add(next);
                        }
                    }
                }
            }
            if (options.getPreferredCipherSuites().size() > 0) {
                for (final String preferredEntry : options.getPreferredCipherSuites()) {
                    Iterator<String> it = list.iterator();
                    while (it.hasNext()) {
                        final String next = it.next();
                        if (StringUtils.containsIgnoreCase(next, preferredEntry)) {
                            it.remove();
                            preferred.add(next);
                        }
                    }
                    it = avoided.iterator();
                    while (it.hasNext()) {
                        final String next = it.next();
                        if (StringUtils.containsIgnoreCase(next, preferredEntry)) {
                            it.remove();
                            preferred.add(next);
                        }
                    }
                }
            }
            if (disabled.size() > 0 || preferred.size() > 0 || avoided.size() > 0) {
                if (preferred.size() > 0) {
                    list.addAll(0, preferred);
                }
                if (avoided.size() > 0) {
                    list.addAll(avoided);
                }
            }
            final String[] sortedCipherSuites = options.sortCipherSuites(list.toArray(new String[0]));
            cipherSuites = new int[sortedCipherSuites.length];
            int index = 0;
            for (final String sortedCipherSuite : sortedCipherSuites) {
                cipherSuites[index++] = getCipherSuiteID(sortedCipherSuite);
            }
        }
        return cipherSuites;
    }

    private static int[] INIT_CIPHER_SUITES() {
        final LinkedHashMap<Integer, String> enabledCipherSuites = new LinkedHashMap<Integer, String>();
        final String[] cipherRules = CIPHERS.split(":");
        for (final String cipherRule : cipherRules) {
            suites: for (final Entry<Integer, String> cipherSuite : CIPHERSUITENAMES.entrySet()) {
                final String cipherSuiteID = cipherSuite.getValue().replaceAll("(AES_(\\d+))", "AES$2").replaceAll("(RC4_(\\d+))", "RC4$2");
                if (cipherRule.startsWith("!")) {
                    final Iterator<Entry<Integer, String>> it = enabledCipherSuites.entrySet().iterator();
                    while (it.hasNext()) {
                        final Entry<Integer, String> next = it.next();
                        if (next.getValue().contains("_" + cipherRule.substring(1))) {
                            it.remove();
                        }
                    }
                    continue;
                } else if (cipherRule.contains("+")) {
                    final String[] rules = cipherRule.replace("EDH", "DHE").replace("AESGCM", "AES+GCM").replace("EECDH", "ECDHE").split("\\+");
                    for (String rule : rules) {
                        if (!cipherSuiteID.contains("_" + rule)) {
                            continue suites;
                        }
                    }
                    enabledCipherSuites.put(cipherSuite.getKey(), cipherSuite.getValue());
                } else {
                    final String[] rules = cipherRule.split("-");
                    for (String rule : rules) {
                        if (!cipherSuiteID.contains("_" + rule)) {
                            continue suites;
                        }
                    }
                    enabledCipherSuites.put(cipherSuite.getKey(), cipherSuite.getValue());
                }
            }
        }
        final int[] ret = new int[enabledCipherSuites.size()];
        int index = 0;
        for (Integer enabledCipherSuite : enabledCipherSuites.keySet()) {
            ret[index++] = enabledCipherSuite.intValue();
        }
        return ret;
    }

    private class BCTLSSocketStreamTlsClient extends DefaultTlsClient {
        private final String                 hostName;
        private final int[]                  enabledCipherSuites;
        private final boolean                sniEnabled;
        private final SSLSocketStreamOptions options;

        private BCTLSSocketStreamTlsClient(final String hostName, final boolean sniEnabled, final SSLSocketStreamOptions options) {
            super(new BcTlsCrypto(new SecureRandom()));
            // super(new JcaTlsCryptoProvider().setProvider(BC).create(new SecureRandom()));
            this.hostName = hostName;
            this.enabledCipherSuites = modifyCipherSuites(CIPHERSUITES, options);
            this.sniEnabled = sniEnabled;
            this.options = options;
        }

        @Override
        public ProtocolVersion[] getProtocolVersions() {
            final ProtocolVersion[] ret = super.getProtocolVersions();
            if (options.getCustomFactorySettings().contains(TLS13_ENABLED)) {
                final List<ProtocolVersion> protocolVersions = new ArrayList<ProtocolVersion>(Arrays.asList(ret));
                if (!protocolVersions.contains(ProtocolVersion.TLSv13)) {
                    protocolVersions.add(0, ProtocolVersion.TLSv13);
                }
                return protocolVersions.toArray(new ProtocolVersion[0]);
            } else {
                return ret;
            }
        }

        @Override
        protected int[] getSupportedCipherSuites() {
            return TlsUtils.getSupportedCipherSuites(getCrypto(), enabledCipherSuites);
        }

        @Override
        protected Vector<ServerName> getSNIServerNames() {
            if (sniEnabled && StringUtils.isNotEmpty(hostName) && !hostName.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                final ServerName serverName = new ServerName(NameType.host_name, Strings.toByteArray(IDNUtil.toASCII(hostName, IDNUtil.USE_STD3_ASCII_RULES)));
                final Vector<ServerName> sni = new Vector<ServerName>();
                sni.add(serverName);
                return sni;
            } else {
                return null;
            }
        }

        @Override
        public Hashtable getClientExtensions() throws IOException {
            final Hashtable clientExtensions = super.getClientExtensions();
            if (clientExtensions != null) {
                if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE || !Arrays.asList(getProtocolVersions()).contains(ProtocolVersion.TLSv13)) {
                    // only possible with TLS1.3/block ciphers, see TlsClientProtocol.processServerHelloMessage
                    clientExtensions.remove(TlsExtensionsUtils.EXT_encrypt_then_mac);
                }
                // do not request OCP
                clientExtensions.remove(TlsExtensionsUtils.EXT_status_request);
                clientExtensions.remove(TlsExtensionsUtils.EXT_status_request_v2);
            }
            return clientExtensions;
        }

        @Override
        public void notifySecureRenegotiation(boolean arg0) throws IOException {
            // ignore, eg mega.co does not support renegotiation
        }

        // public void notifyAlertRaised(short alertLevel, short alertDescription, String message, Throwable cause) {
        // PrintStream out = (alertLevel == AlertLevel.fatal) ? System.err : System.out;
        // out.println("TLS client raised alert: " + AlertLevel.getText(alertLevel) + ", " + AlertDescription.getText(alertDescription));
        // if (message != null) {
        // out.println("> " + message);
        // }
        // if (cause != null) {
        // cause.printStackTrace(out);
        // }
        // }
        //
        // public void notifyAlertReceived(short alertLevel, short alertDescription) {
        // PrintStream out = (alertLevel == AlertLevel.fatal) ? System.err : System.out;
        // out.println("TLS client received alert: " + AlertLevel.getText(alertLevel) + ", " + AlertDescription.getText(alertDescription));
        // }
        @Override
        public TlsAuthentication getAuthentication() throws IOException {
            final TlsAuthentication auth = new TlsAuthentication() {
                public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {
                    return null;
                }

                @Override
                public void notifyServerCertificate(TlsServerCertificate arg0) throws IOException {
                    // here we can access the ServerCertificate
                }
            };
            return auth;
        }

        protected int             selectedCipherSuite = -1;
        protected ProtocolVersion protocolVersion     = null;

        private int getSelectedCipherSuite() {
            return selectedCipherSuite;
        }

        @Override
        public void notifySelectedCipherSuite(int selectedCipherSuite) {
            this.selectedCipherSuite = selectedCipherSuite;
        }

        @Override
        public void notifyServerVersion(ProtocolVersion protocolVersion) throws IOException {
            this.protocolVersion = protocolVersion;
        }

        public ProtocolVersion getClientVersion() {
            return protocolVersion;
        }
    }

    protected static String getCipherSuiteName(final Integer selectedCipherSuite) {
        if (CIPHERSUITENAMES.containsKey(selectedCipherSuite)) {
            return CIPHERSUITENAMES.get(selectedCipherSuite);
        } else {
            return selectedCipherSuite.toString();
        }
    }

    protected static int getCipherSuiteID(final String cipherSuite) {
        for (final Entry<Integer, String> entry : CIPHERSUITENAMES.entrySet()) {
            if (entry.getValue().equals(cipherSuite)) {
                return entry.getKey().intValue();
            }
        }
        return -1;
    }

    public interface BCSSLSocketStreamInterface extends SSLSocketStreamInterface {
        public TlsClientProtocol getTlsClientProtocol();

        public BCTLSSocketStreamTlsClient getTlsClient();
    }

    @Override
    public SSLSocketStreamInterface create(final SocketStreamInterface socketStream, final String hostName, final int port, final boolean autoclose, final SSLSocketStreamOptions options) throws IOException {
        final boolean sniEnabled = !StringUtils.isEmpty(hostName) && (options == null || options.isSNIEnabled());
        final TlsClientProtocol protocol = new TlsClientProtocol(socketStream.getInputStream(), socketStream.getOutputStream());
        final BCTLSSocketStreamTlsClient client = new BCTLSSocketStreamTlsClient(hostName, sniEnabled, options);
        protocol.connect(client);
        final Integer selectedCipherSuite = client.getSelectedCipherSuite();
        final String selectedCipherSuiteName = getCipherSuiteName(selectedCipherSuite);
        return new BCSSLSocketStreamInterface() {
            @Override
            public Socket getSocket() {
                return getParentSocketStream().getSocket();
            }

            @Override
            public SocketStreamInterface getParentSocketStream() {
                return socketStream;
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return protocol.getOutputStream();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return protocol.getInputStream();
            }

            @Override
            public void close() throws IOException {
                try {
                    protocol.close();
                } finally {
                    final Socket socket = getSocket();
                    if (socket != null) {
                        socket.close();
                    }
                }
            }

            @Override
            public String getCipherSuite() {
                return getInfo() + "|" + getTlsClient().getCrypto() + "|Protocol:" + getTlsClient().getClientVersion() + "|CipherSuite:" + selectedCipherSuiteName;
            }

            @Override
            public SSLSocketStreamOptions getOptions() {
                return options;
            }

            @Override
            public TlsClientProtocol getTlsClientProtocol() {
                return protocol;
            }

            @Override
            public BCTLSSocketStreamTlsClient getTlsClient() {
                return client;
            }

            @Override
            public SSLSocketStreamFactory getSSLSocketStreamFactory() {
                return BCSSLSocketStreamFactory.this;
            }
        };
    }

    public String getInfo() {
        return BC.getInfo();
    }

    @Override
    public String retry(SSLSocketStreamOptions options, Exception e) {
        if (StringUtils.containsIgnoreCase(e.getMessage(), "protocol_version")) {
            // https://www.bouncycastle.org/docs/tlsdocs1.5on/org/bouncycastle/tls/AlertDescription.html
            if (options.getCustomFactorySettings().add(TLS13_ENABLED)) {
                // retry with TLS1.3 enabled
                return "enable TLS1.3";
            } else if (options.enableNextDisabledCipher("GCM") != null) {
                // retry with TLS1.3 and GCM
                return "enable GCM for TLS1.3";
            }
        }
        return null;
    }
}
