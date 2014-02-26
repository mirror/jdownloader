package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.reconnect.ipcheck.IP;

import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.DIRECTMODE;
import org.jdownloader.api.myjdownloader.api.MyJDownloaderAPI;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderDirectServer extends Thread {
    private final AtomicLong                 THREADCOUNTER       = new AtomicLong(0);
    private ServerSocket                     currentServerSocket = null;
    private final MyJDownloaderConnectThread connectThread;
    private final DIRECTMODE                 connectMode;
    
    public MyJDownloaderDirectServer(MyJDownloaderConnectThread connectThread) {
        this.connectThread = connectThread;
        DIRECTMODE connectMode = CFG_MYJD.CFG.getDirectConnectMode();
        if (connectMode == null) connectMode = DIRECTMODE.LAN;
        this.connectMode = connectMode;
    }
    
    @Override
    public void run() {
        try {
            switch (connectMode) {
                case LAN:
                    currentServerSocket = new ServerSocket(0);
                    break;
                case LAN_WAN_MANUAL:
                    try {
                        currentServerSocket = new ServerSocket(CFG_MYJD.CFG.getManualLocalPort());
                        break;
                    } catch (final Throwable e) {
                        connectThread.getLogger().log(e);
                    }
                default:
                    currentServerSocket = new ServerSocket(0);
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
                } catch (IOException e) {
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
            connectThread.getLogger().log(e);
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
    
    public int getPort() {
        if (currentServerSocket == null) return -1;
        return currentServerSocket.getLocalPort();
    }
    
    protected void handleConnection(final Socket clientSocket) throws IOException {
        final MyJDownloaderAPI api = connectThread.getApi();
        if (api == null) throw new IOException("api no longer available");
        Thread connectionThread = new Thread("MyJDownloaderDirectConnection:" + THREADCOUNTER.incrementAndGet()) {
            @Override
            public void run() {
                try {
                    MyJDownloaderDirectHttpConnection httpConnection = new MyJDownloaderDirectHttpConnection(clientSocket, api);
                    httpConnection.run();
                } catch (final Throwable e) {
                    connectThread.getLogger().log(e);
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
