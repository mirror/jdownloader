package jd.router.upnp;

/******************************************************************
*
*	CyberUPnP for Java
*
*	Copyright (C) Satoshi Konno 2002
*
*	File : ActionPane.java
*
******************************************************************/

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.UPnPStatus;

public class ActionPane extends JPanel implements ActionListener
{
	private CtrlPoint ctrlPoint;
	private Action action;
	private ActionTable actionTable;
	private JButton actionButton;

	////////////////////////////////////////////////
	//	Constructor
	////////////////////////////////////////////////
	
	public ActionPane(CtrlPoint ctrlPoint, Action action)
	{
		setLayout(new BorderLayout());

		this.ctrlPoint = ctrlPoint;
		this.action = action;
				
		JPanel tablePane = new JPanel();
		tablePane.setLayout(new BorderLayout());
		actionTable = new ActionTable(action);
		tablePane.add(new TableComp(actionTable), BorderLayout.CENTER);
		add(tablePane, BorderLayout.CENTER);
		
		JPanel buttonPane = new JPanel();
		actionButton = new JButton("Action");
		buttonPane.add(actionButton);
		add(buttonPane, BorderLayout.SOUTH);
		actionButton.addActionListener(this);
	}

	////////////////////////////////////////////////
	//	Member
	////////////////////////////////////////////////

	public Action getAction() {
		return action;
	}

	public ActionTable getTable() {
		return actionTable;
	}

	public JButton getButton() {
		return actionButton;
	}

	////////////////////////////////////////////////
	//	actionPerformed
	////////////////////////////////////////////////

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() != actionButton)
			return;
			
		ArgumentList inArgList = action.getInputArgumentList();
		if (0 < inArgList.size()) {
			ActionDialog dlg = new ActionDialog(ctrlPoint.getFrame(), action);
			dlg.doModal();
		}
		String title = action.getName();
		String msg = "";
		boolean ctrlRes = action.postControlAction();
		if (ctrlRes == true) {
			ArgumentList outArgList = action.getOutputArgumentList();
			int nArgs = outArgList.size();
			if (nArgs == 0)
				msg = "(No response value)";
			for (int n=0; n<nArgs; n++) {
				Argument arg = outArgList.getArgument(n);
				String name = arg.getName();
				String value = arg.getValue();
				msg += name + " = " + value;
				if (n < (nArgs-1))
					msg += ", ";
			}
		}
		else {
			UPnPStatus err = action.getControlStatus();
			msg = err.getDescription() + " (" + Integer.toString(err.getCode()) + ")";
		}

		ctrlPoint.printConsole(title + " : " + msg);
		JOptionPane.showMessageDialog(this, msg, title,  JOptionPane.PLAIN_MESSAGE );
	}

}

