package jd.router.upnp;

/******************************************************************
*
*	CyberUPnP for Java
*
*	Copyright (C) Satoshi Konno 2002
*
*	File : IconTable.java
*
******************************************************************/

import org.cybergarage.upnp.Icon;

public class IconTable extends TableModel
{
	public IconTable(Icon icon)
	{
		super(icon.getIconNode());
	}
}

