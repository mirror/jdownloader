package org.jdownloader.captcha.v2.challenge.keycaptcha;

import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.utils.images.IconIO;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solverjob.ResponseList;

public class KeyCaptchaCategoryChallenge extends Challenge<String> {

    private final KeyCaptcha helper;
    private final boolean    noAutoSolver;
    private final Plugin     plugin;

    public KeyCaptchaCategoryChallenge(KeyCaptcha keyCaptcha, Plugin plg, boolean noAutoSolver) {
        super("KeyCaptchaCategoryChallenge", null);
        helper = keyCaptcha;
        this.plugin = plg;
        this.noAutoSolver = noAutoSolver;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public KeyCaptcha getHelper() {
        return helper;
    }

    public boolean isNoAutoSolver() {
        return noAutoSolver;
    }

    /**
     * Used to validate result against expected pattern. <br />
     * This is different to AbstractBrowserChallenge.isSolved, as we don't want to throw the same error exception.
     *
     * @param result
     * @return
     * @author raztoki
     */
    protected final boolean isCaptchaResponseValid() {
        if (isSolved()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean validateResponse(AbstractResponse<String> response) {
        if (response.getPriority() <= 0) {
            return false;
        }
        return true;
    }

    public boolean isSolved() {
        final ResponseList<String> results = getResult();
        return results != null && results.getValue() != null;
    }

    public class APIData implements Storable {
        public APIData(/* Storable */) {
        }

        private String[] pieces;

        public String[] getPieces() {
            return pieces;
        }

        public void setPieces(String[] pieces) {
            this.pieces = pieces;
        }

        public String getCategories() {
            return categories;
        }

        public void setCategories(String categories) {
            this.categories = categories;
        }

        private String categories;
    }

    @Override
    public AbstractResponse<String> parseAPIAnswer(String result, String resultFormat, ChallengeSolver<?> solver) {
        final String token;
        try {
            token = helper.sendCategoryResult(JSonStorage.restoreFromString(result, TypeRef.STRING));
        } catch (Exception e) {
            throw new WTFException(e);
        }
        if (token != null) {
            return new KeyCaptchaResponse(this, solver, token, 100);
        }
        return null;
    }

    public Storable getAPIStorable(String format) throws Exception {
        final CategoryData data = getHelper().getCategoryData();
        final APIData ret = new APIData();
        final String[] pieces = new String[data.getImages().size()];
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = IconIO.toDataUrl(IconIO.toBufferedImage(data.getImages().get(i)), IconIO.DataURLFormat.PNG);
        }
        ret.setPieces(pieces);
        ret.setCategories(IconIO.toDataUrl(data.getBackground(), IconIO.DataURLFormat.JPG));
        return ret;
    }

}
