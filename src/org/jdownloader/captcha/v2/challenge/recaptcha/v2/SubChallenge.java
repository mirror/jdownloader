package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.jdownloader.captcha.v2.ValidationResult;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptcha2FallbackChallenge.ChallengeType;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs.Payload;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs.Response;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs.TileContent;

public class SubChallenge {
    protected String type;
    private String   searchKey;
    private boolean  errorAnotherOneRequired;

    public boolean isErrorAnotherOneRequired() {
        return errorAnotherOneRequired;
    }

    public void setErrorAnotherOneRequired(boolean errorAnotherOneRequired) {
        this.errorAnotherOneRequired = errorAnotherOneRequired;
    }

    public boolean isErrorDynamicTileMore() {
        return errorDynamicTileMore;
    }

    public void setErrorDynamicTileMore(boolean errorDynamicTileMore) {
        this.errorDynamicTileMore = errorDynamicTileMore;
    }

    public boolean isErrorIncorrect() {
        return errorIncorrect;
    }

    public void setErrorIncorrect(boolean errorIncorrect) {
        this.errorIncorrect = errorIncorrect;
    }

    private boolean                        errorDynamicTileMore;
    private boolean                        errorIncorrect;
    private final HashMap<String, Payload> payloads = new HashMap<String, Payload>();
    private volatile String                mainImageUrl;

    public String getMainImageUrl() {
        return mainImageUrl;
    }

    public void setMainImageUrl(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }

    private TileContent[][] grid;

