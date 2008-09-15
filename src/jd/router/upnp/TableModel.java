package jd.router.upnp;

/******************************************************************
*
*	CyberUPnP for Java
*
*	Copyright (C) Satoshi Konno 2002
*
*	File : TableModel.java
*
******************************************************************/

import javax.swing.table.*;

import org.cybergarage.xml.*;

public class TableModel extends DefaultTableModel
{
	public TableModel()
	{
	}

	public TableModel(Node node)
	{
		addColumn("element");
		addColumn("value");
		set(node);
	}

	////////////////////////////////////////////////
	//	set
	////////////////////////////////////////////////

	void set(Node node)
	{
		int nElem = node.getNNodes();
		for (int n=0; n<nElem; n++) {
			Node elem = node.getNode(n);
			if (elem.hasNodes() == true)
				continue;
			addRow(elem.getName(), elem.getValue());
		}
	}

	////////////////////////////////////////////////
	//	addRows
	////////////////////////////////////////////////

	private String rowStrs[] = new String[2];

	public void addRow(String str1, String str2) 
	{
		rowStrs[0] = str1;
		rowStrs[1] = str2;
		addRow(rowStrs);
	}

	////////////////////////////////////////////////
	//	isCellEditable
	////////////////////////////////////////////////
	
	public boolean isCellEditable(int row, int col)
	{
		return false;
	}

}

