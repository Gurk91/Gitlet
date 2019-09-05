package gitlet;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;


class Commit implements Serializable {
    public static String initSHA1 = null;
    /**
     * The SHA-1 identifier of my parent, or null if I am the initial commit.
     */
    private final String parent;
    /**
     * My log message.
     */
    private final String message;
    private String myID;
    /**
     * My timestamp. (java.util.Date)
     */
    private String commitDateTime;

// Fields
    /**
     * A mapping of file names to the SHA-1's of their blobs. We must know what commits have
     */

    private TreeMap<String, String> contents = new TreeMap<>();
    /**
     * A way to keep track of where we branch
     */
    private boolean branchHere;

    // Constructor
    public Commit(String parentId, String message) {
        this.parent = parentId;
        this.message = message;
        this.commitDateTime = getTime();
    }

    // Methods

    public static void main(String[] args) throws ParseException {
        Commit test = new Commit("random numbers", "This is a test");
        System.out.println(test.toString());
        String string = test.commitDateTime;
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = format.parse(string);
        System.out.println(date);

        try {
            Thread.sleep(1000);                 //1000 milliseconds is one second.
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        Commit test2 = new Commit("rando numbers", "This is also a test");
        String string2 = test2.commitDateTime;
        DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date2 = format2.parse(string2);
        System.out.println(date2);
        System.out.println(date2.after(date)); //should be true
        System.out.println(date.after(date2)); //should be false
    }

    private String getTime() {
        Date date = new Date();
        DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String strdate = dateformat.format(date);
        return strdate;
    }

    /**
     * @source http://www.codebind.com/java-tutorials/java-example-convert-date-string/
     */
    public String toString() {
        return String.format("Commit" + " " + myID + "%n" + commitDateTime + "%n" + message);
    }

    /**
     * Get SHA-1 identifier of my parent, or null if I am the initial commit.
     */
    String getParent() {
        return this.parent;
    }

    /**
     * Finalize me and write me to my repository.
     */
    public void finalize() {

    }

    /* Assorted getters and setters for contents and other fields excluded.
       Methods supporting merge also excluded. */

    //Sets the SHA-1 of the current commit
    public void setID(String iD) {
        this.myID = iD;
    }

    public String getMessage() {
        return this.message;
    }

    public String getMyID() {
        return this.myID;
    }

    public String getCommitDateTime() {
        return this.commitDateTime;
    }

    public TreeMap<String, String> getContents() {
        return this.contents;
    }

    public void setContents(TreeMap<String, String> parentContents) {
        this.contents = parentContents;
    }

}

