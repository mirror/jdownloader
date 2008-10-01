package jd.router.upnp;

/******************************************************************
 *
 *	CyberUPnP for Java
 *
 *	Copyright (C) Satoshi Konno 2002
 *
 *	File : ServiceTable.java
 *
 ******************************************************************/

import org.cybergarage.upnp.Service;

public class ServiceTable extends TableModel {
    private static final long serialVersionUID = 1L;

    public ServiceTable(Service dev) {
        super(dev.getServiceNode());
    }
}
