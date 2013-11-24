package org.jdownloader.gui.notify.captcha;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class CESBubbleSupport extends AbstractBubbleSupport {

    private ArrayList<Element>            elements;
    private static final CESBubbleSupport INSTANCE = new CESBubbleSupport();

    /**
     * get the only existing instance of CESBubbleSupport. This is a singleton
     * 
     * @return
     */
    public static CESBubbleSupport getInstance() {
        return CESBubbleSupport.INSTANCE;
    }

    private CESBubbleSupport() {
        super(_GUI._.CESBubbleSupport_CESBubbleSupport(), CFG_CAPTCHA.REMOTE_CAPTCHA_BUBBLE_ENABLED);
        elements = new ArrayList<Element>();

    }

    @Override
    public List<Element> getElements() {
        return null;
    }

    public CESBubble show(final CESChallengeSolver<?> solver, final CESSolverJob<?> cesSolverJob, final int timeoutms) throws InterruptedException {
        if (!keyHandler.isEnabled()) return null;
        final org.jdownloader.gui.notify.captcha.CESBubble ret = new EDTHelper<CESBubble>() {

            @Override
            public org.jdownloader.gui.notify.captcha.CESBubble edtRun() {
                org.jdownloader.gui.notify.captcha.CESBubble bubble;
                show(bubble = new CESBubble(solver, cesSolverJob, timeoutms));
                return bubble;
            }
        }.getReturnValue();
        try {
            if (ret != null) {
                long waitUntil = System.currentTimeMillis() + timeoutms;

                while (true) {
                    final long rest = waitUntil - System.currentTimeMillis();

                    Thread.sleep(1000);
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            ret.update(rest);
                        }
                    };
                    if (rest <= 0) return ret;
                }

            }
        } catch (InterruptedException e) {
            hide(ret);
            throw e;
        }
        return ret;

    }

    public void hide(CESBubble bubble) {
        if (bubble != null) {
            bubble.hideBubble(5000);
        }
    }
}
