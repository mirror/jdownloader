package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.reconnect.pluginsinc.upnp.cling.StreamClientImpl;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.utils.Time;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.IPVERSION;
import org.appwork.utils.net.httpconnection.HTTPProxyUtils;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.igd.callback.PortMappingAdd;
import org.fourthline.cling.support.igd.callback.PortMappingDelete;
import org.fourthline.cling.support.model.PortMapping;
import org.fourthline.cling.transport.impl.StreamClientConfigurationImpl;
import org.jdownloader.api.DeprecatedAPIServer;
import org.jdownloader.api.DeprecatedAPIServer.AutoSSLHttpConnectionFactory;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.DIRECTMODE;
import org.jdownloader.api.myjdownloader.api.MyJDownloaderAPI;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderDirectServer extends Thread implements ShutdownVetoListener {
    private final AtomicReference<ServerSocket> currentServerSocket    = new AtomicReference<ServerSocket>(null);
    private final MyJDownloaderConnectThread    connectThread;
    private final DIRECTMODE                    connectMode;
    private final LogSource                     logger;
    private volatile PortMapping                upnpPortMapping        = null;
    private volatile Service                    upnpPortMappingService = null;

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
        ServerSocket currentServerSocket;
        try {
            currentServerSocket = new ServerSocket(lastPort);
        } catch (final IOException e) {
            logger.log(e);
            currentServerSocket = new ServerSocket(0);
        }
        CFG_MYJD.CFG.setLastLocalPort(currentServerSocket.getLocalPort());
        this.currentServerSocket.set(currentServerSocket);
        return currentServerSocket;
    }

    public static boolean sameNetwork(String ip1, InetAddress ip2, InetAddress netMask) {
        try {
            final byte[] ip1Bytes = HTTPConnectionUtils.resolvHostIP(ip1, IPVERSION.IPV4_ONLY)[0].getAddress();
            // TODO: Check/Add IPv6 Support. We speak IPv4-Only with Router
            final byte[] ip2Bytes = ip2.getAddress();
            if (ip1Bytes.length != ip2Bytes.length) {
                return false;
            }
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

    private void setupUPNPPort() {
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
            final AtomicInteger updateFlag = new AtomicInteger(0);
            upnpService.getControlPoint().getRegistry().addListener(new RegistryListener() {
                @Override
                public void remoteDeviceUpdated(Registry var1, RemoteDevice var2) {
                }

                @Override
                public void remoteDeviceRemoved(Registry var1, RemoteDevice var2) {
                }

                @Override
                public void remoteDeviceDiscoveryStarted(Registry var1, RemoteDevice var2) {
                }

                @Override
                public void remoteDeviceDiscoveryFailed(Registry var1, RemoteDevice var2, Exception var3) {
                }

                @Override
                public void remoteDeviceAdded(Registry var1, RemoteDevice var2) {
                    updateFlag.incrementAndGet();
                }

                @Override
                public void localDeviceRemoved(Registry var1, LocalDevice var2) {
                    updateFlag.incrementAndGet();
                }

                @Override
                public void localDeviceAdded(Registry var1, LocalDevice var2) {
                }

                @Override
                public void beforeShutdown(Registry var1) {
                }

                @Override
                public void afterShutdown() {
                }
            });
            for (final UDAServiceType udaService : udaServices) {
                upnpService.getControlPoint().search(new UDAServiceTypeHeader(udaService), 1);
            }
            final List<InetAddress> localIPs = HTTPProxyUtils.getLocalIPs();
            final InetAddress netMask = InetAddress.getByAddress(new byte[] { (byte) 255, (byte) 255, (byte) 255, 0 });
            final long start = Time.systemIndependentCurrentJVMTimeMillis();
            int lastUpdate = -1;
            while (getLocalPort() > 0) {
                if (Time.systemIndependentCurrentJVMTimeMillis() - start > 5 * 60 * 1000l) {
                    break;
                }
                if (lastUpdate != updateFlag.get()) {
                    lastUpdate = updateFlag.get();
                    for (final UDAServiceType udaService : udaServices) {
                        for (final Device device : upnpService.getRegistry().getDevices(udaService)) {
                            final Service service = device.findService(udaService);
                            if (service == null || service.getAction("AddPortMapping") == null) {
                                continue;
                            }
                            String deviceIP = null;
                            final DeviceDetails details = device.getDetails();
                            if (details.getBaseURL() != null) {
                                deviceIP = details.getBaseURL().getHost();
                            } else {
                                final DeviceIdentity identity = device.getIdentity();
                                if (identity instanceof RemoteDeviceIdentity) {
                                    RemoteDeviceIdentity remoteIdentity = (RemoteDeviceIdentity) identity;
                                    deviceIP = remoteIdentity.getDescriptorURL().getHost();
                                }
                            }
                            if (deviceIP == null) {
                                continue;
                            }
                            final Iterator<InetAddress> it = localIPs.iterator();
                            InetAddress localIP = null;
                            while (it.hasNext()) {
                                final InetAddress checkIP = it.next();
                                if (checkIP instanceof Inet4Address && sameNetwork(deviceIP, checkIP, netMask)) {
                                    // TODO: Check/Add IPv6 Support. We speak IPv4-Only with Router
                                    localIP = checkIP;
                                    break;
                                }
                            }
                            if (localIP == null) {
                                continue;
                            }
                            logger.info("Found Router at '" + deviceIP + "' for " + localIP.getHostAddress());
                            int upnpPort = CFG_MYJD.CFG.getLastUpnpPort();
                            final AtomicInteger portMappingTry = new AtomicInteger(0);
                            while (portMappingTry.incrementAndGet() < 6) {
                                if (portMappingTry.get() > 1 || upnpPort <= 0) {
                                    upnpPort = randomPort();
                                }
                                final PortMapping upnpPortMapping = new PortMapping(upnpPort, localIP.getHostAddress(), PortMapping.Protocol.TCP, "MyJDownloader");
                                upnpPortMapping.setInternalPort(new UnsignedIntegerTwoBytes(getLocalPort()));
                                final PortMappingAdd action = new PortMappingAdd(service, upnpPortMapping) {
                                    @Override
                                    public void success(ActionInvocation invocation) {
                                        MyJDownloaderDirectServer.this.upnpPortMapping = upnpPortMapping;
                                        MyJDownloaderDirectServer.this.upnpPortMappingService = service;
                                        logger.info("PortMappingAdd(" + upnpPortMapping + " successful");
                                        CFG_MYJD.CFG.setLastUpnpPort(upnpPortMapping.getExternalPort().getValue().intValue());
                                    }

                                    @Override
                                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                                        logger.info("PortMappingAdd(" + upnpPortMapping + " failed");
                                    }
                                };
                                final Future<?> result = upnpService.getControlPoint().execute(action);
                                result.get(5, TimeUnit.SECONDS);
                                if (MyJDownloaderDirectServer.this.upnpPortMapping != null) {
                                    return;
                                }
                            }
                        }
                    }
                }
                Thread.sleep(1000);
            }
        } catch (final Throwable e) {
            logger.log(e);
        } finally {
            if (upnpService != null) {
                upnpService.shutdown();
            }
        }
    }

    @Override
    public void run() {
        try {
            ShutdownController.getInstance().addShutdownVetoListener(this);
            final int lastLocalPort = CFG_MYJD.CFG.getLastLocalPort();
            switch (connectMode) {
            case LAN:
                createServerSocket(-1);
                logger.info("MyJDownloaderDirectConnectionServer: Mode=" + connectMode + " LocalPort=" + getLocalPort() + "(" + lastLocalPort + ")");
                break;
            case LAN_WAN_MANUAL:
                createServerSocket(CFG_MYJD.CFG.getManualLocalPort());
                logger.info("MyJDownloaderDirectConnectionServer: Mode=" + connectMode + " RemotePort=" + CFG_MYJD.CFG.getManualRemotePort() + " LocalPort=" + getLocalPort() + "(" + lastLocalPort + "|" + CFG_MYJD.CFG.getManualLocalPort() + ")");
                break;
            case LAN_WAN_UPNP:
                createServerSocket(-1);
                logger.info("MyJDownloaderDirectConnectionServer: Mode=" + connectMode + " LocalPort=" + getLocalPort() + "(" + lastLocalPort + ")");
                new Thread("MyJDownloaderDirectConnectionServer: setup UPNP") {
                    {
                        setDaemon(true);
                    }

                    public void run() {
                        setupUPNPPort();
                    };
                }.start();
                break;
            default:
                return;
            }
            ServerSocket serverSocket = null;
            while (connectThread.isAlive() && (serverSocket = currentServerSocket.get()) != null) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    if (connectMode == DIRECTMODE.LAN) {
                        final InetSocketAddress addr = (InetSocketAddress) clientSocket.getRemoteSocketAddress();
                        if (!addr.getAddress().isLoopbackAddress() && !addr.getAddress().isSiteLocalAddress()) {
                            clientSocket.close();
                            continue;
                        }
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
            close();
        }
    }

    protected void close() {
        ShutdownController.getInstance().removeShutdownVetoListener(this);
        final Thread thread = new Thread(getClass() + ":close()") {
            @Override
            public void run() {
                try {
                    final ServerSocket serverSocket = currentServerSocket.getAndSet(null);
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (final Throwable e) {
                }
                final PortMapping upnpPortMapping = MyJDownloaderDirectServer.this.upnpPortMapping;
                final Service upnpPortMappingService = MyJDownloaderDirectServer.this.upnpPortMappingService;
                if (upnpPortMapping != null && upnpPortMappingService != null) {
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
                    UpnpServiceImpl upnpService = null;
                    try {
                        upnpService = new UpnpServiceImpl(config);
                        upnpService.getControlPoint().execute(new PortMappingDelete(upnpPortMappingService, upnpPortMapping) {
                            @Override
                            public void success(ActionInvocation var1) {
                                logger.info("PortMappingDelete(" + upnpPortMapping + " successful");
                                MyJDownloaderDirectServer.this.upnpPortMapping = null;
                                MyJDownloaderDirectServer.this.upnpPortMappingService = null;
                            }

                            @Override
                            public void failure(ActionInvocation var1, UpnpResponse var2, String var3) {
                                logger.info("PortMappingDelete(" + upnpPortMapping + " failed");
                            }
                        }).get(5, TimeUnit.SECONDS);
                    } catch (final Exception e) {
                    } finally {
                        if (upnpService != null) {
                            upnpService.shutdown();
                        }
                    }
                }
            }
        };
        thread.start();
        try {
            thread.join(10000);
        } catch (InterruptedException ignore) {
        }
    }

    @Override
    public void onShutdown(ShutdownRequest request) {
        close();
    }

    public int getLocalPort() {
        final ServerSocket serverSocket = currentServerSocket.get();
        return serverSocket == null ? -1 : serverSocket.getLocalPort();
    }

    public int getRemotePort() {
        final ServerSocket serverSocket = currentServerSocket.get();
        if (serverSocket == null) {
            return -1;
        } else {
            switch (connectMode) {
            case LAN_WAN_MANUAL:
                return CFG_MYJD.CFG.getManualRemotePort();
            case LAN_WAN_UPNP:
                final PortMapping upnpPortMapping = this.upnpPortMapping;
                return upnpPortMapping == null ? -1 : upnpPortMapping.getExternalPort().getValue().intValue();
            default:
                return -1;
            }
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

    @Override
    public void onShutdownVeto(ShutdownRequest request) {
    }

    @Override
    public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
    }

    @Override
    public long getShutdownVetoPriority() {
        return 0;
    }
}
