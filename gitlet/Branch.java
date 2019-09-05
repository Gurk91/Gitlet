package gitlet;

import java.io.Serializable;

public class Branch implements Serializable {

    private final String branchName;

    //Fields
    /**
     * The SHA-1 identifier of my head commit. Tracks where the branch is currently pointing
     */
    private String headCommit;

    //Constructors
    public Branch(String branchname) {
        this.branchName = branchname;
    }

    //Methods
    public String getName() {
        return this.branchName;
    }

    /**
     * Gets SHA-1 identifier of the head commit
     */
    public String getHeadCommit() {
        return this.headCommit;
    }

    public void setHeadCommit(String comm) {
        this.headCommit = comm;
    }

}
