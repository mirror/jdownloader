package org.jdownloader.api.device;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IP;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.Base64OutputStream;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.appwork.utils.net.httpconnection.HTTPProxyUtils;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectThread;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.api.myjdownloader.MyJDownloaderDirectServer;
import org.jdownloader.api.myjdownloader.MyJDownloaderHttpConnection;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.DIRECTMODE;
import org.jdownloader.myjdownloader.client.json.DirectConnectionInfo;
import org.jdownloader.myjdownloader.client.json.DirectConnectionInfos;

public class DeviceAPIImpl implements DeviceAPI {
    private static final InetAddress[] lookup(final String hostName) throws IOException {
        final AtomicReference<Object> ret = new AtomicReference<Object>();
        final Thread lookup = new Thread("Lookup:" + hostName) {
            {
                setDaemon(true);
            }

            public void run() {
                try {
                    ret.set(HTTPConnectionUtils.resolvHostIP(hostName));
                } catch (IOException e) {
                    ret.set(e);
                }
            };
        };
        lookup.start();
        try {
            lookup.join(2000);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        final Object result = ret.get();
        if (result instanceof IOException) {
            throw (IOException) result;
        } else if (result instanceof InetAddress[]) {
            return (InetAddress[]) result;
        } else {
            return null;
        }
    }

    @Override
    public DirectConnectionInfos getDirectConnectionInfos(final RemoteAPIRequest request) {
        final DirectConnectionInfos ret = new DirectConnectionInfos();
        ret.setMode(DIRECTMODE.NONE.name());
        ret.setInfos(new ArrayList<DirectConnectionInfo>());
        final MyJDownloaderConnectThread thread = MyJDownloaderController.getInstance().getConnectThread();
        if (thread == null) {
            return ret;
        }
        final MyJDownloaderDirectServer directServer = thread.getDirectServer();
        if (directServer == null || !directServer.isAlive() || directServer.getLocalPort() < 0) {
            return ret;
        }
        ret.setMode(directServer.getConnectMode().name());
        final List<DirectConnectionInfo> infos = new ArrayList<DirectConnectionInfo>();
        final List<InetAddress> localIPs = HTTPProxyUtils.getLocalIPs(true);
        if (localIPs != null) {
            try {
                // check dns rebind protection
                final InetAddress[] localhost = lookup("127-0-0-1.mydns.jdownloader.org");
                if (localhost == null || localhost.length != 1 || !"127.0.0.1".equals(localhost[0].getHostAddress())) {
                    ret.setRebindProtectionDetected(true);
                } else {
                    for (final InetAddress localIP : localIPs) {
                        if (localIP.isSiteLocalAddress()) {
                            final InetAddress[] resolv = lookup(HexFormatter.byteArrayToHex(localIP.getAddress()) + ".mydns.jdownloader.org");
                            if (resolv == null || resolv.length != 1 || !resolv[0].equals(localIP)) {
                                ret.setRebindProtectionDetected(true);
                                break;
                            }
                        }
                    }
                }
            } catch (final Throwable e) {
                ret.setRebindProtectionDetected(true);
            }
            for (final InetAddress localIP : localIPs) {
                if (localIP.isLoopbackAddress() && localIP instanceof Inet6Address) {
                    // we don't need loopback via IPv4 and IPv6
                    continue;
                } else if (localIP.isLinkLocalAddress()) {
                    continue;
                } else {
                    final DirectConnectionInfo info = new DirectConnectionInfo();
                    info.setPort(directServer.getLocalPort());
                    if (localIP instanceof Inet6Address) {
                        info.setIp("[" + localIP.getHostAddress().replaceFirst("%.+", "") + "]");
                    } else {
                        info.setIp(localIP.getHostAddress());
                    }
                    infos.add(info);
                }
            }
        }
        if (directServer.getRemotePort() > 0) {
            try {
                final BalancedWebIPCheck ipCheck = new BalancedWebIPCheck() {
                    {
                        br.setConnectTimeout(5000);
                        br.setReadTimeout(5000);
                    }
                };
                final IP externalIP = ipCheck.getExternalIP();
                if (externalIP.getIP() != null) {
                    final DirectConnectionInfo info = new DirectConnectionInfo();
                    info.setPort(directServer.getRemotePort());
                    info.setIp(externalIP.getIP());
                    infos.add(info);
                }
            } catch (final Throwable e) {
                /* eg OfflineException */
            }
        }
        if (infos.size() > 0) {
            ret.setInfos(infos);
        }
        return ret;
    }

    @Override
    public boolean ping() {
        return true;
    }

    @Override
    public String getSessionPublicKey(final RemoteAPIRequest request) {
        final MyJDownloaderHttpConnection connection = MyJDownloaderHttpConnection.getMyJDownloaderHttpConnection(request);
        if (connection != null) {
            final KeyPair keyPair = connection.getRSAKeyPair();
            if (keyPair != null) {
                try {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    final Base64OutputStream b64 = new Base64OutputStream(bos);
                    b64.write(keyPair.getPublic().getEncoded());
                    b64.close();
                    return bos.toString("UTF-8");
                } catch (IOException e) {
                    connection.getLogger().log(e);
                }
            }
        }
        return null;
    }
}
