package jd.router.upnp;

/******************************************************************
*
*	CyberUPnP for Java
*
*	Copyright (C) Satoshi Konno 2002
*
*	File : ServicePane.java
*
******************************************************************/

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.cybergarage.upnp.Service;

public class ServicePane extends JPanel  implements ActionListener
{
	private CtrlPoint ctrlPoint;
	private Service service;
	private ServiceTable serviceTable;
	private JButton subscribeButton;
	private JButton unsubscribeButton;

	////////////////////////////////////////////////
	//	Constructor
	////////////////////////////////////////////////
	
	public ServicePane(CtrlPoint ctrlPoint, Service service)
	{
		setLayout(new BorderLayout());

		this.ctrlPoint = ctrlPoint;
		this.service = service;
				
		JPanel tablePane = new JPanel();
		tablePane.setLayout(new BorderLayout());
		serviceTable = new ServiceTable(service);
		tablePane.add(new TableComp(serviceTable), BorderLayout.CENTER);
		add(tablePane, BorderLayout.CENTER);
		
		JPanel buttonPane = new JPanel();
		subscribeButton = new JButton("Subscribe");
		buttonPane.add(subscribeButton);
		subscribeButton.addActionListener(this);
		unsubscribeButton = new JButton("Unsubscribe");
		buttonPane.add(unsubscribeButton);
		unsubscribeButton.addActionListener(this);
		add(buttonPane, BorderLayout.SOUTH);
	}

	////////////////////////////////////////////////
	//	Frame
	////////////////////////////////////////////////

	private Frame getFrame()
	{
		return (Frame)getRootPane().getParent();
	}

	////////////////////////////////////////////////
	//	Member
	////////////////////////////////////////////////

	public Service getService() {
		return service;
	}

	public ServiceTable getTable() {
		return serviceTable;
	}

	public JButton getButton() {
		return subscribeButton;
	}

	////////////////////////////////////////////////
	//	servicePerformed
	////////////////////////////////////////////////

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == subscribeButton) {
			boolean subRes;
			if (service.hasSID() == true) {
				String sid = service.getSID();
				subRes = ctrlPoint.subscribe(service, sid);
			}
			else
				subRes = ctrlPoint.subscribe(service);
			ctrlPoint.printConsole("subscribe : " + subRes +" (" + service.getSID() + ")");
		}
		else if (e.getSource() == unsubscribeButton) {
			boolean subRes = ctrlPoint.unsubscribe(service);
			ctrlPoint.printConsole("unsubscribe : " + subRes);
		}
	}

}

