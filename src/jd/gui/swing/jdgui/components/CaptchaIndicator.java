package jd.gui.swing.jdgui.components;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import jd.controlling.captcha.SkipRequest;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.event.ChallengeResponseListener;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.images.NewTheme;

public class CaptchaIndicator extends IconedProcessIndicator implements ChallengeResponseListener {
    /**
     *
     */
    private static final long   serialVersionUID = -7267364376253248300L;
    private final SolverJob<?>  job;
    private final StatusBarImpl statusBar;

    public CaptchaIndicator(final StatusBarImpl statusBar, SolverJob<?> job) {
        super(new AbstractIcon(IconKey.ICON_OCR, 16));
        this.job = job;
        this.statusBar = statusBar;
        setTitle(_GUI.T.StatusBarImpl_initGUI_captcha());
        setDescription(_GUI.T.gui_captchaWindow_waitForInput(job.getChallenge().getHost()));
        setEnabled(true);
        ChallengeResponseController.getInstance().getEventSender().addListener(this, true);
        if (job.isAlive()) {
            setIndeterminate(true);
            statusBar.addProcessIndicator(this);
        }
    }

    protected String getHost() {
        return job.getChallenge().getHost();
    }

    protected DomainInfo getDomainInfo() {
        return job.getChallenge().getDomainInfo();
    }

    protected String getPackageName() {
        final Plugin plugin = job.getChallenge().getPlugin();
        if (plugin instanceof PluginForHost) {
            final DownloadLink link = ((PluginForHost) plugin).getDownloadLink();
            if (link != null) {
                return link.getFilePackage().getName();
            }
        }
        return null;
    }

    protected JPopupMenu createPopup() {
        final JPopupMenu popup = new JPopupMenu();
        popup.add(new AppAction() {
            private static final long serialVersionUID = -968768342263254431L;
            {
                this.setIconKey(IconKey.ICON_CANCEL);
                this.setName(_GUI.T.AbstractCaptchaDialog_AbstractCaptchaDialog_cancel());
            }

            public void actionPerformed(ActionEvent e) {
                job.setSkipRequest(SkipRequest.SINGLE);
            }
        });
        final Plugin plugin = job.getChallenge().getPlugin();
        if (plugin instanceof PluginForHost) {
            popup.add(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_skip_and_disable_all_downloads_from(getHost()));
                    try {
                        setSmallIcon(getDomainInfo().getIcon(16));
                    } catch (final Throwable e) {
                        this.setIconKey(IconKey.ICON_CANCEL);
                    }
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    job.setSkipRequest(SkipRequest.BLOCK_HOSTER);
                }
            });
            popup.add(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_skip_and_disable_package(getPackageName()));
                    setSmallIcon(new BadgeIcon(IconKey.ICON_PACKAGE_OPEN, IconKey.ICON_SKIPPED, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    job.setSkipRequest(SkipRequest.BLOCK_PACKAGE);
                }
            });
            popup.add(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_skip_and_hide_all_captchas_download());
                    setSmallIcon(NewTheme.I().getIcon(IconKey.ICON_CLEAR, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    job.setSkipRequest(SkipRequest.BLOCK_ALL_CAPTCHAS);
                }
            });
            popup.add(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_skip_and_stop_all_downloads());
                    setSmallIcon(new AbstractIcon(IconKey.ICON_STOP, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    job.setSkipRequest(SkipRequest.STOP_CURRENT_ACTION);
                }
            });
        } else {
            popup.add(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_cancel_linkgrabbing());
                    setSmallIcon(new AbstractIcon(IconKey.ICON_STOP, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    job.setSkipRequest(SkipRequest.STOP_CURRENT_ACTION);
                }
            });
            popup.add(new AppAction() {
                {
                    setName(_GUI.T.AbstractCaptchaDialog_createPopup_cancel_stop_showing_crawlercaptchs());
                    setSmallIcon(new AbstractIcon(IconKey.ICON_FIND, 16));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    job.setSkipRequest(SkipRequest.BLOCK_ALL_CAPTCHAS);
                }
            });
        }
        return popup;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            final JPopupMenu popup = createPopup();
            popup.show(CaptchaIndicator.this, e.getPoint().x, 0 - popup.getPreferredSize().height);
        }
    }

    @Override
    public void onNewJobAnswer(SolverJob<?> job, AbstractResponse<?> response) {
    }

    @Override
    public void onJobDone(final SolverJob<?> job) {
        if (job == this.job) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    setIndeterminate(false);
                    statusBar.removeProcessIndicator(CaptchaIndicator.this);
                }
            };
        }
    }

    @Override
    public void onNewJob(SolverJob<?> job) {
    }

    @Override
    public void onJobSolverEnd(ChallengeSolver<?> solver, SolverJob<?> job) {
    }

    @Override
    public void onJobSolverStart(ChallengeSolver<?> solver, SolverJob<?> job) {
    }
}
