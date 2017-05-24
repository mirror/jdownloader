package org.jdownloader.api;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.x500.X500Principal;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.HttpConnection.HttpConnectionType;
import org.appwork.utils.net.httpserver.HttpHandlerInfo;
import org.appwork.utils.net.httpserver.HttpServer;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HeadRequest;
import org.appwork.utils.net.httpserver.requests.OptionsRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.tls.DefaultTlsServer;
import org.bouncycastle.crypto.tls.DefaultTlsSignerCredentials;
import org.bouncycastle.crypto.tls.ExtensionType;
import org.bouncycastle.crypto.tls.HashAlgorithm;
import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.bouncycastle.crypto.tls.SignatureAlgorithm;
import org.bouncycastle.crypto.tls.SignatureAndHashAlgorithm;
import org.bouncycastle.crypto.tls.TlsFatalAlert;
import org.bouncycastle.crypto.tls.TlsServerProtocol;
import org.bouncycastle.crypto.tls.TlsSignerCredentials;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.jdownloader.logging.LogController;

public class DeprecatedAPIServer extends HttpServer {
    protected static class APICert {
        private final KeyPair keyPair;

        protected final KeyPair getKeyPair() {
            return keyPair;
        }

        protected final AsymmetricKeyParameter getAsymKeyParam() {
            return asymKeyParam;
        }

        protected final Certificate getCert() {
            return cert;
        }

        protected final String[] getSubjects() {
            return subjects;
        }

        private final AsymmetricKeyParameter asymKeyParam;
        private final byte[]                 x509;
        private final Certificate            cert;
        private final String[]               subjects;

        protected APICert() {
            this.asymKeyParam = null;
            this.cert = null;
            this.subjects = null;
            this.x509 = null;
            this.keyPair = null;
        }

        protected APICert(final KeyPair keyPair, final X509Certificate x509, final String[] subjects) throws CertificateEncodingException, IOException {
            this.keyPair = keyPair;
            this.asymKeyParam = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
            this.x509 = x509.getEncoded();
            this.cert = new Certificate(new org.bouncycastle.asn1.x509.Certificate[] { org.bouncycastle.asn1.x509.Certificate.getInstance(this.x509) });
            this.subjects = subjects.clone();
        }

        protected CertStorable toCertStorable() throws CertificateEncodingException, IOException {
            if (keyPair != null) {
                final CertStorable ret = new CertStorable();
                ret.setCert(HexFormatter.byteArrayToHex(x509));
                ret.setPrivateKey(HexFormatter.byteArrayToHex(keyPair.getPrivate().getEncoded()));
                ret.setPublicKey(HexFormatter.byteArrayToHex(keyPair.getPublic().getEncoded()));
                ret.setSubjects(subjects);
                return ret;
            }
            return null;
        }

