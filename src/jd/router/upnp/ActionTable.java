package jd.router.upnp;

/******************************************************************
*
*	CyberUPnP for Java
*
*	Copyright (C) Satoshi Konno 2002
*
*	File : ActionTable.java
*
******************************************************************/

import org.cybergarage.upnp.Action;

public class ActionTable extends TableModel
{
	public ActionTable(Action act)
	{
		super(act.getActionNode());
	}
}

