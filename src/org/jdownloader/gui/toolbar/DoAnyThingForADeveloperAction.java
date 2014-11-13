package org.jdownloader.gui.toolbar;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;

import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class DoAnyThingForADeveloperAction extends AbstractToolBarAction {
    private static int ID = 0;

    public DoAnyThingForADeveloperAction() {
        setName("DoAnyThingForADeveloperAction");
        setIconKey(IconKey.ICON_BATCH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new Thread() {
            @Override
            public void run() {
                BasicCaptchaChallenge c;
                try {
                    File file;
                    c = new BasicCaptchaChallenge("recaptcha", file = Application.getResource("captchas/recaptcha.jpg"), "", "Enter it", HostPluginController.getInstance().get("uploaded.to").newInstance(PluginClassLoader.getInstance().getChild()), 0) {

                        @Override
                        public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                            return false;
                        }

                    };

                    c.setTimeout(60000);

                    SolverJob<String> job = ChallengeResponseController.getInstance().handle(c);

                    ResponseList<String> responseList = c.getResult();

                    try {
                        Dialog.getInstance().showDialog(new ConfirmDialog(0, "", responseList.getValue(), new ImageIcon(ImageIO.read(file)), null, null));
                        job.validate();
                    } catch (Exception e) {
                        job.invalidate();
                    }
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                } catch (SkipException e1) {
                    e1.printStackTrace();
                } catch (UpdateRequiredClassNotFoundException e2) {
                    e2.printStackTrace();
                }
            }
        }.start();

        // final int id = ID++;
        // BubbleNotify.getInstance().show(new AbstractNotifyWindowFactory() {
        //
        // @Override
        // public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
        // return new BasicNotify("BlaBla....", "Even more Bla ...............", NewTheme.I().getIcon(IconKey.ICON_DESKTOP, 32)) {
        // public String toString() {
        // return "bubble_" + id;
        // }
        // };
        // }
        // });
    }

    @Override
    protected String createTooltip() {
        return null;
    }

}
