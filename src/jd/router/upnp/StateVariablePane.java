package jd.router.upnp;

/******************************************************************
 *
 *	CyberUPnP for Java
 *
 *	Copyright (C) Satoshi Konno 2002
 *
 *	File : StateVariablePane.java
 *
 ******************************************************************/

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.UPnPStatus;

public class StateVariablePane extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;
    private CtrlPoint ctrlPoint;
    private StateVariable stateVar;
    private StateVariableTable stateVarTable;
    private JButton queryButton;

    // //////////////////////////////////////////////
    // Constructor
    // //////////////////////////////////////////////

    public StateVariablePane(CtrlPoint ctrlPoint, StateVariable var) {
        setLayout(new BorderLayout());

        this.ctrlPoint = ctrlPoint;
        this.stateVar = var;

        JPanel tablePane = new JPanel();
        tablePane.setLayout(new BorderLayout());
        stateVarTable = new StateVariableTable(var);
        tablePane.add(new TableComp(stateVarTable), BorderLayout.CENTER);
        add(tablePane, BorderLayout.CENTER);

        JPanel buttonPane = new JPanel();
        queryButton = new JButton("Query");
        buttonPane.add(queryButton);
        queryButton.addActionListener(this);

        add(buttonPane, BorderLayout.SOUTH);
    }

    // //////////////////////////////////////////////
    // Member
    // //////////////////////////////////////////////

    public StateVariable getStateVariable() {
        return stateVar;
    }

    public StateVariableTable getTable() {
        return stateVarTable;
    }

    public JButton getButton() {
        return queryButton;
    }

    // //////////////////////////////////////////////
    // varPerformed
    // //////////////////////////////////////////////

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() != queryButton) return;

        String title = stateVar.getName();
        String msg;
        boolean queryRes = stateVar.postQuerylAction();
        if (queryRes == true)
            msg = stateVar.getName() + " = " + stateVar.getValue();
        else {
            UPnPStatus err = stateVar.getQueryStatus();
            msg = err.getDescription() + " (" + Integer.toString(err.getCode()) + ")";
        }
        ctrlPoint.printConsole(title + " : " + msg);
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.PLAIN_MESSAGE);
    }

}
