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

import org.cybergarage.upnp.*;

public class ServiceTable extends TableModel
{
	public ServiceTable(Service dev)
	{
		super(dev.getServiceNode());
	}
}

