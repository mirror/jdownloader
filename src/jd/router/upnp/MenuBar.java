package jd.router.upnp;

/******************************************************************
*
*	CyberUPnP for Java
*
*	Copyright (C) Satoshi Konno 2002
*
*	File : ActionDialog.java
*
******************************************************************/

import java.awt.event.*;
import javax.swing.*;

public class MenuBar extends JMenuBar implements ActionListener
{
	CtrlPoint ctrlPoint;

	JMenu fileMenu;
	JMenu searchMenu;
	JMenu logMenu;

	JMenuItem quitItem;
	JMenuItem searchRootDeviceItem;
	JMenuItem clearItem;
	
	public MenuBar(CtrlPoint ctrlPoint)
	{
		this.ctrlPoint = ctrlPoint;
		
		fileMenu = new JMenu("File");
		add(fileMenu);
		quitItem = new JMenuItem("Quit");
		quitItem.addActionListener(this);
		fileMenu.add(quitItem);

		searchMenu = new JMenu("Search");
		add(searchMenu);
		searchRootDeviceItem = new JMenuItem("upnp:rootdevice");
		searchRootDeviceItem.addActionListener(this);
		searchMenu.add(searchRootDeviceItem);

		logMenu = new JMenu("Log");
		add(logMenu);
		clearItem = new JMenuItem("Clear");
		clearItem.addActionListener(this);
		logMenu.add(clearItem);
	}

	////////////////////////////////////////////////
	//	actionPerformed
	////////////////////////////////////////////////

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == quitItem) {
			System.exit(0);
		}
		if (e.getSource() == searchRootDeviceItem) {
			ctrlPoint.search("upnp:rootdevice");
		}
		if (e.getSource() == clearItem) {
			ctrlPoint.clearConsole();
		}
	}
}
