package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.reconnect.ipcheck.IP;

import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxyUtils;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.fourthline.cling.support.igd.callback.PortMappingAdd;
import org.fourthline.cling.support.model.PortMapping;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.DIRECTMODE;
import org.jdownloader.api.myjdownloader.api.MyJDownloaderAPI;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderDirectServer extends Thread {
    private ServerSocket                     currentServerSocket = null;
    private final MyJDownloaderConnectThread connectThread;
    private final DIRECTMODE                 connectMode;
    private final LogSource                  logger;
    private int                              upnpPort            = -1;
    
    public MyJDownloaderDirectServer(MyJDownloaderConnectThread connectThread) {
        this.connectThread = connectThread;
        DIRECTMODE connectMode = CFG_MYJD.CFG.getDirectConnectMode();
        if (connectMode == null) connectMode = DIRECTMODE.LAN;
        this.connectMode = connectMode;
        logger = connectThread.getLogger();
    }
    
    private ServerSocket createServerSocket(int wished) throws IOException {
        int lastPort = CFG_MYJD.CFG.getLastLocalPort();
        if (lastPort <= 0 || lastPort > 65000) lastPort = 0;
        if (wished > 0 && wished < 65000) lastPort = wished;
        try {
            ServerSocket currentServerSocket = new ServerSocket(lastPort);
            CFG_MYJD.CFG.setLastLocalPort(currentServerSocket.getLocalPort());
            return currentServerSocket;
        } catch (final Throwable e) {
            logger.log(e);
        }
        ServerSocket currentServerSocket = new ServerSocket(0);
        CFG_MYJD.CFG.setLastLocalPort(currentServerSocket.getLocalPort());
        return currentServerSocket;
    }
    
    public static boolean sameNetwork(String ip1, InetAddress ip2, InetAddress netMask) {
        try {
            byte[] ip1Bytes = InetAddress.getByName(ip1).getAddress();
            byte[] ip2Bytes = ip2.getAddress();
            byte[] maskBytes = netMask.getAddress();
            for (int i = 0; i < ip1Bytes.length; i++) {
                if ((ip1Bytes[i] & maskBytes[i]) != (ip2Bytes[i] & maskBytes[i])) return false;
            }
            return true;
        } catch (final Throwable e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private static int randomPort() {
        int min = 1025;
        int max = 65000;
        return new Random().nextInt((max - min) + 1) + min;
    }
    
    private int setUPNPPort() throws InterruptedException {
        UDAServiceType[] udaServices = new UDAServiceType[] { new UDAServiceType("WANPPPConnection"), new UDAServiceType("WANIPConnection") };
        UpnpServiceImpl upnpService = null;
        try {
            upnpService = new UpnpServiceImpl();
            upnpService.getControlPoint().search(10000);
            Thread.sleep(10000);
            final CopyOnWriteArrayList<InetAddress> localIPs = new CopyOnWriteArrayList<InetAddress>(HTTPProxyUtils.getLocalIPs());
            InetAddress netMask = InetAddress.getByName("255.255.255.0");
            for (UDAServiceType udaService : udaServices) {
                for (Device device : upnpService.getRegistry().getDevices(udaService)) {
                    Service service = device.findService(udaService);
                    if (service == null) continue;
                    String deviceIP = device.getDetails().getBaseURL().getHost();
                    Iterator<InetAddress> it = localIPs.iterator();
                    InetAddress localIP = null;
                    while (it.hasNext()) {
                        InetAddress next = it.next();
                        if (sameNetwork(deviceIP, next, netMask)) {
                            localIP = next;
                            break;
                        }
                    }
                    if (localIP == null) continue;
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
                if (upnpService != null) upnpService.shutdown();
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
                    } else
                        throw e;
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
        if (currentServerSocket == null) return -1;
        return currentServerSocket.getLocalPort();
    }
    
    public int getRemotePort() {
        if (currentServerSocket == null) return -1;
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
        if (api == null) throw new IOException("api no longer available");
        final long requestNumber = connectThread.THREADCOUNTER.incrementAndGet();
        Thread connectionThread = new Thread("MyJDownloaderDirectConnection:" + requestNumber) {
            @Override
            public void run() {
                try {
                    System.out.println("Handle a direct MyJDownloader connection:" + requestNumber);
                    MyJDownloaderDirectHttpConnection httpConnection = new MyJDownloaderDirectHttpConnection(clientSocket, api);
                    httpConnection.run();
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
            MyJDownloaderConnectThread.getOpenconnections().put(connectionThread, clientSocket);
        }
        connectThread.setEstablishedConnections(connectThread.getEstablishedConnections());
        connectionThread.setDaemon(true);
        connectionThread.start();
    }
}
