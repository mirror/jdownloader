package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.pluginsinc.upnp.cling.StreamClientImpl;

import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxyUtils;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.fourthline.cling.support.igd.callback.PortMappingAdd;
import org.fourthline.cling.support.model.PortMapping;
import org.fourthline.cling.transport.impl.StreamClientConfigurationImpl;
import org.jdownloader.api.DeprecatedAPIServer;
import org.jdownloader.api.DeprecatedAPIServer.AutoSSLHttpConnectionFactory;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.DIRECTMODE;
import org.jdownloader.api.myjdownloader.api.MyJDownloaderAPI;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderDirectServer extends Thread {
    private ServerSocket                     currentServerSocket = null;
    private final MyJDownloaderConnectThread connectThread;
    private final DIRECTMODE                 connectMode;
    private final LogSource                  logger;
    private int                              upnpPort            = -1;

    public MyJDownloaderDirectServer(MyJDownloaderConnectThread connectThread, DIRECTMODE connectMode) {
        this.connectThread = connectThread;
        this.connectMode = connectMode;
        logger = connectThread.getLogger();
        setName("MyJDownloaderDirectServer");
        setDaemon(true);
    }

    public DIRECTMODE getConnectMode() {
        return connectMode;
    }

    private ServerSocket createServerSocket(int wished) throws IOException {
        int lastPort = CFG_MYJD.CFG.getLastLocalPort();
        if (lastPort <= 0 || lastPort > 65000) {
            lastPort = 0;
        }
        if (wished > 0 && wished < 65000) {
            lastPort = wished;
        }
        try {
            final ServerSocket currentServerSocket = new ServerSocket(lastPort);
            CFG_MYJD.CFG.setLastLocalPort(currentServerSocket.getLocalPort());
            return currentServerSocket;
        } catch (final Throwable e) {
            logger.log(e);
        }
        final ServerSocket currentServerSocket = new ServerSocket(0);
        CFG_MYJD.CFG.setLastLocalPort(currentServerSocket.getLocalPort());
        return currentServerSocket;
    }

    public static boolean sameNetwork(String ip1, InetAddress ip2, InetAddress netMask) {
        try {
            final byte[] ip1Bytes = InetAddress.getByName(ip1).getAddress();
            final byte[] ip2Bytes = ip2.getAddress();
            final byte[] maskBytes = netMask.getAddress();
            for (int i = 0; i < ip1Bytes.length; i++) {
                if ((ip1Bytes[i] & maskBytes[i]) != (ip2Bytes[i] & maskBytes[i])) {
                    return false;
                }
            }
            return true;
        } catch (final Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    private static int randomPort() {
        final int min = 1025;
        final int max = 65000;
        return new Random().nextInt((max - min) + 1) + min;
    }

    private int setUPNPPort() throws InterruptedException {
        final UDAServiceType[] udaServices = new UDAServiceType[] { new UDAServiceType("WANPPPConnection"), new UDAServiceType("WANIPConnection") };
        UpnpServiceImpl upnpService = null;
        try {
            final DefaultUpnpServiceConfiguration config = new DefaultUpnpServiceConfiguration() {
                @Override
                public Executor getMulticastReceiverExecutor() {
                    return super.getMulticastReceiverExecutor();
                }

                @Override
                public Integer getRemoteDeviceMaxAgeSeconds() {
                    return super.getRemoteDeviceMaxAgeSeconds();
                }

                @Override
                public StreamClientImpl createStreamClient() {
                    return new StreamClientImpl(new StreamClientConfigurationImpl(getSyncProtocolExecutorService()));
                }
            };
            upnpService = new UpnpServiceImpl(config);
            upnpService.getControlPoint().search(15000);
            Thread.sleep(15000);
            final CopyOnWriteArrayList<InetAddress> localIPs = new CopyOnWriteArrayList<InetAddress>(HTTPProxyUtils.getLocalIPs());
            final InetAddress netMask = InetAddress.getByName("255.255.255.0");
            for (final UDAServiceType udaService : udaServices) {
                for (final Device device : upnpService.getRegistry().getDevices(udaService)) {
                    final Service service = device.findService(udaService);
                    if (service == null) {
                        continue;
                    }
                    String deviceIP = null;
                    DeviceDetails details = device.getDetails();
                    if (details.getBaseURL() != null) {
                        deviceIP = details.getBaseURL().getHost();
                    } else {
                        DeviceIdentity identity = device.getIdentity();
                        if (identity instanceof RemoteDeviceIdentity) {
                            RemoteDeviceIdentity remoteIdentity = (RemoteDeviceIdentity) identity;
                            deviceIP = remoteIdentity.getDescriptorURL().getHost();
                        }
                    }
                    if (deviceIP == null) {
                        continue;
                    }
                    Iterator<InetAddress> it = localIPs.iterator();
                    InetAddress localIP = null;
                    while (it.hasNext()) {
                        InetAddress next = it.next();
                        if (sameNetwork(deviceIP, next, netMask)) {
                            localIP = next;
                            break;
                        }
                    }
                    if (localIP == null) {
                        continue;
                    }
                    logger.info("Found Router at " + deviceIP + " for " + localIP.getHostAddress());
                    int upnpPort = CFG_MYJD.CFG.getLastUpnpPort();
                    final AtomicBoolean upnpPortMapped = new AtomicBoolean(false);
                    final AtomicInteger portMappingTry = new AtomicInteger(0);
                    while (portMappingTry.incrementAndGet() < 6) {
                        if (portMappingTry.get() > 1 || upnpPort <= 0) {
                            upnpPort = randomPort();
                        }
                        final PortMapping desiredMapping = new PortMapping(upnpPort, localIP.getHostAddress(), PortMapping.Protocol.TCP, "MyJDownloader");
                        desiredMapping.setInternalPort(new UnsignedIntegerTwoBytes(CFG_MYJD.CFG.getLastLocalPort()));
                        Future result = upnpService.getControlPoint().execute(new PortMappingAdd(service, desiredMapping) {
                            @Override
                            public void success(ActionInvocation invocation) {
                                upnpPortMapped.set(true);
                                logger.info("PortMapping(" + portMappingTry.get() + ") " + desiredMapping.getExternalPort() + " to " + desiredMapping.getInternalClient() + ":" + desiredMapping.getInternalPort() + " successful");
                            }

                            @Override
                            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                                logger.info("PortMapping(" + portMappingTry.get() + ") " + desiredMapping.getExternalPort() + " to " + desiredMapping.getInternalClient() + ":" + desiredMapping.getInternalPort() + " failed");
                            }
                        });
                        result.get();
                        if (upnpPortMapped.get()) {
                            CFG_MYJD.CFG.setLastUpnpPort(upnpPort);
                            return upnpPort;
                        }
                    }
                }
            }
            return -1;
        } catch (final Throwable e) {
            logger.log(e);
            return -1;
        } finally {
            try {
                if (upnpService != null) {
                    upnpService.shutdown();
                }
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void run() {
        try {
            int lastLocalPort = CFG_MYJD.CFG.getLastLocalPort();
            switch (connectMode) {
            case LAN:
                currentServerSocket = createServerSocket(-1);
                logger.info("MyJDownloaderDirectConnectionServer: Mode=" + connectMode + " LocalPort=" + currentServerSocket.getLocalPort() + "(" + lastLocalPort + ")");
                break;
            case LAN_WAN_MANUAL:
                currentServerSocket = createServerSocket(CFG_MYJD.CFG.getManualLocalPort());
                logger.info("MyJDownloaderDirectConnectionServer: Mode=" + connectMode + " RemotePort=" + CFG_MYJD.CFG.getManualRemotePort() + " LocalPort=" + currentServerSocket.getLocalPort() + "(" + lastLocalPort + "|" + CFG_MYJD.CFG.getManualLocalPort() + ")");
                break;
            case LAN_WAN_UPNP:
                currentServerSocket = createServerSocket(-1);
                upnpPort = setUPNPPort();
                logger.info("MyJDownloaderDirectConnectionServer: Mode=" + connectMode + " RemotePort=" + upnpPort + " LocalPort=" + currentServerSocket.getLocalPort() + "(" + lastLocalPort + ")");
                break;
            default:
                return;
            }
            while (connectThread.isAlive()) {
                Socket clientSocket = null;
                try {
                    clientSocket = currentServerSocket.accept();
                    if (connectMode == DIRECTMODE.LAN && !IP.isLocalIP(((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress().getHostAddress())) {
                        clientSocket.close();
                        continue;
                    }
                    clientSocket.setReuseAddress(true);
                    clientSocket.setSoTimeout(180000);
                    clientSocket.setTcpNoDelay(true);
                    handleConnection(clientSocket);
                } catch (Throwable e) {
                    if (clientSocket != null) {
                        try {
                            clientSocket.close();
                        } catch (final Throwable ignore) {
                        }
                    } else {
                        throw e;
                    }
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
        } finally {
            try {
                currentServerSocket.close();
            } catch (final Throwable e) {
            }
        }
    }

    protected void close() {
        try {
            currentServerSocket.close();
        } catch (final Throwable e) {
        }
    }

    public int getLocalPort() {
        if (currentServerSocket == null) {
            return -1;
        }
        return currentServerSocket.getLocalPort();
    }

    public int getRemotePort() {
        if (currentServerSocket == null) {
            return -1;
        }
        switch (connectMode) {
        case LAN_WAN_MANUAL:
            return CFG_MYJD.CFG.getManualRemotePort();
        case LAN_WAN_UPNP:
            return upnpPort;
        default:
            return -1;
        }
    }

    protected void handleConnection(final Socket clientSocket) throws IOException {
        final MyJDownloaderAPI api = connectThread.getApi();
        if (api == null) {
            throw new IOException("api no longer available");
        }
        final long requestNumber = connectThread.THREADCOUNTER.incrementAndGet();
        final Thread connectionThread = new Thread("MyJDownloaderDirectConnection:" + requestNumber) {
            @Override
            public void run() {
                try {
                    final HttpConnection httpConnection = DeprecatedAPIServer.autoWrapSSLConnection(clientSocket, new AutoSSLHttpConnectionFactory() {
                        @Override
                        public MyJDownloaderDirectHttpConnection create(Socket clientSocket, InputStream is, OutputStream os) throws IOException {
                            return new MyJDownloaderDirectHttpConnection(clientSocket, is, os, api);
                        }
                    });
                    if (httpConnection != null) {
                        httpConnection.run();
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                } finally {
                    try {
                        clientSocket.close();
                    } catch (final Throwable e) {
                    }
                    synchronized (MyJDownloaderConnectThread.getOpenconnections()) {
                        MyJDownloaderConnectThread.getOpenconnections().remove(Thread.currentThread());
                    }
                    connectThread.setEstablishedConnections(connectThread.getEstablishedConnections());
                }
            }
        };
        synchronized (MyJDownloaderConnectThread.getOpenconnections()) {
            MyJDownloaderConnectThread.getOpenconnections().put(connectionThread, new SocketStreamInterface() {
                @Override
                public Socket getSocket() {
                    return clientSocket;
                }

                @Override
                public OutputStream getOutputStream() throws IOException {
                    return clientSocket.getOutputStream();
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return clientSocket.getInputStream();
                }

                @Override
                public void close() throws IOException {
                    clientSocket.close();
                }
            });
        }
        connectThread.setEstablishedConnections(connectThread.getEstablishedConnections());
        connectionThread.setDaemon(true);
        connectionThread.start();
    }
}
