package jd.controlling.downloadcontroller;

import java.lang.ref.WeakReference;

import javax.swing.Icon;

import jd.controlling.downloadcontroller.AccountCache.ACCOUNTTYPE;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.NoProxySelector;
import jd.controlling.proxy.SingleDirectGatewaySelector;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;

import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.WaitingSkipReason;
import org.jdownloader.plugins.WaitingSkipReason.CAUSE;

public class HistoryEntry {

    private ACCOUNTTYPE accountType;
    private String      resultIconKey;
    private String      resultStatus;

    public String getResultStatus() {
        if (StringUtils.isNotEmpty(resultStatus)) {
            return resultStatus;
        }
        String ret = null;
        if (resultSkipReason != null) {
            ret = resultSkipReason.getExplanation(this);
        } else if (resultFinalStatus != null) {
            ret = resultFinalStatus.getExplanation(this, link);
        } else if (resultConditionalSkipReason != null) {
            ret = resultConditionalSkipReason.getMessage(this, link);
        }
        return ret;
    }

    private WeakReference<DownloadLinkCandidate> candidate;
    private String                               accountIconKey;

    private String getAccountIconKey() {
        return accountIconKey;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public String getGatewayStatus() {
        return gatewayStatus;
    }

    private String getGatewayIconKey() {
        return gatewayIconKey;
    }

    private String                         accountStatus;
    private String                         gatewayStatus;
    private String                         gatewayIconKey;
    private long                           createTime;
    private SkipReason                     resultSkipReason;
    private ConditionalSkipReason          resultConditionalSkipReason;
    private FinalLinkState                 resultFinalStatus;
    private DownloadLink                   link;
    private WeakReference<DownloadSession> session;
    private Account                        account;

    public DownloadLinkCandidate getCandidate() {
        return candidate.get();
    }

    public Icon getResultIcon(int size) {
        if (resultSkipReason != null) {
            return resultSkipReason.getIcon(this, size);
        }
        String key = resultIconKey;
        if (key != null) {
            return new AbstractIcon(key, size);
        }
        if (resultSkipReason != null) {
            return resultSkipReason.getIcon(this, size);
        } else if (resultFinalStatus != null) {
            return resultFinalStatus.getIcon(size);
        } else if (resultConditionalSkipReason != null) {
            return resultConditionalSkipReason.getIcon(this, link);
        }

        return null;
    }

    public Icon getAccountIcon(int size) {
        String key = getAccountIconKey();
        if (key == null) {
            return null;
        }
        return new AbstractIcon(key, size);
    }

    public Icon getGatewayIcon(int size) {
        String key = getGatewayIconKey();
        if (key == null) {
            return null;
        }
        if ("expired".equals(key)) {
            return new ExtMergedIcon(new AbstractIcon(IconKey.ICON_ERROR, size)).add(new AbstractIcon(IconKey.ICON_WAIT, (int) (size * 0.7)), (int) (size * 0.2), (int) (size * 0.3));

        }
        return new AbstractIcon(key, size);

    }

    public DownloadSession getSession() {
        return session.get();
    }

    public HistoryEntry(DownloadLinkCandidate candidate) {
        this.candidate = new WeakReference<DownloadLinkCandidate>(candidate);
        this.session = new WeakReference<DownloadSession>(DownloadWatchDog.getInstance().getSession());
        this.link = candidate.getLink();
        createTime = System.currentTimeMillis();
        resultStatus = _GUI._.CandidateTooltipTableModel_initColumns_running_();
        resultIconKey = IconKey.ICON_PLAY;
    }

    public static HistoryEntry create(DownloadLinkCandidate candidate) {
        HistoryEntry ret = new HistoryEntry(candidate);
        collectInfo(candidate.getCachedAccount().getAccount(), ret);
        ret.accountType = candidate.getCachedAccount().getType();
        AbstractProxySelectorImpl proxySel = candidate.getProxySelector();
        SingleDownloadController controller = candidate.getLink().getDownloadLinkController();
        if (controller != null) {
            HTTPProxy proxy = controller.getUsedProxy();
            if (proxy != null) {
                ret.gatewayStatus = proxy.toString();
                switch (proxy.getType()) {
                case DIRECT:
                case NONE:
                    break;
                case HTTP:
                    ret.gatewayIconKey = IconKey.ICON_PROXY;

                    break;
                case SOCKS4:
                    ret.gatewayIconKey = IconKey.ICON_PROXY;
                    break;
                case SOCKS5:
                    ret.gatewayIconKey = IconKey.ICON_PROXY;
                    break;
                }

            }
        }
        if (StringUtils.isEmpty(ret.gatewayStatus)) {
            if (proxySel instanceof NoProxySelector) {
                ret.gatewayStatus = HTTPProxy.NONE.toString();
            } else if (proxySel instanceof SingleDirectGatewaySelector) {
                ret.gatewayStatus = ((SingleDirectGatewaySelector) proxySel).getProxy().toString();
            } else {
                ret.gatewayIconKey = IconKey.ICON_PROXY;
                ret.gatewayStatus = proxySel.toString();
            }

        }
        return ret;
    }

    public ACCOUNTTYPE getAccountType() {
        return accountType;
    }

    private static void collectInfo(Account account, HistoryEntry history) {
        history.account = account;
        if (account == null) {

            history.accountIconKey = IconKey.ICON_DOWNLOAD;
            history.accountStatus = _GUI._.CandidateAccountColumn_getStringValue_free_();
            return;
        }
        if (account.isChecking()) {
            history.accountIconKey = IconKey.ICON_REFRESH;

        }
        if (account.getError() == null) {
            history.accountIconKey = IconKey.ICON_OK;

            AccountInfo ai = account.getAccountInfo();
            String ret = ai == null ? null : ai.getStatus();
            if (StringUtils.isEmpty(ret)) {
                if (account.isTempDisabled()) {
                    if (StringUtils.isNotEmpty(account.getErrorString())) {
                        history.accountStatus = account.getErrorString();
                        return;
                    }
                    ret = _GUI._.PremiumAccountTableModel_getStringValue_temp_disabled();
                } else {
                    ret = _GUI._.PremiumAccountTableModel_getStringValue_account_ok_();
                }
            } else {
                if (account.isTempDisabled()) {
                    if (StringUtils.isNotEmpty(account.getErrorString())) {

                        history.accountIconKey = IconKey.ICON_WAIT;
                        history.accountStatus = account.getErrorString();
                        return;
                    }
                    ret = _GUI._.PremiumAccountTableModel_getStringValue_temp_disabled2(ret);
                } else {
                    ret = _GUI._.PremiumAccountTableModel_getStringValue_account_ok_2(ret);
                }

            }
            history.accountStatus = ret;
            return;
        }
        if (StringUtils.isNotEmpty(account.getErrorString())) {
            history.accountStatus = account.getErrorString();
            return;
        }
        switch (account.getError()) {
        case EXPIRED:
            history.accountIconKey = "expired";
            history.accountStatus = _GUI._.PremiumAccountTableModel_getStringValue_status_expired();
            break;
        case INVALID:
            history.accountIconKey = IconKey.ICON_ERROR;
            history.accountStatus = _GUI._.PremiumAccountTableModel_getStringValue_status_invalid();
            break;
        case PLUGIN_ERROR:
            history.accountIconKey = IconKey.ICON_ERROR;
            history.accountStatus = _GUI._.PremiumAccountTableModel_getStringValue_status_plugin_error();
            break;
        case TEMP_DISABLED:
            history.accountIconKey = IconKey.ICON_WAIT;
            history.accountStatus = _GUI._.PremiumAccountTableModel_getStringValue_status_unknown_error();
            break;
        default:
            history.accountIconKey = IconKey.ICON_ERROR;
            history.accountStatus = _GUI._.PremiumAccountTableModel_getStringValue_status_unknown_error();
            break;
        }
    }

    public Account getAccount() {
        return account;
    }

    public static void updateResult(HistoryEntry history, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        try {
            history.resultStatus = null;
            history.resultIconKey = null;
            if (result == null || result.getResult() == null) {
                history.resultIconKey = IconKey.ICON_PLAY;

                history.resultStatus = _GUI._.CandidateTooltipTableModel_initColumns_running_();

            } else {
                WaitingSkipReason sr;
                switch (result.getResult()) {
                case PROXY_UNAVAILABLE:

                    break;
                case CONDITIONAL_SKIPPED:
                    history.resultConditionalSkipReason = result.getConditionalSkip();

                    break;

                case IP_BLOCKED:

                    history.resultConditionalSkipReason = new WaitingSkipReason(CAUSE.IP_BLOCKED, result.getWaitTime(), result.getMessage());

                    break;
                case HOSTER_UNAVAILABLE:

                    history.resultConditionalSkipReason = new WaitingSkipReason(CAUSE.HOST_TEMP_UNAVAILABLE, result.getWaitTime(), result.getMessage());

                    break;
                case FILE_UNAVAILABLE:

                    history.resultConditionalSkipReason = new WaitingSkipReason(CAUSE.FILE_TEMP_UNAVAILABLE, result.getWaitTime(), result.getMessage());

                    break;
                case CONNECTION_ISSUES:

                    history.resultConditionalSkipReason = new WaitingSkipReason(CAUSE.CONNECTION_TEMP_UNAVAILABLE, result.getWaitTime(), result.getMessage());

                    break;
                case SKIPPED:
                    history.resultSkipReason = result.getSkipReason();

                    break;
                case PLUGIN_DEFECT:
                    history.resultFinalStatus = FinalLinkState.PLUGIN_DEFECT;

                    break;
                case OFFLINE_TRUSTED:
                    history.resultFinalStatus = FinalLinkState.OFFLINE;
                    break;
                case FINISHED_EXISTS:

                    history.resultFinalStatus = FinalLinkState.FINISHED_MIRROR;

                    break;
                case FINISHED:
                    history.resultFinalStatus = FinalLinkState.FINISHED;
                    break;
                case FAILED_EXISTS:

                    history.resultFinalStatus = FinalLinkState.FAILED_EXISTS;

                    break;
                case FAILED:
                    history.resultFinalStatus = FinalLinkState.FAILED;
                    break;
                case STOPPED:

                    history.resultStatus = _GUI._.CandidateTooltipTableModel_configureRendererComponent_stopped_();
                    history.resultIconKey = IconKey.ICON_CANCEL;
                    break;
                case ACCOUNT_ERROR:

                    /* there was an unknown account issue */

                    if (result.getThrowable() != null) {
                        history.resultStatus = result.getThrowable().getMessage();
                    } else {
                        history.resultStatus = null;
                    }
                    history.resultIconKey = IconKey.ICON_FALSE;

                    break;
                case ACCOUNT_INVALID:

                    /* account has been recognized as valid and/or premium but now throws invalid messages */

                    if (result.getThrowable() != null) {
                        history.resultStatus = result.getThrowable().getMessage();
                    } else {
                        history.resultStatus = null;
                    }
                    history.resultIconKey = IconKey.ICON_FALSE;
                    break;
                case ACCOUNT_UNAVAILABLE:
                    history.resultStatus = _GUI._.CandidateTooltipTableModel_configureRendererComponent_account_unavailable();
                    history.resultIconKey = IconKey.ICON_FALSE;
                    break;
                case ACCOUNT_REQUIRED:
                    history.resultStatus = _GUI._.CandidateTooltipTableModel_configureRendererComponent_account_required();
                    history.resultIconKey = IconKey.ICON_FALSE;
                    break;
                case CAPTCHA:
                    history.resultSkipReason = SkipReason.CAPTCHA;

                    break;
                case FATAL_ERROR:

                    history.resultFinalStatus = FinalLinkState.FAILED_FATAL;
                    if (StringUtils.isNotEmpty(result.getMessage())) {
                        history.resultStatus = result.getMessage();
                    }
                    history.resultIconKey = IconKey.ICON_FALSE;
                    break;
                default:
                    history.resultStatus = result.getResult() + "";
                    if (StringUtils.isNotEmpty(result.getMessage())) {
                        history.resultStatus = result.getMessage();
                    }
                    break;
                }

                if (StringUtils.isEmpty(history.resultStatus)) {
                    history.resultStatus = result.getMessage();

                }

            }
        } finally {

        }
    }

    public long getCreateTime() {
        return createTime;
    }

    public DownloadLink getLink() {
        return link;
    }
}
