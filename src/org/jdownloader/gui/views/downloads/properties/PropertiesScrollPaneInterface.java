package org.jdownloader.gui.views.downloads.properties;

import java.awt.Container;

import jd.controlling.packagecontroller.AbstractNode;

public interface PropertiesScrollPaneInterface {

    Container getParent();

    void update(AbstractNode objectbyRow);

}
