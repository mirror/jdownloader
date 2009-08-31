package jd.update;

import java.io.Serializable;

public class Branch implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private boolean beta;

    public boolean isBeta() {
        return beta;
    }

    public void setBeta(boolean beta) {
        this.beta = beta;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
public String toString(){
    return name;

}
    private String name;
    private String desc;

    public Branch(String name) {
        if (name.trim().startsWith("beta_")) {
            beta = true;
        }
        String[] params = name.split(";");
        this.name = params[0];
        if (params.length > 1) this.desc = params[1];
    }
}
