package org.jdownloader.controlling.contextmenu;

import java.util.List;

import org.jdownloader.actions.AppAction;

public interface MenuLink extends CustomSettingsPanelInterface {

    String getName();

    String getIconKey();

    List<AppAction> createActionsToLink();

}
