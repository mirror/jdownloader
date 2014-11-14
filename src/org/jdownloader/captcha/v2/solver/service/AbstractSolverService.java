package org.jdownloader.captcha.v2.solver.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.SolverService;

public abstract class AbstractSolverService implements SolverService {
    public AbstractSolverService() {

    }

    private List<ChallengeSolver<?>> solverList = new ArrayList<ChallengeSolver<?>>();

    public List<ChallengeSolver<?>> getSolverList() {
        return solverList;
    }

    public void setSolverList(List<ChallengeSolver<?>> solverList) {
        this.solverList = solverList;
    }

    @Override
    public void addSolver(ChallengeSolver<?> solver) {
        solverList.add(solver);
    }

    @Override
    public int getWaitForByID(String solverID) {
        Integer obj = getWaitForMap().get(solverID);
        return obj == null ? 0 : obj.intValue();
    }

    public void setWaitForMap(Map<String, Integer> waitForMap) {
        synchronized (this) {
            this.waitForMap = null;
            getConfig().setWaitForMap(waitForMap);
            // reinit

            getWaitForMap();
        }
    }

    @Override
    public boolean isEnabled() {
        return getConfig().isEnabled();
    }

    @Override
    public void setEnabled(boolean b) {
        getConfig().setEnabled(b);
    }

    protected void initServicePanel(final KeyHandler... handlers) {

        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            @SuppressWarnings("unchecked")
            public void run() {

                for (KeyHandler k : handlers) {

                    k.getEventSender().addListener(new GenericConfigEventListener<Object>() {

                        @Override
                        public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
                        }

                        @Override
                        public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                            ServicePanel.getInstance().requestUpdate(true);
                        }
                    });
                }

            }
        });

    }

    private Map<String, Integer> waitForMap = null;

    public Map<String, Integer> getWaitForMap() {
        synchronized (this) {
            if (waitForMap != null) {
                return waitForMap;
            }
            getConfig()._getStorageHandler().getKeyHandler("WaitForMap").getEventSender().addListener(new GenericConfigEventListener<Object>() {

                @Override
                public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                    synchronized (this) {
                        waitForMap = null;
                        getWaitForMap();
                    }
                }

                @Override
                public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
                }
            });
            Map<String, Integer> map = getConfig().getWaitForMap();

            if (map == null || map.size() == 0) {
                map = getWaitForOthersDefaultMap();
            }
            waitForMap = Collections.synchronizedMap(map);
        }
        return waitForMap;
    }
}
