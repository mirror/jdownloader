package jd.router.upnp;

/******************************************************************
 *
 *	CyberUPnP for Java
 *
 *	Copyright (C) Satoshi Konno 2002
 *
 *	File : TableComp.java
 *
 ******************************************************************/

import javax.swing.JTable;

public class TableComp extends JTable {
    private static final long serialVersionUID = 1L;

    public TableComp(TableModel tm) {
        super(tm);
    }
}
