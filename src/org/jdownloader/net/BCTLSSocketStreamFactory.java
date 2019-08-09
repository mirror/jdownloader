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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.SSLSocketStreamFactory;
import org.appwork.utils.net.httpconnection.SSLSocketStreamInterface;
import org.appwork.utils.net.httpconnection.SSLSocketStreamOptions;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;
import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.crypto.tls.DefaultTlsClient;
import org.bouncycastle.crypto.tls.ExtensionType;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsClientProtocol;
import org.bouncycastle.crypto.tls.TlsCredentials;
import org.bouncycastle.crypto.tls.TlsExtensionsUtils;

/**
 * @author daniel
 *
 */
public class BCTLSSocketStreamFactory implements SSLSocketStreamFactory {
    // raymii.org/s/tutorials/Strong_SSL_Security_On_nginx.html
    // openssl.org/docs/man1.0.1/apps/ciphers.html
    private static final String                   CIPHERS          = "EECDH+AESGCM:EDH+AESGCM:ECDHE-RSA-AES128-GCM-SHA256:AES256+EECDH:DHE-RSA-AES128-GCM-SHA256:AES256+EDH:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-SHA384:ECDHE-RSA-AES128-SHA256:ECDHE-RSA-AES256-SHA:ECDHE-RSA-AES128-SHA:DHE-RSA-AES256-SHA256:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-SHA:DHE-RSA-AES128-SHA:ECDHE-RSA-DES-CBC3-SHA:EDH-RSA-DES-CBC3-SHA:AES256-GCM-SHA384:AES128-GCM-SHA256:AES256-SHA256:AES128-SHA256:AES256-SHA:AES128-SHA:DES-CBC3-SHA:HIGH:!anon:!eNULL:!DHE:!SRP:!EXPORT:!DES:!MD5:!PSK:!RC4";
    private static final HashMap<Integer, String> CIPHERSUITENAMES = new HashMap<Integer, String>();
    private static int[]                          CIPHERSUITES;
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

    private static int[] getEnabledCipherSuites(int[] cipherSuites, Set<String> disabledCipherSuites) {
        if (disabledCipherSuites == null || disabledCipherSuites.size() == 0) {
            return cipherSuites;
        } else {
            final List<Integer> enabledCipherSuites = new ArrayList<Integer>();
            cipherSuites: for (final int cipherSuite : cipherSuites) {
                final String name = getCipherSuiteName(cipherSuite);
                for (String disabledCipherSuite : disabledCipherSuites) {
                    if (StringUtils.containsIgnoreCase(name, disabledCipherSuite)) {
                        continue cipherSuites;
                    }
                }
                enabledCipherSuites.add(cipherSuite);
            }
            final int[] ret = new int[enabledCipherSuites.size()];
            int index = 0;
            for (Integer enabledCipherSuite : enabledCipherSuites) {
                ret[index++] = enabledCipherSuite.intValue();
            }
            return ret;
        }
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
        private final String  hostName;
        private final int[]   enabledCipherSuites;
        private final boolean sniEnabled;

        private BCTLSSocketStreamTlsClient(final String hostName, final boolean sniEnabled, final int[] enabledCipherSuites) {
            this.hostName = hostName;
            this.enabledCipherSuites = enabledCipherSuites;
            this.sniEnabled = sniEnabled;
        }

        @Override
        public int[] getCipherSuites() {
            return enabledCipherSuites;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Hashtable getClientExtensions() throws IOException {
            Hashtable clientExtensions = super.getClientExtensions();
            if (clientExtensions == null) {
                clientExtensions = TlsExtensionsUtils.ensureExtensionsInitialised(clientExtensions);
            }
            // rfc3546
            if (sniEnabled && StringUtils.isNotEmpty(hostName) && !hostName.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                final ByteArrayOutputStream extBaos = new ByteArrayOutputStream();
                final DataOutputStream extOS = new DataOutputStream(extBaos);
                final byte[] hostnameBytes = this.hostName.getBytes("UTF-8");
                final int snl = hostnameBytes.length;
                // OpenSSL breaks if an extension with length "0" sent, they expect
                // at least an entry with length "0"
                extOS.writeShort(snl == 0 ? 0 : snl + 3); // entry size
                if (snl > 0) {
                    extOS.writeByte(0); // name type = hostname
                    extOS.writeShort(snl); // name size
                    if (snl > 0) {
                        extOS.write(hostnameBytes);
                    }
                }
                extOS.close();
                clientExtensions.put(ExtensionType.server_name, extBaos.toByteArray());
            }
            // can be used to customize
            // clientECPointFormats = new short[] { ECPointFormat.uncompressed };
            // namedCurves = new int[] { NamedCurve.secp256r1, NamedCurve.secp384r1 };
            // TlsECCUtils.addSupportedEllipticCurvesExtension(clientExtensions, namedCurves);
            // TlsECCUtils.addSupportedPointFormatsExtension(clientExtensions, clientECPointFormats);
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
                public void notifyServerCertificate(org.bouncycastle.crypto.tls.Certificate serverCertificate) throws IOException {
                }

                public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {
                    return null;
                }
            };
            return auth;
        }

        private int getSelectedCipherSuite() {
            return selectedCipherSuite;
        }
    }

    private static String getCipherSuiteName(Integer selectedCipherSuite) {
        if (CIPHERSUITENAMES.containsKey(selectedCipherSuite)) {
            return CIPHERSUITENAMES.get(selectedCipherSuite);
        } else {
            return selectedCipherSuite.toString();
        }
    }

    @Override
    public SSLSocketStreamInterface create(final SocketStreamInterface socketStream, final String hostName, final int port, final boolean autoclose, final SSLSocketStreamOptions options) throws IOException {
        final boolean sniEnabled = !StringUtils.isEmpty(hostName) && (options == null || options.isSNIEnabled());
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        final TlsClientProtocol protocol = new TlsClientProtocol(socketStream.getInputStream(), socketStream.getOutputStream(), secureRandom);
        final BCTLSSocketStreamTlsClient client = new BCTLSSocketStreamTlsClient(hostName, sniEnabled, getEnabledCipherSuites(CIPHERSUITES, options != null ? options.getDisabledCipherSuites() : null));
        protocol.connect(client);
        final Integer selectedCipherSuite = client.getSelectedCipherSuite();
        final String selectedCipherSuiteName = getCipherSuiteName(selectedCipherSuite);
        return new SSLSocketStreamInterface() {
            @Override
            public Socket getSocket() {
                return socketStream.getSocket();
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
                    if (socketStream.getSocket() != null) {
                        socketStream.getSocket().close();
                    }
                }
            }

            @Override
            public String getCipherSuite() {
                return selectedCipherSuiteName;
            }
        };
    }
}
