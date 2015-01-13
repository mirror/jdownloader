package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import java.util.ArrayList;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.settings.Pair;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public abstract class AbstractCaptchaSolverConfigPanel extends AbstractConfigPanel {
    /**
     * 
     */
    private static final long              serialVersionUID = 1L;
    private Pair<CaptchaRegexListTextPane> blacklist;
    private Pair<CaptchaRegexListTextPane> whitlist;

    public AbstractCaptchaSolverConfigPanel() {
        super(0);
        setLayout(new MigLayout("ins 0, wrap 2", "[][grow,fill]", "[]"));

    }

    protected void addBlackWhiteList(final ChallengeSolverConfig cfg) {
        addHeader(_GUI._.captcha_settings_black_whitelist_header(), new AbstractIcon(IconKey.ICON_LIST, 32));
        addDescription(_GUI._.captcha_settings_black_whitelist_description());
        BooleanKeyHandler keyHandler = cfg._getStorageHandler().getKeyHandler("BlackWhiteListingEnabled", BooleanKeyHandler.class);
        Pair<Checkbox> condition = null;
        if (keyHandler != null) {
            condition = addPair(_GUI._.captcha_settings_blacklist_enabled(), null, new jd.gui.swing.jdgui.views.settings.components.Checkbox(keyHandler));

        }
        blacklist = addPair(_GUI._.captcha_settings_blacklist(), null, new CaptchaRegexListTextPane());
        whitlist = addPair(_GUI._.captcha_settings_whitelist(), null, new CaptchaRegexListTextPane());
        if (condition != null) {
            blacklist.setConditionPair(condition);
            whitlist.setConditionPair(condition);
        }
        blacklist.getComponent().setList(cfg.getBlacklistEntries());
        whitlist.getComponent().setList(cfg.getWhitelistEntries());
        blacklist.getComponent().addStateUpdateListener(new StateUpdateListener() {

            @Override
            public void onStateUpdated() {
                cfg.setBlacklistEntries(new ArrayList<String>(blacklist.getComponent().getList()));
            }
        });

        whitlist.getComponent().addStateUpdateListener(new StateUpdateListener() {

            @Override
            public void onStateUpdated() {
                cfg.setWhitelistEntries(new ArrayList<String>(whitlist.getComponent().getList()));
            }
        });
    }

    @Override
    protected void onShow() {

        super.onShow();
    }
}
