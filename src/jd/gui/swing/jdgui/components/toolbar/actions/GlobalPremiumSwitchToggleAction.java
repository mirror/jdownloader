package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.Color;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.Timer;

import jd.gui.swing.jdgui.Flashable;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class GlobalPremiumSwitchToggleAction extends AbstractToolbarToggleAction implements Flashable {

    protected Timer       timer;
    private JToggleButton bt;
    private final Icon    iconNormal;
    private final Icon    iconHighlight;
    private final Icon    iconSelected;

    public GlobalPremiumSwitchToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS);
        setIconKey("premium");
        iconNormal = NewTheme.I().getCheckBoxImage(getIconKey(), false, 24);
        iconHighlight = NewTheme.I().getCheckBoxImage(getIconKey(), false, 24, new Color(0xFF9393));
        iconSelected = NewTheme.I().getCheckBoxImage(this.getIconKey(), true, 24);

        if (Boolean.FALSE.equals(CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getValue()) && CFG_GUI.CFG.isPremiumDisabledWarningFlashEnabled()) {
            JDGui.getInstance().getFlashController().register(GlobalPremiumSwitchToggleAction.this);
        } else {
            JDGui.getInstance().getFlashController().unregister(GlobalPremiumSwitchToggleAction.this);
        }
        CFG_GUI.PREMIUM_DISABLED_WARNING_FLASH_ENABLED.getEventSender().addListener(this, true);
    }

    @Override
    protected String createTooltip() {
        if (CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled()) {
            return _GUI._.Premium_enabled_button_tooltip_selected();
        } else {
            return _GUI._.Premium_enabled_button_tooltip_not_selected();
        }
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI._.GlobalPremiumSwitchToggleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI._.GlobalPremiumSwitchToggleAction_getNameWhenEnabled_();
    }

    @Override
    public void onConfigValueModified(final KeyHandler<Boolean> keyHandler, Boolean newValue) {
        super.onConfigValueModified(keyHandler, newValue);
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled()) {
                    setTooltipText(_GUI._.Premium_enabled_button_tooltip_selected());
                } else {
                    setTooltipText(_GUI._.Premium_enabled_button_tooltip_not_selected());
                }
                if (!CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled() && CFG_GUI.PREMIUM_DISABLED_WARNING_FLASH_ENABLED.isEnabled()) {
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
        if (bt == null || !bt.isVisible() || !bt.isDisplayable() || !CFG_GUI.CFG.isPremiumDisabledWarningFlashEnabled()) {
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
