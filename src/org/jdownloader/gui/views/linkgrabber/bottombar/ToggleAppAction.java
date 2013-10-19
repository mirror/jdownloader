package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtCheckBoxMenuItem;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.actions.ComponentProviderInterface;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.images.NewTheme;

public class ToggleAppAction extends AppAction implements CachableInterface, ComponentProviderInterface, GenericConfigEventListener<Boolean> {

    private BooleanKeyHandler handler;
    private boolean           hidePopupOnClick = false;

    @Customizer(name = "Hide the Popupmenu after clicking")
    public boolean isHidePopupOnClick() {
        return hidePopupOnClick;
    }

    public void setHidePopupOnClick(boolean hidePopupOnClick) {
        this.hidePopupOnClick = hidePopupOnClick;
    }

    public ToggleAppAction(BooleanKeyHandler handler, String name, String tt) {
        this.handler = handler;

        setName(name);
        setTooltipText(tt);
        setSelected(handler.isEnabled());
        handler.getEventSender().addListener(this, true);
    }

    @Override
    public JComponent createComponent(MenuItemData menuItemData) {
        ExtCheckBoxMenuItem ret = new ExtCheckBoxMenuItem(this);
        ret.setHideOnClick(isHidePopupOnClick());
        ret.getAccessibleContext().setAccessibleName(getName());
        ret.getAccessibleContext().setAccessibleDescription(getTooltipText());
        if (StringUtils.isNotEmpty(menuItemData.getName())) {
            ret.setText(menuItemData.getName());
        }
        if (StringUtils.isNotEmpty(menuItemData.getIconKey())) {
            ret.setIcon(NewTheme.I().getIcon(menuItemData.getIconKey(), 20));
        }
        return ret;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        handler.toggle();
    }

    @Override
    public void setData(String data) {
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setSelected(handler.isEnabled());
            }
        };
    }

}
