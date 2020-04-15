package org.jdownloader.api;

import java.io.EOFException;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
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
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.IDNUtil;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.DefaultTlsServer;
import org.bouncycastle.tls.HashAlgorithm;
import org.bouncycastle.tls.NameType;
import org.bouncycastle.tls.ServerName;
import org.bouncycastle.tls.SignatureAlgorithm;
import org.bouncycastle.tls.SignatureAndHashAlgorithm;
import org.bouncycastle.tls.TlsCredentialedDecryptor;
import org.bouncycastle.tls.TlsCredentialedSigner;
import org.bouncycastle.tls.TlsExtensionsUtils;
import org.bouncycastle.tls.TlsServerProtocol;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedDecryptor;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.bouncycastle.util.Strings;
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

        protected final String[] getSubjects() {
            return subjects;
        }

        private final AsymmetricKeyParameter asymKeyParam;
        private final List<byte[]>           x509 = new ArrayList<byte[]>();
        private final String[]               subjects;

        protected APICert() {
            this.asymKeyParam = null;
            this.subjects = null;
            this.keyPair = null;
        }

        protected final List<byte[]> getX509() {
            return x509;
        }

        protected APICert(final KeyPair keyPair, final X509Certificate x509, final String[] subjects) throws CertificateEncodingException, IOException {
            this.keyPair = keyPair;
            this.asymKeyParam = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
            this.x509.add(x509.getEncoded());
            this.subjects = subjects.clone();
        }

        protected CertStorable toCertStorable() throws CertificateEncodingException, IOException {
            if (keyPair != null && x509.size() == 1) {
                final CertStorable ret = new CertStorable();
                ret.setCert(HexFormatter.byteArrayToHex(x509.get(0)));
                ret.setPrivateKey(HexFormatter.byteArrayToHex(keyPair.getPrivate().getEncoded()));
                ret.setPublicKey(HexFormatter.byteArrayToHex(keyPair.getPublic().getEncoded()));
                ret.setSubjects(subjects);
                return ret;
            } else {
                return null;
            }
        }

        protected APICert(final CertStorable certStorable) throws Exception {
            this.x509.add(HexFormatter.hexToByteArray(certStorable.getCert()));
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

    protected static final APICert getAPICert(final String serverName) throws Exception {
        final String cn;
        if (serverName != null) {
            if (serverName.matches("(?i)^\\d+-\\d+-\\d+-\\d+.mydns.jdownloader.org$") || serverName.matches("(?i)^[a-fA-F0-9]{8}.mydns.jdownloader.org$") || serverName.matches("(?i)^[a-fA-F0-9]{32}.mydns.jdownloader.org$")) {
                cn = "*.mydns.jdownloader.org";
            } else {
                cn = serverName;
            }
        } else {
            cn = "localhost";
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
            APICert apiCert = APICERTS.get(cn);
            if (apiCert == null) {
                final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048);
                final KeyPair keyPair = keyPairGenerator.genKeyPair();
                final X509Certificate cert = createSelfSignedCertificate(keyPair, cn);
                final X509CertificateHolder holder = new X509CertificateHolder(cert.getEncoded());
                final GeneralNames subjectAltNames = GeneralNames.fromExtensions(holder.getExtensions(), Extension.subjectAlternativeName);
                final List<String> subjects = new ArrayList<String>();
                for (final GeneralName subjectAltName : subjectAltNames.getNames()) {
                    switch (subjectAltName.getTagNo()) {
                    case GeneralName.dNSName:
                        subjects.add(subjectAltName.getName().toString());
                        break;
                    case GeneralName.iPAddress:
                        subjects.add(InetAddress.getByAddress(DEROctetString.getInstance(subjectAltName.getName()).getOctets()).getHostAddress());
                        break;
                    }
                }
                apiCert = new APICert(keyPair, cert, subjects.toArray(new String[0]));
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

    protected static X509Certificate createSelfSignedCertificate(final KeyPair keyPair, final String cn) throws Exception {
        final long now = System.currentTimeMillis();
        final Date notBefore = new Date(now - (24 * 60 * 60 * 1000l));
        final Date notAfter = new Date(now + (50 * 365 * 24 * 60 * 60 * 1000l));
        final ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
        final X500Name x500Name = new X500Name("CN=Self signed certificate for local JDownloader@" + System.getProperty("user.name", "User"));
        final JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        final X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(x500Name, BigInteger.valueOf(now), notBefore, notAfter, x500Name, keyPair.getPublic());
        certificateBuilder.addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(keyPair.getPublic()));
        certificateBuilder.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(keyPair.getPublic()));
        certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certificateBuilder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment));
        final List<GeneralName> alternativeNames = new ArrayList<GeneralName>();
        if (StringUtils.equalsIgnoreCase("localhost", cn)) {
            alternativeNames.add(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
            alternativeNames.add(new GeneralName(GeneralName.dNSName, cn));
        } else if (cn.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            alternativeNames.add(new GeneralName(GeneralName.iPAddress, cn));
            alternativeNames.add(new GeneralName(GeneralName.dNSName, "localhost"));
        } else {
            alternativeNames.add(new GeneralName(GeneralName.dNSName, cn));
            alternativeNames.add(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
            alternativeNames.add(new GeneralName(GeneralName.dNSName, "localhost"));
        }
        final GeneralNames subjectAltNames = new GeneralNames(alternativeNames.toArray(new GeneralName[0]));
        certificateBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        return new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate(certificateBuilder.build(contentSigner));
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
                final TlsServerProtocol tlsServerProtocol = new TlsServerProtocol(clientSocketIS, clientSocket.getOutputStream()) {
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
                tlsServerProtocol.accept(new DefaultTlsServer(new BcTlsCrypto(new SecureRandom())) {
                    private Certificate            cert     = null;
                    private AsymmetricKeyParameter keyParam = null;

                    @Override
                    public void notifySecureRenegotiation(boolean arg0) throws IOException {
                    }

                    // rfc3546
                    protected String getServerName(Hashtable arg0) throws IOException {
                        final Vector<ServerName> serverNames = TlsExtensionsUtils.getServerNameExtensionClient(arg0);
                        if (serverNames != null) {
                            for (final ServerName serverName : serverNames) {
                                if (serverName.getNameType() == NameType.host_name) {
                                    final String name = IDNUtil.toASCII(Strings.fromUTF8ByteArray(serverName.getNameData()), IDNUtil.USE_STD3_ASCII_RULES);
                                    return name;
                                }
                            }
                        }
                        return null;
                    }

                    @Override
                    protected boolean allowEncryptThenMAC() {
                        return false;
                    }

                    @Override
                    public void processClientExtensions(Hashtable arg0) throws IOException {
                        String serverName = getServerName(arg0);
                        if (serverName == null) {
                            serverName = clientSocket.getLocalAddress().getHostAddress();
                        }
                        super.processClientExtensions(arg0);
                        try {
                            final APICert apiCert = getAPICert(serverName);
                            final List<TlsCertificate> certificates = new ArrayList<TlsCertificate>();
                            for (final byte[] certificiate : apiCert.getX509()) {
                                certificates.add(context.getCrypto().createCertificate(certificiate));
                            }
                            cert = new Certificate(certificates.toArray(new TlsCertificate[0]));
                            keyParam = apiCert.getAsymKeyParam();
                        } catch (Exception e) {
                            LogController.CL(DeprecatedAPIServer.class).log(e);
                            throw new IOException(e);
                        }
                    }

                    @Override
                    protected TlsCredentialedDecryptor getRSAEncryptionCredentials() throws IOException {
                        return new BcDefaultTlsCredentialedDecryptor((BcTlsCrypto) getCrypto(), cert, keyParam);
                    }

                    @Override
                    protected TlsCredentialedSigner getRSASignerCredentials() throws IOException {
                        final SignatureAndHashAlgorithm signatureAndHashAlgorithm = new SignatureAndHashAlgorithm(HashAlgorithm.sha256, SignatureAlgorithm.rsa);
                        // SignatureAndHashAlgorithm needed for TLS1.2
                        return new BcDefaultTlsCredentialedSigner(new TlsCryptoParameters(context), (BcTlsCrypto) getCrypto(), keyParam, cert, signatureAndHashAlgorithm);
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
