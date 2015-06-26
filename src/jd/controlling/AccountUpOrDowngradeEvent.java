package jd.controlling;

import jd.plugins.Account;

public abstract class AccountUpOrDowngradeEvent extends AccountControllerEvent {

    public AccountUpOrDowngradeEvent(final AccountController caller, final Account account) {
        super(caller, AccountControllerEvent.Types.ACCOUNT_UP_OR_DOWNGRADE, account, (Object[]) null);
    }

    public abstract boolean isPremiumAccount();

    public abstract boolean isPremiumUpgraded();

    public abstract boolean isPremiumDowngraded();

    public abstract boolean isPremiumLimitedRenewal();

    public abstract boolean isPremiumUnlimitedRenewal();

    public abstract long getPremiumRenewalDuration();

    public abstract long getExpireTimeStamp();

    public boolean isPremiumExpired() {
        if (isPremiumAccount()) {
            final long expireTimeStamp = getExpireTimeStamp();
            if (expireTimeStamp == -1 || (expireTimeStamp - System.currentTimeMillis() > 0)) {
                return false;
            }
        }
        return true;
    }

}
