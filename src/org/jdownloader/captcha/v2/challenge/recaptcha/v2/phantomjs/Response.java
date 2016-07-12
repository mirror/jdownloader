package org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.SubChallenge;

public class Response {

    private ArrayList<Integer>       clickedIndices;

    private AbstractResponse<String> response;

    private HashSet<Integer>         clickedSet;

    public List<Integer> getClickedIndices() {
        return Collections.unmodifiableList(clickedIndices);
    }

    public Response(AbstractResponse<String> response, SubChallenge sc) {
        this.response = response;
        final String[] parts = response.getValue().split("[,]+");

        this.clickedIndices = new ArrayList<Integer>();
        this.clickedSet = new HashSet<Integer>();
        if (response.getValue().length() > 0) {
            for (int i = 0; i < parts.length; i++) {

                final int num = Integer.parseInt(parts[i]) - 1;

                if (num < sc.getGridHeight() * sc.getGridWidth()) {
                    clickedIndices.add(num);
                    clickedSet.add(num);

                }

            }
        }
    }

    public AbstractResponse<String> getResponse() {
        return response;
    }

    public int getSize() {
        return clickedIndices.size();
    }

    public boolean isSelected(Integer num) {
        return clickedSet.contains(num);
    }

    public void remove(Integer num) {
        clickedSet.remove(num);
        clickedIndices.remove(num);

    }

    public void remove(HashSet<Integer> remove) {
        clickedSet.removeAll(remove);
        clickedIndices.removeAll(remove);
    }

}
