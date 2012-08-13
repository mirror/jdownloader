package org.jdownloader.extensions.streaming.upnp;

import java.net.NetworkInterface;

import org.appwork.utils.os.CrossSystem;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.impl.MulticastReceiverConfigurationImpl;
import org.fourthline.cling.transport.impl.MulticastReceiverImpl;
import org.fourthline.cling.transport.spi.DatagramProcessor;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;

public class MulticastReceiverImpl2 extends MulticastReceiverImpl {

    public MulticastReceiverImpl2(NetworkAddressFactory networkAddressFactory) {
        super(new MulticastReceiverConfigurationImpl(networkAddressFactory.getMulticastGroup(), networkAddressFactory.getMulticastPort()));
    }

    @Override
    public synchronized void init(NetworkInterface networkInterface, Router router, DatagramProcessor datagramProcessor) throws InitializationException {
        if (CrossSystem.isMac()) {
            super.init(networkInterface, router, datagramProcessor);
        } else {
            super.init(networkInterface, router, datagramProcessor);
        }
    }

}