    public SubChallenge() {
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    private int gridWidth;

    public int getGridWidth() {
        return gridWidth;
    }

    public void setGridWidth(int splitWidth) {
        this.gridWidth = splitWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public void setGridHeight(int splitHeight) {
        this.gridHeight = splitHeight;
    }

    private int                    gridHeight;
    private volatile ChallengeType challengeType;
    private int                    reloadCounter = 0;

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setChallengeType(ChallengeType challengeType) {
        this.challengeType = challengeType;
    }

    public ChallengeType getChallengeType() {
        return challengeType;
    }

    public void fillGrid(String mainPayloadUrl) {
        synchronized (payloads) {
            this.mainImageUrl = mainPayloadUrl;
            for (int x = 0; x < getGridWidth(); x++) {
                for (int y = 0; y < getGridHeight(); y++) {
                    grid[x][y] = new TileContent(x, y, payloads.get(mainPayloadUrl));
                }
            }
        }
    }

    public int getDynamicRoundCount() {
        synchronized (responses) {
            return responses.size() + 1;
        }
    }

    public void resetErrors() {
        errorAnotherOneRequired = false;
        errorDynamicTileMore = false;
        errorIncorrect = false;
    }

    public TileContent getTile(int num) {
        return grid[num % getGridWidth()][num / getGridWidth()];
    }

    public TileContent getTile(int x, int y) {
        return grid[x][y];
    }

    public boolean containsPayload(Object key) {
        synchronized (payloads) {
            return payloads.containsKey(key);
        }
    }

    public Payload putPayload(String tileUrl, Payload payload) {
        synchronized (payloads) {
            return payloads.put(tileUrl, payload);
        }
    }

    public Payload getPayloadByUrl(String tileUrl) {
        synchronized (payloads) {
            return payloads.get(tileUrl);
        }
    }

    public int getTileCount() {
        return getGridWidth() * getGridWidth();
    }

    public Payload getMainPayload() {
        synchronized (payloads) {
            return payloads.get(getMainImageUrl());
        }
    }

    public BufferedImage paintImage() {
        Payload mainImage = getMainPayload();
        BufferedImage imgnew = IconIO.createEmptyImage(mainImage.image.getWidth(), mainImage.image.getHeight());
        Graphics2D g2d = (Graphics2D) imgnew.getGraphics();
        if (getChallengeType() == ChallengeType.DYNAMIC) {
            double tileWidth = (double) mainImage.image.getWidth() / getGridWidth();
            double tileHeight = (double) mainImage.image.getHeight() / getGridHeight();
            BufferedImage grayOriginal = null;
            // BufferedImage grayOriginal = null;
            for (int x = 0; x < getGridWidth(); x++) {
                for (int y = 0; y < getGridHeight(); y++) {
                    int tileX = (int) (x * tileWidth);
                    int tileY = (int) (y * tileHeight);
                    TileContent tile = getTile(x, y);
                    if (!isSlotAnnotated(x, y)) {
                        if (tile.getPayload().url.contains("&id=")) {
                            g2d.drawImage(tile.getPayload().image, (int) (x * tileWidth), (int) (y * tileHeight), (int) tileWidth, (int) tileHeight, null);
                            g2d.drawImage(ImageProvider.convertToGrayScale(tile.getPayload().image), (int) (x * tileWidth), (int) (y * tileHeight), (int) tileWidth, (int) tileHeight, null);
                        } else {
                            if (grayOriginal == null) {
                                grayOriginal = ImageProvider.convertToGrayScale(mainImage.image);
                            }
                            g2d.drawImage(grayOriginal, tileX, tileY, tileX + (int) tileWidth, tileY + (int) tileHeight, tileX, tileY, tileX + (int) tileWidth, tileY + (int) tileHeight, null);
                        }
                        Composite c = g2d.getComposite();
                        try {
                            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                            g2d.setColor(Color.WHITE);
                            g2d.fillRect((int) (x * tileWidth), (int) (y * tileHeight), (int) tileWidth, (int) tileHeight);
                        } finally {
                            g2d.setComposite(c);
                        }
                    } else {
                        if (tile.getPayload().url.contains("&id=")) {
                            g2d.drawImage(tile.getPayload().image, (int) (x * tileWidth), (int) (y * tileHeight), (int) tileWidth, (int) tileHeight, null);
                        } else {
                            g2d.drawImage(mainImage.image, tileX, tileY, tileX + (int) tileWidth, tileY + (int) tileHeight, tileX, tileY, tileX + (int) tileWidth, tileY + (int) tileHeight, null);
                        }
                    }
                }
            }
        } else {
            g2d.drawImage(mainImage.image, 0, 0, null);
        }
        // if (!getExplain().contains(_GUI.T.RECAPTCHA_2_Dialog_help_dynamic())) {
        // setExplain(getExplain() + " <br>" + _GUI.T.RECAPTCHA_2_Dialog_help_dynamic());
        // }
        g2d.dispose();
        return imgnew;
    }

    public void initGrid(int x, int y) {
        setGridWidth(x);
        setGridHeight(y);
        grid = new TileContent[x][y];
    }

    public void reload() {
        reloadCounter++;
    }

    public int getReloudCounter() {
        return reloadCounter;
    }

    private final ArrayList<Response> responses = new ArrayList<Response>();
    private HashSet<Integer>          annotatedIndices;

    public ArrayList<Response> getResponses() {
        synchronized (responses) {
            return new ArrayList<Response>(responses);
        }
    }

    public void addResponse(Response resp) {
        synchronized (responses) {
            HashSet<Integer> remove = new HashSet<Integer>();
            for (Integer num : resp.getClickedIndices()) {
                if (num >= 0 && !isSlotAnnotated(num % getGridWidth(), num / getGridWidth())) {
                    resp.getResponse().setValidation(ValidationResult.INVALID);
                    remove.add(num);
                }
            }
            resp.remove(remove);
            // this response selects an image that has not been selected for *unchangedFor* rounds.
            responses.add(resp);
            updateAnnotations();
            HashSet<Integer> selected = new HashSet<Integer>(resp.getClickedIndices());
            for (int i = 0; i < getTileCount(); i++) {
                TileContent tile = getTile(i);
                tile.mark(resp, selected.contains(i), responses.size());
            }
            if (responses.size() > 1) {
                for (int i = 0; i < responses.size() - 1; i++) {
                    Response old = responses.get(i);
                    selected = new HashSet<Integer>(resp.getClickedIndices());
                    int unchanged = Integer.MAX_VALUE;
                    for (int t = 0; t < getTileCount(); t++) {
                        if (!selected.contains(t)) {
                            unchanged = Math.min(staysUnselectedInFurtherResponses(i, t), unchanged);
                            if (unchanged < 0) {
                                break;
                            }
                        }
                    }
                    if (unchanged > 1) {
                        old.getResponse().setValidation(ValidationResult.VALID);
                    }
                }
            }
        }
    }

    private int staysUnselectedInFurtherResponses(int responseIndex, int tileIndex) {
        int unchanged = 0;
        synchronized (responses) {
            for (int i = responseIndex + 1; i < responses.size(); i++) {
                final Response old = responses.get(i);
                if (old.getClickedIndices().contains(tileIndex)) {
                    return -1;
                }
                unchanged++;
            }
        }
        return unchanged;
    }

    private void updateAnnotations() {
        HashSet<Integer> annotated = new HashSet<Integer>();
        for (int x = 0; x < getGridWidth(); x++) {
            for (int y = 0; y < getGridHeight(); y++) {
                int num = x + y * getGridWidth();
                if (isSlotAnnotatedInternal(x, y)) {
                    annotated.add(num);
                }
            }
        }
        if (annotated.size() == 0) {
            // error;
            annotated = null;
        }
        this.annotatedIndices = annotated;
    }

    public boolean isSlotAnnotated(int x, int y) {
        if (true) {
            return true;
        } else {
            int num = x + y * getGridWidth();
            return annotatedIndices == null || annotatedIndices.contains(num);
        }
    }

    private boolean isSlotAnnotatedInternal(int xslot, int yslot) {
        int num = xslot + yslot * getGridWidth();
        synchronized (responses) {
            int count = 0;
            for (int i = responses.size() - 1; i >= 0; i--) {
                if (!responses.get(i).isSelected(num)) {
                    count++;
                }
            }
            if (count > 2) {
                return false;
            }
        }
        return true;
    }

    public boolean isDynamicAndHasZerosInARow(int amount) {
        if (getChallengeType() != ChallengeType.DYNAMIC) {
            return false;
        }
        synchronized (responses) {
            int count = 0;
            for (int i = responses.size() - 1; i >= 0; i--) {
                if (responses.get(i).isSelected(-1) && responses.get(i).getSize() == 1) {
                    count++;
                }
            }
            if (count >= amount) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPayload() {
        synchronized (payloads) {
            return payloads.size() > 0;
        }
    }
}
