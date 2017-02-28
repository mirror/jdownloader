package org.jdownloader.captcha.v2.challenge.oauth;

import jd.plugins.Account;

import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.translate._JDT;

public abstract class AccountLoginOAuthChallenge extends OAuthChallenge {

    private Account account;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public AccountLoginOAuthChallenge(String method, String explain, Account account, String url) {
        super(method, url, explain == null ? _JDT.T.OAUTH_CHALLENGE_EXPLAIN() : explain);
        this.account = account;

    }

    @Override
    public boolean isSolved() {
        // final ResponseList<String> results = getResult();
        // return results != null && results.getValue() != null;
        return false;
    }

    public abstract boolean autoSolveChallenge(SolverJob<Boolean> job);

}
