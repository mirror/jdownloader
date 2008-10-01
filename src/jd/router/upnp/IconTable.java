package jd.router.upnp;

/******************************************************************
 *
 *	CyberUPnP for Java
 *
 *	Copyright (C) Satoshi Konno 2002
 *
 *	File : IconTable.java
 *
 ******************************************************************/

import org.cybergarage.upnp.Icon;

public class IconTable extends TableModel {
    private static final long serialVersionUID = 1L;

    public IconTable(Icon icon) {
        super(icon.getIconNode());
    }
}
