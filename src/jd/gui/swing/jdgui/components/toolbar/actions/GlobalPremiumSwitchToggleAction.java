package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.Color;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JToggleButton;

import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.gui.swing.jdgui.Flashable;
import jd.gui.swing.jdgui.JDGui;

public class GlobalPremiumSwitchToggleAction extends AbstractToolbarToggleAction implements Flashable {

    private JToggleButton bt;
    private final Icon    iconNormal;
    private final Icon    iconHighlight;
    private final Icon    iconSelected;

    public GlobalPremiumSwitchToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS);
        setIconKey(IconKey.ICON_PREMIUM);
        iconNormal = NewTheme.I().getCheckBoxImage(getIconKey(), false, 24);
        iconHighlight = NewTheme.I().getCheckBoxImage(getIconKey(), false, 24, new Color(0xFF9393));
        iconSelected = NewTheme.I().getCheckBoxImage(this.getIconKey(), true, 24);
        CFG_GUI.PREMIUM_DISABLED_WARNING_FLASH_ENABLED.getEventSender().addListener(this, true);
        if (!getKeyHandler().isEnabled() && CFG_GUI.PREMIUM_DISABLED_WARNING_FLASH_ENABLED.isEnabled()) {
            JDGui.getInstance().getFlashController().register(GlobalPremiumSwitchToggleAction.this);
        } else {
            JDGui.getInstance().getFlashController().unregister(GlobalPremiumSwitchToggleAction.this);
        }
    }

    @Override
    protected String createTooltip() {
        if (getKeyHandler().isEnabled()) {
            return _GUI.T.Premium_enabled_button_tooltip_selected();
        } else {
            return _GUI.T.Premium_enabled_button_tooltip_not_selected();
        }
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI.T.GlobalPremiumSwitchToggleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI.T.GlobalPremiumSwitchToggleAction_getNameWhenEnabled_();
    }

    @Override
    public void onConfigValueModified(final KeyHandler<Boolean> keyHandler, Boolean newValue) {
        super.onConfigValueModified(keyHandler, newValue);
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                final boolean useAvailableAccounts = getKeyHandler().isEnabled();
                if (useAvailableAccounts) {
                    setTooltipText(_GUI.T.Premium_enabled_button_tooltip_selected());
                } else {
                    setTooltipText(_GUI.T.Premium_enabled_button_tooltip_not_selected());
                }
                if (!useAvailableAccounts && CFG_GUI.PREMIUM_DISABLED_WARNING_FLASH_ENABLED.isEnabled()) {
                    JDGui.getInstance().getFlashController().register(GlobalPremiumSwitchToggleAction.this);
                } else {
                    JDGui.getInstance().getFlashController().unregister(GlobalPremiumSwitchToggleAction.this);
                }
            }
        };
    }

    @Override
    public AbstractButton createButton() {
        if (bt != null) {
            return bt;
        }
        bt = new JToggleButton(this);
        bt.setIcon(iconNormal);
        bt.setRolloverIcon(iconNormal);
        bt.setSelectedIcon(iconSelected);
        bt.setRolloverSelectedIcon(iconSelected);
        bt.setHideActionText(true);
        return bt;
    }

    @Override
    public void onFlashRegister(long c) {
        onFlash(c);

    }

    @Override
    public void onFlashUnRegister(long c) {
        if (bt != null) {
            bt.setIcon(iconNormal);
            bt.setRolloverIcon(iconNormal);
        }
    }

    @Override
    protected Icon getLargeIconForToolbar() {
        return super.getLargeIconForToolbar();
    }

    @Override
    public Object getValue(String key) {
        return super.getValue(key);
    }

    @Override
    protected Icon getSmallIconForToolbar() {
        return super.getSmallIconForToolbar();
    }

    @Override
    public boolean onFlash(long l) {
        if (bt == null || !bt.isVisible() || !bt.isDisplayable() || !CFG_GUI.PREMIUM_DISABLED_WARNING_FLASH_ENABLED.isEnabled()) {
            return false;
        }
        if (l % 2 != 0) {
            bt.setIcon(iconHighlight);
            bt.setRolloverIcon(iconHighlight);
        } else {
            bt.setIcon(iconNormal);
            bt.setRolloverIcon(iconNormal);
        }
        return true;
    }
}
