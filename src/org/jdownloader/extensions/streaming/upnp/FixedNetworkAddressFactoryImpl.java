package org.jdownloader.extensions.streaming.upnp;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.transport.impl.NetworkAddressFactoryImpl;
import org.jdownloader.logging.LogController;

public class FixedNetworkAddressFactoryImpl extends NetworkAddressFactoryImpl {

    private LogSource logger;

    public FixedNetworkAddressFactoryImpl(int streamListenPort) {
        super(streamListenPort);
        logger = LogController.getInstance().getLogger("streaming");

    }

    @Override
    protected List<InterfaceAddress> getInterfaceAddresses(NetworkInterface networkInterface) {
        List<InterfaceAddress> ret = super.getInterfaceAddresses(networkInterface);

        if (ret == null || ret.size() == 0 || ret.get(0) == null) {
            logger.info("Use getInterfaceAddresses workaround");
            ret = new ArrayList<InterfaceAddress>();
            // java bug create dummy addresses

            for (InetAddress adr : getInetAddresses(networkInterface)) {
                try {
                    Constructor<InterfaceAddress> con = InterfaceAddress.class.getDeclaredConstructor(new Class[] {});
                    con.setAccessible(true);
                    InterfaceAddress instance = con.newInstance(new Object[] {});

                    Field address = InterfaceAddress.class.getDeclaredField("address");
                    address.setAccessible(true);
                    Field broadcast = InterfaceAddress.class.getDeclaredField("broadcast");
                    broadcast.setAccessible(true);
                    Field maskLength = InterfaceAddress.class.getDeclaredField("maskLength");
                    maskLength.setAccessible(true);

                    address.set(instance, adr);
                    byte[] bytes = adr.getAddress();

                    broadcast.set(instance, InetAddress.getByAddress(new byte[] { bytes[0], bytes[1], bytes[2], (byte) 255 }));
                    maskLength.set(instance, (short) 24);
                    ret.add(instance);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
        logger.info("Result: " + networkInterface + " - " + ret);

        return ret;
    }

    @Override
    protected List<InetAddress> getInetAddresses(NetworkInterface networkInterface) {
        List<InetAddress> ret = super.getInetAddresses(networkInterface);
        return ret;
    }

}