        protected APICert(final CertStorable certStorable) throws CertificateEncodingException, IOException, InvalidKeySpecException, NoSuchAlgorithmException {
            this.x509 = HexFormatter.hexToByteArray(certStorable.getCert());
            this.cert = new Certificate(new org.bouncycastle.asn1.x509.Certificate[] { org.bouncycastle.asn1.x509.Certificate.getInstance(x509) });
            final PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(HexFormatter.hexToByteArray(certStorable.getPublicKey())));
            final PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(HexFormatter.hexToByteArray(certStorable.getPrivateKey())));
            this.keyPair = new KeyPair(publicKey, privateKey);
            this.asymKeyParam = PrivateKeyFactory.createKey(privateKey.getEncoded());
            this.subjects = certStorable.getSubjects();
        }
    }

    public static final class DeprecatedPostRequest extends PostRequest implements DeprecatedAPIRequestInterface {
        public DeprecatedPostRequest(HttpConnection connection) {
            super(connection);
        }
    }

    public static final class DeprecatedOptionsRequest extends OptionsRequest implements DeprecatedAPIRequestInterface {
        public DeprecatedOptionsRequest(HttpConnection connection) {
            super(connection);
        }
    }

    public static final class DeprecatedHeadRequest extends HeadRequest implements DeprecatedAPIRequestInterface {
        public DeprecatedHeadRequest(HttpConnection connection) {
            super(connection);
        }
    }

    public static final class DeprecatedGetRequest extends GetRequest implements DeprecatedAPIRequestInterface {
        public DeprecatedGetRequest(HttpConnection connection) {
            super(connection);
        }
    }

    public DeprecatedAPIServer(int port) {
        super(port);
    }

    @Override
    public HttpHandlerInfo registerRequestHandler(HttpRequestHandler handler) {
        return super.registerRequestHandler(handler);
    }

    public class CustomHttpConnection extends HttpConnection {
        protected CustomHttpConnection(HttpServer server, Socket clientSocket, InputStream is, OutputStream os) throws IOException {
            super(server, clientSocket, is, os);
        }

        protected GetRequest buildGetRequest() {
            return new DeprecatedGetRequest(this);
        }

        protected HeadRequest buildHeadRequest() {
            return new DeprecatedHeadRequest(this);
        }

        protected OptionsRequest buildOptionsRequest() {
            return new DeprecatedOptionsRequest(this);
        }

        protected PostRequest buildPostRequest() {
            return new DeprecatedPostRequest(this);
        }

        public CustomHttpConnection(HttpServer server, Socket clientSocket) throws IOException {
            super(server, clientSocket);
        }
    }

    private static final HashMap<String, APICert> APICERTS           = new HashMap<String, APICert>();
    private static final File                     APICERTSFILE       = Application.getTempResource("myjd.certs");
    private static final AtomicBoolean            APICERTSFILELOADED = new AtomicBoolean(false);

    protected static final APICert getAPICert(final String serverName) throws NoSuchAlgorithmException, CertificateEncodingException, InvalidKeyException, IllegalStateException, SignatureException, IOException {
        final String name;
        if (serverName != null) {
            if (serverName.matches("(?i)^\\d+-\\d+-\\d+-\\d+.mydns.jdownloader.org$") || serverName.matches("(?i)^[a-fA-F0-9]{8}.mydns.jdownloader.org$") || serverName.matches("(?i)^[a-fA-F0-9]{32}.mydns.jdownloader.org$")) {
                name = "*.mydns.jdownloader.org";
            } else {
                name = serverName;
            }
        } else {
            name = "localhost";
        }
        synchronized (APICERTS) {
            if (APICERTSFILELOADED.compareAndSet(false, true)) {
                if (APICERTSFILE.exists()) {
                    try {
                        // load apiCerts
                        final List<CertStorable> certStorables = JSonStorage.restoreFrom(APICERTSFILE, false, JSonStorage.KEY, new TypeRef<ArrayList<CertStorable>>() {
                        }, null);
                        if (certStorables != null) {
                            for (final CertStorable certStorable : certStorables) {
                                try {
                                    final APICert apiCert = new APICert(certStorable);
                                    final String[] subjects = apiCert.getSubjects();
                                    if (subjects != null) {
                                        for (final String subject : subjects) {
                                            if (!APICERTS.containsKey(subject)) {
                                                APICERTS.put(subject, apiCert);
                                            }
                                        }
                                    }
                                } catch (final Throwable e) {
                                    LogController.CL(DeprecatedAPIServer.class).log(e);
                                }
                            }
                        }
                    } catch (final Throwable e) {
                        LogController.CL(DeprecatedAPIServer.class).log(e);
                    }
                }
            }
            APICert apiCert = APICERTS.get(name);
            if (apiCert == null) {
                final long currentTimeMillis = System.currentTimeMillis();
                final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048, new SecureRandom());
                final KeyPair keyPair = keyPairGenerator.genKeyPair();
                final X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
                certGen.setSerialNumber(BigInteger.valueOf(currentTimeMillis));
                final X500Principal dnName = new X500Principal("CN=Self signed certificate for local JDownloader@" + System.getProperty("user.name", "User"));
                certGen.setIssuerDN(dnName);
                certGen.setSubjectDN(dnName); // note: same as issuer
                certGen.setNotBefore(new Date(currentTimeMillis - (2 * 24 * 60 * 60 * 1000l)));
                certGen.setNotAfter(new Date(currentTimeMillis + (50 * 365 * 24 * 60 * 60 * 1000l)));
                certGen.setPublicKey(keyPair.getPublic());
                certGen.setSignatureAlgorithm("SHA1withRSA");
                final ASN1EncodableVector alternativeNames = new ASN1EncodableVector();
                final String[] subjects;
                if (name.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                    alternativeNames.add(new GeneralName(GeneralName.iPAddress, name));
                    subjects = new String[] { name };
                } else {
                    alternativeNames.add(new GeneralName(GeneralName.dNSName, name));
                    subjects = new String[] { name };
                }
                certGen.addExtension(X509Extensions.SubjectAlternativeName, false, new DERSequence(alternativeNames));
                final X509Certificate cert = certGen.generate(keyPair.getPrivate());
                apiCert = new APICert(keyPair, cert, subjects);
                for (final String subject : subjects) {
                    if (!APICERTS.containsKey(subject)) {
                        APICERTS.put(subject, apiCert);
                    }
                }
                final List<CertStorable> certStorables = new ArrayList<CertStorable>();
                for (final APICert toStorable : new HashSet<APICert>(APICERTS.values())) {
                    final CertStorable storable = toStorable.toCertStorable();
                    if (storable != null) {
                        certStorables.add(storable);
                    }
                }
                final byte[] json = JSonStorage.getMapper().objectToByteArray(certStorables);
                final Runnable run = new Runnable() {
                    @Override
                    public void run() {
                        JSonStorage.saveTo(APICERTSFILE, false, JSonStorage.KEY, json);
                    }
                };
                StorageHandler.enqueueWrite(run, APICERTSFILE.getAbsolutePath(), true);
            }
            return apiCert;
        }
    }

    public static interface AutoSSLHttpConnectionFactory {
        HttpConnection create(Socket clientSocket, InputStream is, OutputStream os) throws IOException;
    }

    public static HttpConnection autoWrapSSLConnection(final Socket clientSocket, AutoSSLHttpConnectionFactory factory) throws IOException {
        boolean finallyCloseSocket = true;
        try {
            clientSocket.setSoTimeout(60 * 1000);
            final InputStream is = clientSocket.getInputStream();
            final byte[] guessProtocolBuffer = new byte[8];
            int index = 0;
            for (index = 0; index < 8; index++) {
                final int read = is.read();
                if (read == -1) {
                    if (index == 0) {
                        return null;
                    } else {
                        throw new EOFException("guess protocol failed: " + index);
                    }
                }
                guessProtocolBuffer[index] = (byte) read;
            }
            final HttpConnectionType httpConnectionType = HttpConnectionType.get(guessProtocolBuffer);
            final PushbackInputStream clientSocketIS = new PushbackInputStream(is, 8);
            clientSocketIS.unread(guessProtocolBuffer, 0, index);
            final InputStream httpIS;
            final OutputStream httpOS;
            if (!HttpConnectionType.UNKNOWN.equals(httpConnectionType)) {
                // http
                httpIS = clientSocketIS;
                httpOS = clientSocket.getOutputStream();
            } else {
                // https
                final TlsServerProtocol tlsServerProtocol = new TlsServerProtocol(clientSocketIS, clientSocket.getOutputStream(), new SecureRandom()) {
                    @Override
                    protected void failWithError(short arg0, short arg1, String arg2, Throwable arg3) throws IOException {
                        if (true) {
                            super.failWithError(arg0, arg1, arg2, arg3);
                        } else {
                            // see modified getInputStream
                            if (arg3 instanceof TlsFatalAlert || !(arg3 instanceof IOException)) {
                                super.failWithError(arg0, arg1, arg2, arg3);
                            } else if (arg3 instanceof IOException) {
                                if ("Failed to read record".equals(arg2)) {
                                    // ignore
                                } else {
                                    super.failWithError(arg0, arg1, arg2, arg3);
                                }
                            }
                        }
                    }

                    InputStream modifiedTlsInputStream = null;

                    @Override
                    public InputStream getInputStream() {
                        if (modifiedTlsInputStream == null && super.getInputStream() != null) {
                            /*
                             * customized InputStream with optimized implementation of available to provide support for non blocking read
                             */
                            modifiedTlsInputStream = new FilterInputStream(super.getInputStream()) {
                                public int available() throws IOException {
                                    final int dataAvailable = applicationDataAvailable();
                                    if (dataAvailable > 0) {
                                        return dataAvailable;
                                    } else {
                                        final int socketAvailable = clientSocketIS.available();
                                        if (socketAvailable >= 5) {
                                            // recordHeader must be minimum 5 long
                                            // TlsProtocol.readApplicationData -> safeReadRecord -> recordStream.readRecord() -> byte[]
                                            // recordHeader = TlsUtils.readAllOrNothing(5, input);
                                            return socketAvailable;
                                        } else {
                                            return 0;
                                        }
                                    }
                                };
                            };
                        }
                        return modifiedTlsInputStream;
                    }
                };
                tlsServerProtocol.accept(new DefaultTlsServer() {
                    private Certificate            cert     = null;
                    private AsymmetricKeyParameter keyParam = null;

                    @Override
                    public void notifySecureRenegotiation(boolean arg0) throws IOException {
                    }

                    // rfc3546
                    protected String getServerName(Hashtable arg0) throws IOException {
                        final Object serverNameBytes = arg0.get(ExtensionType.server_name);
                        if (serverNameBytes != null && serverNameBytes instanceof byte[]) {
                            final DataInputStream is = new DataInputStream(new ByteArrayInputStream((byte[]) serverNameBytes));
                            final int snl = is.readShort();
                            if (snl > 0) {
                                final byte type = is.readByte(); // name type
                                if (type == 0) {
                                    // hostname
                                    final int length = is.readShort(); // name size
                                    if (length > 0) {
                                        final byte[] nameBytes = new byte[length];
                                        is.readFully(nameBytes, 0, length); // name bytes, UTF-8
                                        return new String(nameBytes, "UTF-8");
                                    }
                                }
                            }
                        }
                        return null;
                    }

                    @Override
                    public void processClientExtensions(Hashtable arg0) throws IOException {
                        String serverName = getServerName(arg0);
                        if (serverName == null) {
                            serverName = clientSocket.getLocalAddress().getHostAddress();
                        }
                        super.processClientExtensions(arg0);
                        try {
                            APICert apiCert = getAPICert(serverName);
                            cert = apiCert.getCert();
                            keyParam = apiCert.getAsymKeyParam();
                        } catch (Throwable e) {
                            LogController.CL(DeprecatedAPIServer.class).log(e);
                            throw new IOException(e);
                        }
                    }

                    protected TlsSignerCredentials getRSASignerCredentials() throws IOException {
                        // SignatureAndHashAlgorithm needed for TLS1.2
                        final SignatureAndHashAlgorithm signatureAndHashAlgorithm = new SignatureAndHashAlgorithm(HashAlgorithm.sha256, SignatureAlgorithm.rsa);
                        return new DefaultTlsSignerCredentials(context, cert, keyParam, signatureAndHashAlgorithm);
                    }

                    protected org.bouncycastle.crypto.tls.ProtocolVersion getMaximumVersion() {
                        // signal TLS1.2 support
                        return ProtocolVersion.TLSv12;
                    };
                });
                httpIS = tlsServerProtocol.getInputStream();
                httpOS = tlsServerProtocol.getOutputStream();
            }
            finallyCloseSocket = false;
            return factory.create(clientSocket, httpIS, httpOS);
        } finally {
            try {
                if (finallyCloseSocket) {
                    clientSocket.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    @Override
    protected Runnable createConnectionHandler(final Socket clientSocket) throws IOException {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    final HttpConnection httpConnection = autoWrapSSLConnection(clientSocket, new AutoSSLHttpConnectionFactory() {
                        @Override
                        public HttpConnection create(Socket clientSocket, InputStream is, OutputStream os) throws IOException {
                            return new CustomHttpConnection(DeprecatedAPIServer.this, clientSocket, is, os);
                        }
                    });
                    if (httpConnection != null) {
                        httpConnection.run();
                    }
                } catch (Throwable e) {
                    LogController.CL(DeprecatedAPIServer.class).log(e);
                }
            }
        };
    }
}
