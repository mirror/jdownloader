package jd.router.upnp;

/******************************************************************
 *
 *	CyberUPnP for Java
 *
 *	Copyright (C) Satoshi Konno 2002
 *
 *	File : CtrlPointPane.java
 *
 ******************************************************************/

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.tree.TreePath;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Icon;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.StateVariable;

public class CtrlPointPane extends JPanel implements MouseListener {
    private static final long serialVersionUID = 1L;
    private CtrlPoint ctrlPoint;

    public CtrlPointPane(CtrlPoint ctrlPoint) {
        setLayout(new BorderLayout());

        this.ctrlPoint = ctrlPoint;

        TreeNode root = new TreeNode("root");

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false);
        add(mainSplitPane, BorderLayout.CENTER);

        devSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
        mainSplitPane.setTopComponent(devSplitPane);

        // Device Tree
        deviceTree = new TreeComp(root);
        deviceTree.addMouseListener(this);
        JScrollPane devScrPane = new JScrollPane(deviceTree);
        devScrPane.setPreferredSize(new Dimension(CtrlPoint.DEFAULT_WIDTH / 2, CtrlPoint.DEFAULT_HEIGHT / 4 * 3));
        devSplitPane.setLeftComponent(devScrPane);

        // Element
        JPanel dummyPane = new JPanel();
        devSplitPane.setRightComponent(dummyPane);

        // Console
        consoleArea = new JTextArea();
        consoleScrPane = new JScrollPane(consoleArea);
        mainSplitPane.setBottomComponent(consoleScrPane);
    }

    // //////////////////////////////////////////////
    // Frame
    // //////////////////////////////////////////////

    public Frame getFrame() {
        return (Frame) ctrlPoint.getFrame();
    }

    // //////////////////////////////////////////////
    // SplitPane
    // //////////////////////////////////////////////

    private JSplitPane devSplitPane;

    private JSplitPane getDeviceSplitPane() {
        return devSplitPane;
    }

    // //////////////////////////////////////////////
    // Console
    // //////////////////////////////////////////////

    private JTextArea consoleArea;
    private JScrollPane consoleScrPane;

    public JTextArea getConsoleArea() {
        return consoleArea;
    }

    public void printConsole(String str) {
        consoleArea.append(str + "\n");
        JScrollBar scrBar = consoleScrPane.getVerticalScrollBar();
        int maxPos = scrBar.getMaximum();
        scrBar.setValue(maxPos);
    }

    public void clearConsole() {
        consoleArea.setText("");
    }

    // //////////////////////////////////////////////
    // TreeComp
    // //////////////////////////////////////////////

    private TreeComp deviceTree;

    public TreeComp getTreeComp() {
        return deviceTree;
    }

    // //////////////////////////////////////////////
    // mouse
    // //////////////////////////////////////////////

    public void mouseClicked(MouseEvent e) {
        if (e.getComponent() == getTreeComp()) deviceTreeClicked(e);
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    // //////////////////////////////////////////////
    // mouse
    // //////////////////////////////////////////////

    private void setRightComponent(Component comp) {
        JSplitPane spane = getDeviceSplitPane();
        Component newComp = comp;// new JScrollPane(comp);
        Component currComp = spane.getRightComponent();
        newComp.setSize(currComp.getSize());
        spane.setRightComponent(newComp);
    }

    public void deviceTreeClicked(MouseEvent e) {
        TreePath path = getTreeComp().getPathForLocation(e.getX(), e.getY());
        if (path == null) return;

        Object lastComp = path.getLastPathComponent();

        // System.out.println("lastComp = " + lastComp);

        if (lastComp instanceof TreeNode) {
            TreeNode devTreeNode = (TreeNode) lastComp;
            Object data = devTreeNode.getUserData();
            if (data instanceof Device) {
                Device dev = (Device) data;
                DeviceTable devTable = new DeviceTable(dev);
                setRightComponent(new TableComp(devTable));
                return;
            }
            if (data instanceof Service) {
                Service service = (Service) data;
                ServicePane servicePane = new ServicePane(ctrlPoint, service);
                setRightComponent(servicePane);
                return;
            }
            if (data instanceof Icon) {
                Icon icon = (Icon) data;
                IconTable iconTable = new IconTable(icon);
                setRightComponent(new TableComp(iconTable));
                return;
            }
            if (data instanceof Action) {
                Action action = (Action) data;
                ActionPane actionPane = new ActionPane(ctrlPoint, action);
                setRightComponent(actionPane);
                return;
            }
            if (data instanceof Argument) {
                Argument arg = (Argument) data;
                ArgumentTable argTable = new ArgumentTable(arg);
                setRightComponent(new TableComp(argTable));
                return;
            }
            if (data instanceof StateVariable) {
                StateVariable state = (StateVariable) data;
                StateVariablePane servicePane = new StateVariablePane(ctrlPoint, state);
                setRightComponent(servicePane);
                return;
            }
        }

        getDeviceSplitPane().setRightComponent(new JPanel());
    }

}
