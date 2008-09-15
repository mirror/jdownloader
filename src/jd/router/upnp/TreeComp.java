package jd.router.upnp;

/******************************************************************
*
*	CyberUPnP for Java
*
*	Copyright (C) Satoshi Konno 2002-2003
*
*	File : TreeComp.java
*
******************************************************************/

import javax.swing.*;
import javax.swing.tree.*;

import org.cybergarage.upnp.*;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Icon;

public class TreeComp extends JTree
{
	public TreeComp(TreeNode root)
	{
		super(root);
		setRootNode(root);
	}

	////////////////////////////////////////////////
	//	Root Node
	////////////////////////////////////////////////

	private TreeNode rootNode;
	
	public void setRootNode(TreeNode node)
	{
		rootNode = node;
	}

	public TreeNode getRootNode()
	{
		return rootNode;
	}
	 
	////////////////////////////////////////////////
	//	Update Tree
	////////////////////////////////////////////////

	void update(ControlPoint ctrlp)
	{
		TreeNode rootNode = getRootNode();
		if (rootNode == null)
			return;
			
		rootNode.removeAllChildren();
		
		DeviceList rootDevList = ctrlp.getDeviceList();
		updateDeviceList(rootNode, rootDevList);
		
		((DefaultTreeModel)getModel()).reload();
		repaint();
	}

	void updateDeviceList(TreeNode parentNode, DeviceList devList)
	{
		int nDevs = devList.size();
		for (int n=0; n<nDevs; n++) {
			Device dev = devList.getDevice(n);
			String friendlyName = dev.getFriendlyName();
			TreeNode devNode = new TreeNode(friendlyName);
			devNode.setUserData(dev);
			parentNode.add(devNode);
			updateServiceList(devNode, dev);
			updateIconList(devNode, dev);
			updateDeviceList(devNode, dev.getDeviceList());
		}
	}

	void updateIconList(TreeNode parentNode, Device device)
	{
		IconList iconList = device.getIconList();
		int nIcons = iconList.size();
		for (int n=0; n<nIcons; n++) {
			Icon icon = iconList.getIcon(n);
			String url = icon.getURL();
			TreeNode iconNode = new TreeNode(url);
			iconNode.setUserData(icon);
			parentNode.add(iconNode);
		}
	}

	void updateServiceList(TreeNode parentNode, Device device)
	{
		ServiceList serviceList = device.getServiceList();
		int nServices = serviceList.size();
		for (int n=0; n<nServices; n++) {
			Service service = serviceList.getService(n);
			String serviceType = service.getServiceType();
			TreeNode serviceNode = new TreeNode(serviceType);
			serviceNode.setUserData(service);
			parentNode.add(serviceNode);
			updateActionList(serviceNode, service);
			updateStateVariableList(serviceNode, service);
		}
	}

	void updateActionList(TreeNode parentNode, Service service)
	{
		ActionList actionList = service.getActionList();
		int nActions = actionList.size();
		for (int n=0; n<nActions; n++) {
			Action action = actionList.getAction(n);
			String actionName = action.getName();
			TreeNode actionNode = new TreeNode(actionName);
			actionNode.setUserData(action);
			parentNode.add(actionNode);
			updateArgumentList(actionNode, action);
		}
	}

	void updateArgumentList(TreeNode parentNode, Action action)
	{
		ArgumentList argList = action.getArgumentList();
		int nArguments = argList.size();
		for (int n=0; n<nArguments; n++) {
			Argument arg = argList.getArgument(n);
			String argName = arg.getName() + "(" + arg.getDirection() + ")";
			TreeNode argNode = new TreeNode(argName);
			argNode.setUserData(arg);
			parentNode.add(argNode);
		}
	}

	void updateStateVariableList(TreeNode parentNode, Service service)
	{
		ServiceStateTable stateList = service.getServiceStateTable();
		int nStateVariables = stateList.size();
		for (int n=0; n<nStateVariables; n++) {
			StateVariable state = stateList.getStateVariable(n);
			String stateName = state.getName();
			TreeNode stateNode = new TreeNode(stateName);
			stateNode.setUserData(state);
			parentNode.add(stateNode);
		}
	}
}

