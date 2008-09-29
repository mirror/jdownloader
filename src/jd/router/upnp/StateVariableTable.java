package jd.router.upnp;

/******************************************************************
*
*	CyberUPnP for Java
*
*	Copyright (C) Satoshi Konno 2002
*
*	File : StateVariableTable.java
*
******************************************************************/

import org.cybergarage.upnp.StateVariable;

public class StateVariableTable extends TableModel
{
	public StateVariableTable(StateVariable var)
	{
		super(var.getStateVariableNode());
	}
}

