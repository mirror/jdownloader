package org.jdownloader.captcha.v2;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import jd.controlling.captcha.SkipRequest;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.controlling.UniqueAlltimeID;

public abstract class Challenge<T> {
    private final UniqueAlltimeID id = new UniqueAlltimeID();
    private Class<T>              resultType;

    public UniqueAlltimeID getId() {
        return id;
    }

    abstract public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge);

    @SuppressWarnings("unchecked")
    public Challenge(String method, String explain2) {
        typeID = method;
        explain = explain2;

        Type superClass = this.getClass().getGenericSuperclass();
        while (superClass instanceof Class) {

            superClass = ((Class<?>) superClass).getGenericSuperclass();
            if (superClass == null) throw new IllegalArgumentException("Wrong Construct");

        }
        resultType = (Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public Class<T> getResultType() {
        return resultType;
    }

    abstract public boolean isSolved();

    private String typeID;
    private String explain;

    public String getTypeID() {
        return typeID;
    }

    public void setTypeID(String typeID) {
        this.typeID = typeID;
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }

    public ResponseList<T> getResult() {
        return result;
    }

    public void setResult(ResponseList<T> result) {
        this.result = result;
    }

    private ResponseList<T> result;

    public static String getHost(Challenge<?> challenge) {

        if (challenge instanceof ImageCaptchaChallenge) { return ((ImageCaptchaChallenge) challenge).getPlugin().getHost();

        }
        return null;
    }

    public static DownloadLink getDownloadLink(Challenge<?> challenge) {

        Plugin plugin = getPlugin(challenge);
        if (plugin == null) return null;
        if (plugin instanceof PluginForHost) { return ((PluginForHost) plugin).getDownloadLink(); }
        return null;
    }

    private static Plugin getPlugin(Challenge<?> challenge) {

        if (challenge instanceof ImageCaptchaChallenge) { return ((ImageCaptchaChallenge) challenge).getPlugin();

        }
        return null;
    }
}
