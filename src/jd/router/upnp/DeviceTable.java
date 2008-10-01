package jd.router.upnp;

/******************************************************************
 *
 *	CyberUPnP for Java
 *
 *	Copyright (C) Satoshi Konno 2002
 *
 *	File : DeviceTable.java
 *
 ******************************************************************/

import org.cybergarage.upnp.Device;

public class DeviceTable extends TableModel {
    private static final long serialVersionUID = 1L;

    public DeviceTable(Device dev) {
        addColumn("element");
        addColumn("value");
        set(dev);
    }

    // //////////////////////////////////////////////
    // set
    // //////////////////////////////////////////////

    void set(Device dev) {
        if (dev.isRootDevice() == true) {
            addRow("Location", dev.getLocation());
            addRow("URLBase", dev.getURLBase());
            addRow("LeaseTime", Long.toString(dev.getLeaseTime()));
        }

        set(dev.getDeviceNode());
    }
}
