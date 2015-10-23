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
import java.util.HashMap;

import org.appwork.utils.net.httpconnection.SSLSocketStreamFactory;
import org.appwork.utils.net.httpconnection.SSLSocketStreamInterface;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;
import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.crypto.tls.DefaultTlsClient;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsClientProtocol;
import org.bouncycastle.crypto.tls.TlsCredentials;

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

        @Override
        public TlsAuthentication getAuthentication() throws IOException {
            TlsAuthentication auth = new TlsAuthentication() {
                // Capture the server certificate information!
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
    public SSLSocketStreamInterface create(final SocketStreamInterface socketStream, String host, int port, boolean autoclose, boolean trustAll) throws IOException {
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        final TlsClientProtocol protocol = new TlsClientProtocol(socketStream.getInputStream(), socketStream.getOutputStream(), secureRandom);
        final BCTLSSocketStreamTlsClient client = new BCTLSSocketStreamTlsClient();
        protocol.connect(client);
        final String cipherSuite = CIPHERSUITENAMES.get(client.getSelectedCipherSuite());
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
                    socketStream.getSocket().close();
                }

            }

            @Override
            public String getCipherSuite() {
                return cipherSuite;
            }
        };
    }

}
