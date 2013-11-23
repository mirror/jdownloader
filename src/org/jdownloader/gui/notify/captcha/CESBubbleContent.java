package org.jdownloader.gui.notify.captcha;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.notify.AbstractBubbleContentPanel;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class CESBubbleContent extends AbstractBubbleContentPanel {

    private JLabel             status;

    private long               startTime;

    private JLabel             duration;

    private ChallengeSolver<?> solver;

    private SolverJob<?>       job;

    private JLabel             statusLbl;

    private JLabel             timeoutLbl;

    private JLabel             durationLbl;

    private ExtButton          button;

    private CESBubble          bubble;

    public CESBubbleContent(final ChallengeSolver<?> solver, final SolverJob<?> job, int timeoutms) {
        super(solver.getIcon(20));
        this.solver = solver;
        this.job = job;
        startTime = System.currentTimeMillis();
        // super("ins 0,wrap 2", "[][grow,fill]", "[grow,fill]");

        // , _GUI._.balloon_reconnect_start_msg(), NewTheme.I().getIcon("reconnect", 32)

        add(timeoutLbl = new JLabel(_GUI._.CESBubbleContent_CESBubbleContent_wait(TimeFormatter.formatMilliSeconds(timeoutms, 0), solver.getName())), "hidemode 3,spanx,spany 2");
        timeoutLbl.setForeground(LAFOptions.getInstance().getColorForErrorForeground());
        SwingUtils.toBold(timeoutLbl);

        add(durationLbl = createHeaderLabel((_GUI._.ReconnectDialog_layoutDialogContent_duration())), "hidemode 3");
        add(duration = new JLabel(""), "hidemode 3");
        add(statusLbl = createHeaderLabel((_GUI._.CESBubbleContent_CESBubbleContent_status())), "hidemode 3");

        add(status = new JLabel(""), "hidemode 3");

        if (job.getChallenge() instanceof ImageCaptchaChallenge) {
            try {
                ImageIcon icon = new ImageIcon(ImageIO.read(((ImageCaptchaChallenge) job.getChallenge()).getImageFile()));
                if (icon.getIconWidth() > 300 || icon.getIconHeight() > 300) {

                    icon = new ImageIcon(IconIO.getScaledInstance(icon.getImage(), 300, 300));
                }
                add(new JSeparator(), "spanx,pushx,growx");
                add(new JLabel(icon), "spanx,pushx,growx");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        progressCircle.setIndeterminate(true);
        progressCircle.setValue(0);
        addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                ((ChallengeSolver<Object>) solver).kill((SolverJob<Object>) job);
                if (bubble != null) {
                    bubble.hideBubble(0);
                }
            }
        });
        add(button = new ExtButton(new AppAction() {
            {
                setName(_GUI._.lit_cancel());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                ((ChallengeSolver<Object>) solver).kill((SolverJob<Object>) job);
                if (bubble != null) {
                    bubble.hideBubble(0);
                }

            }
        }), "hidemode 3,spanx,pushx,growx");
        updateTimer(timeoutms);
    }

    @Override
    protected void addProgress() {
        add(progressCircle, "width 32!,height 32!,pushx,growx,pushy,growy,spany 2,aligny top");
    }

    public void update() {
        SolverStatus s = solver.getStatus(job);
        if (s == null) {
            status.setVisible(false);
            statusLbl.setVisible(false);
        } else {
            status.setVisible(true);
            statusLbl.setVisible(true);
            status.setText(s.getLabel());
            status.setIcon(s.getIcon());
        }
        duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));

    }

    @Override
    public void updateLayout() {
    }

    public static void fill(ArrayList<Element> elements) {
    }

    public void updateTimer(long rest) {
        update();
        timeoutLbl.setText(_GUI._.CESBubbleContent_CESBubbleContent_wait(TimeFormatter.formatMilliSeconds(rest, 0), solver.getName()));
        button.setVisible(true);
        timeoutLbl.setVisible(rest > 0);
        durationLbl.setVisible(rest <= 0);
        duration.setVisible(rest <= 0);
        statusLbl.setVisible(rest <= 0);
        status.setVisible(rest <= 0);
        if (bubble != null) {
            bubble.pack();
            BubbleNotify.getInstance().relayout();
        }
    }

    public void setBubble(CESBubble cesBubble) {
        bubble = cesBubble;
    }

}
