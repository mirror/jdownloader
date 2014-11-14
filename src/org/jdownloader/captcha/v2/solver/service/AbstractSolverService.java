package org.jdownloader.captcha.v2.solver.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.captcha.v2.ChallengeResponseController;
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
    public synchronized int getWaitForByID(String solverID) {
        Map<String, Integer> map = getConfig().getWaitForMap();
        if (map == null) {
            map = getWaitForOthersDefaultMap();
        }
        if (map != null) {
            final Integer obj = map.get(solverID);
            return obj == null ? 0 : Math.max(0, obj.intValue());
        }
        return 0;
    }

    @Override
    public synchronized void setWaitFor(String id, Integer waitFor) {
        Map<String, Integer> map = getConfig().getWaitForMap();
        if (map == null) {
            map = getWaitForOthersDefaultMap();
        }
        if (map != null) {
            if (waitFor == null || waitFor <= 0) {
                map.remove(id);
            } else {
                map.put(id, waitFor);
            }
            getConfig().setWaitForMap(map);
        }
    }

    @Override
    public synchronized Map<String, Integer> getWaitForMapCopy() {
        final HashMap<String, Integer> ret = new HashMap<String, Integer>();
        Map<String, Integer> map = getConfig().getWaitForMap();
        if (map == null) {
            map = getWaitForOthersDefaultMap();
        }
        if (map != null) {
            ret.putAll(map);
        }
        return ret;
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

    public static ArrayList<SolverService> validateWaittimeQueue(SolverService start, SolverService check) {
        if (start == null || check == null) {
            return null;
        }
        return validateWaittimeQueue(start, check, new ArrayList<SolverService>(), new HashSet<SolverService>());
    }

    private static ArrayList<SolverService> validateWaittimeQueue(SolverService start, SolverService check, ArrayList<SolverService> arrayList, HashSet<SolverService> dupe) {
        if (arrayList == null) {
            arrayList = new ArrayList<SolverService>();
        }
        if (dupe == null) {
            dupe = new HashSet<SolverService>();
        }
        if (arrayList.size() == 0) {
            arrayList.add(start);
            dupe.add(start);
        }
        arrayList.add(check);
        if (!dupe.add(check)) {
            return arrayList;
        }
        for (Entry<String, Integer> es : check.getWaitForMapCopy().entrySet()) {
            final SolverService service = ChallengeResponseController.getInstance().getServiceByID(es.getKey());
            if (service != null && es.getValue() != null && es.getValue().intValue() > 0) {
                final ArrayList<SolverService> ret = validateWaittimeQueue(start, service, new ArrayList<SolverService>(arrayList), new HashSet<SolverService>(dupe));
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }
}
