package org.jdownloader.controlling.contextmenu;

import java.util.List;

import javax.swing.JComponent;

import org.jdownloader.actions.AppAction;

public interface MenuLink {

    String getName();

    String getIconKey();

    JComponent createSettingsPanel();

    List<AppAction> createActionsToLink();

}
