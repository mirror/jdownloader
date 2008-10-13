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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;

public class ActionDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;
    private Action action;
    private JButton okButton;
    private JButton cancelButton;
    private boolean result;
    private ArgumentList inArgList;
    private Vector<JTextField> inArgFieldList;

    @SuppressWarnings("unchecked")
    public ActionDialog(Frame frame, Action action) {
        super(frame, true);
        getContentPane().setLayout(new BorderLayout());

        this.action = action;

        inArgList = new ArgumentList();
        inArgFieldList = new Vector<JTextField>();

        JPanel argListPane = new JPanel();
        // argListPane.setLayout(new BoxLayout(argListPane, BoxLayout.Y_AXIS));
        argListPane.setLayout(new GridLayout(0, 2));
        getContentPane().add(argListPane, BorderLayout.CENTER);

        ArgumentList argList = action.getArgumentList();
        int nArgs = argList.size();
        for (int n = 0; n < nArgs; n++) {
            Argument arg = argList.getArgument(n);
            if (arg.isInDirection() == false) continue;

            JLabel argLabel = new JLabel(arg.getName());
            JTextField argField = new JTextField();

            inArgFieldList.add(argField);
            argListPane.add(argLabel);
            argListPane.add(argField);

            Argument inArg = new Argument();
            inArg.setName(arg.getName());
            inArgList.add(inArg);
        }

        okButton = new JButton("OK");
        okButton.addActionListener(this);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        JPanel buttonPane = new JPanel();
        buttonPane.add(okButton);
        buttonPane.add(cancelButton);
        getContentPane().add(buttonPane, BorderLayout.SOUTH);

        pack();

        Dimension size = getSize();
        Point fpos = frame.getLocationOnScreen();
        Dimension fsize = frame.getSize();
        setLocation(fpos.x + (fsize.width - size.width) / 2, fpos.y + (fsize.height - size.height) / 2);
    }

    // //////////////////////////////////////////////
    // actionPerformed
    // //////////////////////////////////////////////

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            result = true;
            int fieldCnt = inArgFieldList.size();
            for (int n = 0; n < fieldCnt; n++) {
                JTextField field = (JTextField) inArgFieldList.get(n);
                String value = field.getText();
                Argument arg = inArgList.getArgument(n);
                arg.setValue(value);
            }
            action.setArgumentValues(inArgList);
            dispose();
        }
        if (e.getSource() == cancelButton) {
            result = false;
            dispose();
        }
    }

    // //////////////////////////////////////////////
    // actionPerformed
    // //////////////////////////////////////////////

    public boolean doModal() {
        setVisible(true);
        return result;
    }
}
