package gitlet;

import java.io.Serializable;

public class Head implements Serializable {

    /**
     * Stores the current branch that head points to
     */
    private Branch currentHead;

    //Fields

    //Constructor initializes head to master branch
    public Head(Branch master) {
        this.currentHead = master;
    }

    public Branch getCurrHead() {
        return currentHead;
    }

    public void setHeadBranch(Branch branche) { //this is française
        currentHead = branche;
    }
}
