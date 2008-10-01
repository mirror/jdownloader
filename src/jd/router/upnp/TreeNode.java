package jd.router.upnp;

/******************************************************************
 *
 *	CyberUPnP for Java
 *
 *	Copyright (C) Satoshi Konno 2002
 *
 *	File : TreeNode.java
 *
 ******************************************************************/

import javax.swing.tree.DefaultMutableTreeNode;

public class TreeNode extends DefaultMutableTreeNode {
    private static final long serialVersionUID = 1L;

    public TreeNode() {
        setUserData(null);
    }

    public TreeNode(Object obj) {
        super(obj);
        setUserData(null);
    }

    // //////////////////////////////////////////////
    // userData
    // //////////////////////////////////////////////

    private Object userData = null;

    public void setUserData(Object data) {
        userData = data;
    }

    public Object getUserData() {
        return userData;
    }
}
