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
import java.util.HashMap;
import java.util.Hashtable;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.SSLSocketStreamFactory;
import org.appwork.utils.net.httpconnection.SSLSocketStreamInterface;
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

    private static final HashMap<Integer, String> CIPHERSUITENAMES = new HashMap<Integer, String>();
    {
        try {
            final Field[] fields = CipherSuite.class.getFields();
            for (Field field : fields) {
                final int cipherSuite = field.getInt(null);
                CIPHERSUITENAMES.put(cipherSuite, field.getName());
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    private static class BCTLSSocketStreamTlsClient extends DefaultTlsClient {

        private final String hostName;

        private BCTLSSocketStreamTlsClient(final String hostName) {
            this.hostName = hostName;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Hashtable getClientExtensions() throws IOException {
            Hashtable clientExtensions = super.getClientExtensions();
            if (clientExtensions == null) {
                clientExtensions = TlsExtensionsUtils.ensureExtensionsInitialised(clientExtensions);
            }
            // rfc3546
            if (StringUtils.isNotEmpty(hostName) && !hostName.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
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

    @Override
    public SSLSocketStreamInterface create(final SocketStreamInterface socketStream, final String hostName, final int port, final boolean autoclose, final boolean trustAll) throws IOException {
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        final TlsClientProtocol protocol = new TlsClientProtocol(socketStream.getInputStream(), socketStream.getOutputStream(), secureRandom);
        final BCTLSSocketStreamTlsClient client = new BCTLSSocketStreamTlsClient(hostName);
        protocol.connect(client);
        final String cipherSuite;
        final Integer selectedCipherSuite = client.getSelectedCipherSuite();
        if (CIPHERSUITENAMES.containsKey(selectedCipherSuite)) {
            cipherSuite = CIPHERSUITENAMES.get(selectedCipherSuite);
        } else {
            cipherSuite = selectedCipherSuite.toString();
        }
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
                return cipherSuite;
            }
        };
    }
}
