package org.jdownloader.captcha.v2.challenge.keycaptcha;

import java.io.IOException;
import java.util.ArrayList;

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

public class KeyCaptchaPuzzleChallenge extends Challenge<String> {

    private KeyCaptcha helper;
    private boolean    noAutoSolver;
    private Plugin     plugin;

    public KeyCaptcha getHelper() {
        return helper;
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

        public String getBackground() {
            return background;
        }

        public void setBackground(String categories) {
            this.background = categories;
        }

        private String sample;

        public String getSample() {
            return sample;
        }

        public void setSample(String sample) {
            this.sample = sample;
        }

        private String background;
    }

    public class ApiResponse implements Storable {
        public ApiResponse(/* Storable */) {
        }

        private ArrayList<Integer> mouseArray;

        public ArrayList<Integer> getMouseArray() {
            return mouseArray;
        }

        public void setMouseArray(ArrayList<Integer> mouseArray) {
            this.mouseArray = mouseArray;
        }

        public String getOut() {
            return out;
        }

        public void setOut(String cout) {
            this.out = cout;
        }

        private String out;
    }

    @Override
    public AbstractResponse<String> parseAPIAnswer(String json, ChallengeSolver<?> solver) {

        ApiResponse response = JSonStorage.restoreFromString(json, new TypeRef<ApiResponse>() {

        });
        String token;
        try {
            token = helper.sendPuzzleResult(response.getMouseArray(), response.getOut());
        } catch (IOException e) {
            throw new WTFException(e);
        }
        if (token != null) {
            return new KeyCaptchaResponse(this, solver, token, 100);
        }
        return null;
    }

    public Storable getAPIStorable(String format) throws Exception {

        APIData ret = new APIData();
        String[] pieces = new String[getHelper().getPuzzleData().getImages().pieces.size()];
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = IconIO.toDataUrl(getHelper().getPuzzleData().getImages().pieces.get(i));
        }
        ret.setPieces(pieces);
        ret.setBackground(IconIO.toDataUrl(getHelper().getPuzzleData().getImages().backgroundImage));
        ret.setSample(IconIO.toDataUrl(getHelper().getPuzzleData().getImages().sampleImage));

        return ret;
    }

    public boolean isNoAutoSolver() {
        return noAutoSolver;
    }

    public KeyCaptchaPuzzleChallenge(KeyCaptcha keyCaptcha, Plugin plg, boolean noAutoSolver) {
        super("keyCaptchaPuzzle", null);
        this.helper = keyCaptcha;
        this.plugin = plg;

        this.noAutoSolver = noAutoSolver;
    }

    public Plugin getPlugin() {
        return plugin;
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

    public boolean isSolved() {
        final ResponseList<String> results = getResult();
        return results != null && results.getValue() != null;
    }
}
