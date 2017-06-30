package org.jdownloader.api.device;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IP;

import org.appwork.remoteapi.RemoteAPIRequest;
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
    private final InetAddress[] lookup(final String hostName) throws IOException {
        final Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "1000");
        env.put("com.sun.jndi.dns.timeout.retries", "3");
        InitialDirContext ictx = null;
        try {
            ictx = new InitialDirContext(env);
            final Attributes attrs = ictx.getAttributes(hostName, new String[] { "A", "AAAA" });
            final List<InetAddress> ret = new ArrayList<InetAddress>();
            for (final String ipV : new String[] { "A", "AAAA" }) {
                final Attribute attr = attrs.get(ipV);
                if (attr != null) {
                    final NamingEnumeration<?> it = attr.getAll();
                    while (it.hasMoreElements()) {
                        final Object next = it.next();
                        ret.add(InetAddress.getByName(next.toString()));
                    }
                }
            }
            return ret.toArray(new InetAddress[0]);
        } catch (NamingException e) {
            return HTTPConnectionUtils.resolvHostIP(hostName);
        } finally {
            try {
                if (ictx != null) {
                    ictx.close();
                }
            } catch (final Throwable ignore) {
            }
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
                            final InetAddress[] resolv = lookup(localIP.getHostAddress().replace(".", "-") + ".mydns.jdownloader.org");
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
                final DirectConnectionInfo info = new DirectConnectionInfo();
                info.setPort(directServer.getLocalPort());
                info.setIp(localIP.getHostAddress());
                infos.add(info);
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
