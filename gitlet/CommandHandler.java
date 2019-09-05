package gitlet;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * Class handles inputs passed into gitlet.Main
 */
public class CommandHandler {

    /**
     * Initializes gitlet repository
     * <p>
     * https://stackoverflow.com/questions/15571496/
     * how-to-check-if-a-folder-exists
     */
    public static void init() {
        String current = System.getProperty("user.dir");
        File check = new File(current + "/.gitlet");
        if (check.exists() && check.isDirectory()) {
            System.out.println(
                "A gitlet version control system already exists in the current directory.");
            return;
        }
        File dir = new File(current + "/.gitlet");
        dir.mkdir();
        File stagingArea = new File(current + "/.gitlet/Staging Area");
        stagingArea.mkdir();
        File commits = new File(current + "/.gitlet/Commits");
        commits.mkdir();
        Commit initial = new Commit(null, "initial commit");
        byte[] tempSerial = serialize(initial);

        String commitName = Utils.sha1(tempSerial, initial.getMessage(),
            initial.getCommitDateTime());
        initial.setID(commitName);
        Commit.initSHA1 = commitName;
        byte[] finalSerial = serialize(initial);
        File storedCommit = Utils.join(current, ".gitlet", "Commits", commitName);
        Utils.writeContents(storedCommit, finalSerial);

        File branches = Utils.join(current, ".gitlet", "Branches");
        branches.mkdir();
        Branch master = new Branch("master");
        File storedBranch = Utils.join(branches, master.getName());
        master.setHeadCommit(commitName);
        byte[] serializedBranch = serialize(master);
        Utils.writeContents(storedBranch, serializedBranch);

        File headsRoll = Utils.join(current, ".gitlet", "Heads");
        headsRoll.mkdir();
        Head currentHead = new Head(master);
        File storedHead = Utils.join(headsRoll, master.getName());
        byte[] serialHead = serialize(currentHead);
        Utils.writeContents(storedHead, serialHead);
        File blobs = Utils.join(current, ".gitlet", "Blobs");
        blobs.mkdir();
        File removals = Utils.join(current, ".gitlet", "Removals");
        removals.mkdir();

    }


    /*
      Adds files to staging area
     */
    public static void add(String[] files) throws IOException, ClassNotFoundException {
        int index = 1;
        int temp = index;
        String path = System.getProperty("user.dir");
        Head currHead;
        File inFile = Utils.join(path, ".gitlet", "Heads", "master");
        currHead = (Head) deserialize(inFile);
        String parentID = currHead.getCurrHead().getHeadCommit();
        File f = Utils.join(path, ".gitlet", "Commits");
        File[] matchingFiles = f.listFiles((dir, name) -> name.startsWith(parentID));
        if (matchingFiles == null) {
            System.out.println("File does not exist");
            return;
        }


        File maybeRemoved = Utils.join(path, ".gitlet", "Removals", files[temp]);
        if (maybeRemoved.exists()) {
            maybeRemoved.delete();
        }

        File parentFile = matchingFiles[0];
        Commit currCommit = (Commit) deserialize(parentFile);
        if (currCommit != null
            && currCommit.getContents().containsKey(files[index])) {
            String blobID = currCommit.getContents().get(files[index]);
            File blob = Utils.join(path, ".gitlet", "Blobs");
            File[] matches = blob.listFiles((dir, name) -> name.startsWith(blobID));
            byte[] originalFile = Utils.readContents(matches[0]);
            byte[] toAdd = Utils.readContents(Utils.join(path, files[index]));
            if (Arrays.equals(originalFile, toAdd)) {
                return;
            }

        }
        while (index < files.length) {
            File curr = Utils.join(path, files[index]);

            if (!(curr.exists())) {
                System.out.println("File does not exist.");
                return;
            }

            byte[] contents = Utils.readContents(curr);
            File copy = Utils.join(path, ".gitlet", "Staging Area", files[index]);
            Utils.writeContents(copy, contents);
            index += 1;
        }


    }

    /**
     * Commits files to .gitlet directory
     *
     * @source https://stackoverflow.com/questions/4852531/find-files-in-a-folder-using-java
     */

    public static void commit(String msg) throws IOException, ClassNotFoundException {
        String currDirectory = System.getProperty("user.dir");
        Head currHead;
        boolean noAdds = true;
        boolean noRemovals = true;
        File inFile = Utils.join(currDirectory, ".gitlet", "Heads", "master");
        currHead = (Head) deserialize(inFile);
        Branch currBranch = currHead.getCurrHead();
        String parentID = currHead.getCurrHead().getHeadCommit();
        File f = Utils.join(currDirectory, ".gitlet", "Commits");
        File[] matchingFiles = f.listFiles((dir, name) -> name.startsWith(parentID));
        File parentFile = matchingFiles[0];
        Commit parentCommit = (Commit) deserialize(parentFile);
        Commit next;
        if (parentCommit == null) {
            next = new Commit(Commit.initSHA1, msg);
        } else {
            next = new Commit(parentCommit.getMyID(), msg);
        }
        next.setContents(parentCommit.getContents());
        TreeMap<String, String> currContents = next.getContents();
        /*  //@source https://stackoverflow.com/questions/4917326/
         * how-to-iterate-over-the-files-of-a-certain-directory-in-java */
        File dir = Utils.join(currDirectory, ".gitlet", "Staging Area");
        File[] directoryListing = dir.listFiles();
        if (directoryListing.length != 0) {
            noAdds = false;
            for (File child : directoryListing) {
                currContents.remove(child.getName());
                byte[] fileStuff = Utils.readContents(child);
                String val = Utils.sha1(child.getName(), fileStuff);
                File storedBlob = Utils.join(currDirectory, ".gitlet", "Blobs", val);
                Utils.writeContents(storedBlob, fileStuff);
                currContents.put(child.getName(), val);
                child.delete();
            }

        }

        /** Following code checks for files marked for removal */
        File marked = Utils.join(currDirectory, ".gitlet", "Removals");
        File[] markedNames = marked.listFiles();
        if (markedNames.length != 0) {
            noRemovals = false;
            for (File file : markedNames) {
                currContents.remove(file.getName());
                file.delete();
            }
        }

        if (noAdds && noRemovals) {
            System.out.println("No changes added to the commit.");
            return;
        }
        byte[] tempSer = serialize(next);
        String commitName = Utils.sha1(tempSer, next.getMessage(),
            next.getCommitDateTime(), next.getParent());
        next.setID(commitName);
        byte[] finalSer = serialize(next);
        currBranch.setHeadCommit(commitName);
        File storedCommit = Utils.join(currDirectory, ".gitlet", "Commits", commitName);
        Utils.writeContents(storedCommit, finalSer);

        File headsRoll = Utils.join(currDirectory, ".gitlet", "Heads", "master");
        byte[] serHead = serialize(currHead); //Should serialize the branch also
        Utils.writeContents(headsRoll, serHead);

        File branches = Utils.join(currDirectory, ".gitlet", "Branches");
        File storedBranch = Utils.join(branches, currBranch.getName());
        byte[] serBranch = serialize(currBranch);
        Utils.writeContents(storedBranch, serBranch);
    }

    public static void remove(String filename) throws IOException, ClassNotFoundException {
        boolean tracked = false;
        String path = System.getProperty("user.dir");
        /**Code below checks that the file actually changed from most recent commit
         * If runtime ends up being too long, can we do it woth SHA-1 comparisons?
         * */
        Head currHead; //we have to deserialize the Head to get its pointer here
        File inFile = Utils.join(path, ".gitlet", "Heads", "master");
        currHead = (Head) deserialize(inFile);
        Commit currCommit = getCommitObj(currHead, currHead.getCurrHead().getHeadCommit());
        if (currCommit.getContents().containsKey(filename)) {
            tracked = true;
        }
        File staged = Utils.join(path, ".gitlet", "Staging Area", filename);
        if (tracked) { //File is being tracked in the current commit
            File condemned = Utils.join(path, filename);
            Utils.restrictedDelete(condemned); //Remove from working directory
            if (staged.exists()) {
                staged.delete(); //Unstages the file if it is staged
            }
            File toRemove = Utils.join(path, ".gitlet", "Removals", filename);
            if (!(toRemove.exists())) {
                toRemove.createNewFile();
            }
            /**currCommit.getContents().remove(filename); //Removes from the current commit
             byte[] edited_commit = serialize(currCommit);
             File stored = Utils.join(path, ".gitlet", "Commits", currCommit.getMyID());
             Utils.writeContents(stored, edited_commit);*/
        } else if (staged.exists()) {
            staged.delete();
        } else {
            System.out.println("No reason to remove the file.");
        }

    }

    private static Commit getCommitObj(Head master, String sha1) {
        String path = System.getProperty("user.dir");
        /**Code below checks that the file actually changed from most recent commit
         * If runtime ends up being too long, can we do it woth SHA-1 comparisons?
         * */
        String parentID = master.getCurrHead().getHeadCommit();
        File f = Utils.join(path, ".gitlet", "Commits");
        File[] matchingFiles = f.listFiles((dir, name) -> name.startsWith(parentID));
        File parentFile = matchingFiles[0];
        Commit currCommit = null;
        try {
            currCommit = (Commit) deserialize(parentFile);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return currCommit;

    }

    /**
     * Prints status to terminal
     */
    public static void status() throws IOException, ClassNotFoundException {
        String path = System.getProperty("user.dir");
        System.out.println("=== Branches ===");
        File inFile = Utils.join(path, ".gitlet", "Heads", "master");
        Head currhead = (Head) deserialize(inFile);
        Branch headBranch = currhead.getCurrHead();
        System.out.println("*" + headBranch.getName());
        File branches = Utils.join(path, ".gitlet", "Branches");
        File[] printBranches = branches.listFiles();
        ArrayList<String> res = new ArrayList<>();
        if (printBranches != null) {
            for (File file : printBranches) { //Iterates through the files marked for removal
                res.add(file.getName());
            }
            for (String str : res) {
                if (!(str.equals(headBranch.getName()))) {
                    System.out.println(str);
                }
            }
        }

        System.out.println();
        System.out.println("=== Staged Files ===");
        File staged = Utils.join(path, ".gitlet", "Staging Area");
        sortandPrintFiles(staged);

        System.out.println();
        System.out.println("=== Removed Files ===");
        File removedFiles = Utils.join(path, ".gitlet", "Removals");
        sortandPrintFiles(removedFiles);

        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");

    }

    private static void sortandPrintFiles(File directory) {
        ArrayList<String> result = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                result.add(f.getName());
            }
        }
        Collections.sort(result); //Is this redundant?
        if (result != null) {
            for (String S : result) {
                System.out.println(S);
            }
        }
    }

    /**
     * Prints history of most recent commits
     */
    public static void log() {
        String current = System.getProperty("user.dir");
        Head currHead = null;
        String headSHA = "";
        Commit headCommit = null;
        File inFile = Utils.join(current, ".gitlet", "Heads", "master");
        try {
            currHead = (Head) deserialize(inFile);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        Branch currBranch = currHead.getCurrHead();
        headSHA = currBranch.getHeadCommit();
        headCommit = getCommitObj(currHead, headSHA);
        TreeMap<String, String> commits = headCommit.getContents();
        String parent = "";
        File parentFile;
        System.out.println("===");
        System.out.println(headCommit.toString());
        System.out.println();
        while (headCommit.getParent() != null) {
            parent = headCommit.getParent();
            //System.out.println(parent);
            parentFile = Utils.join(current, ".gitlet", "Commits", parent);
            try {
                headCommit = (Commit) deserialize(parentFile);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("===");
            System.out.println(headCommit.toString());
            System.out.println();
        }

    }

    public static void globalLog() {
        String path = System.getProperty("user.dir");
        File commits = Utils.join(path, ".gitlet", "Commits");
        Commit currentCommit = null;
        File[] allCommits = commits.listFiles();
        for (File file : allCommits) {
            try {
                currentCommit = (Commit) deserialize(file);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("===");
            System.out.println(currentCommit.toString());
            System.out.println();
        }
    }

    public static void find(String message) {
        String path = System.getProperty("user.dir");
        File commits = Utils.join(path, ".gitlet", "Commits");
        Commit currentCommit = null;
        boolean nonExistent = true;
        File[] allCommits = commits.listFiles();
        for (File file : allCommits) {
            try {
                currentCommit = (Commit) deserialize(file);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (currentCommit.getMessage().equals(message)) {
                System.out.println(currentCommit.getMyID());
                nonExistent = false;
            }
        }
        if (nonExistent) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void branch(String name) throws IOException, ClassNotFoundException {
        String path = System.getProperty("user.dir");
        File branches = Utils.join(path, ".gitlet", "Branches");
        File[] allBranches = branches.listFiles();
        for (File file : allBranches) {
            if (file.getName().equals(name)) {
                System.out.println("A branch with that name already exists.");
                return;
            }
        }
        Branch other = new Branch(name);
        File inFile = Utils.join(path, ".gitlet", "Heads", "master");
        Head currhead = (Head) deserialize(inFile);
        other.setHeadCommit(currhead.getCurrHead().getHeadCommit());
        File storedBranch = Utils.join(branches, other.getName());
        byte[] serializedBranch = serialize(other);
        Utils.writeContents(storedBranch, serializedBranch);

    }

    public static void removeBranch(String name) throws IOException, ClassNotFoundException {
        String path = System.getProperty("user.dir");
        File inFile = Utils.join(path, ".gitlet", "Heads", "master");
        Head currhead = (Head) deserialize(inFile);
        boolean nBranch = true;
        if (currhead.getCurrHead().getName().equals(name)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        File branches = Utils.join(path, ".gitlet", "Branches");
        File[] allBranches = branches.listFiles();
        for (File file : allBranches) {
            if (file.getName().equals(name)) {
                file.delete();
                nBranch = false;
            }
        }

        if (nBranch) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

    }

    /**
     * Takes the version of the file as it exists in the head commit,
     * the front of the current branch, and puts it in the working directory,
     * overwriting the version of the file that’s already there if there is one. --taken from spec
     */


    public static void reset(String commitID) throws IOException, ClassNotFoundException {
        String path = System.getProperty("user.dir");
        File currDir = Utils.join(path);
        File inFile = Utils.join(path, ".gitlet", "Heads", "master");
        Head currHead = (Head) deserialize(inFile);
        Branch currBranch = currHead.getCurrHead();
        File commits = Utils.join(path, ".gitlet", "Commits");
        File blobs = Utils.join(".gitlet", "Blobs");
        File[] matchingFiles = commits.listFiles((dir, name) -> name.startsWith(commitID));
        if (matchingFiles.length == 0) {
            System.out.println("No commit with that id exists.");
            return;
        }
        File correct = matchingFiles[0];
        Commit headCommit = (Commit) deserialize(Utils.join(commits,
            currHead.getCurrHead().getHeadCommit()));
        Commit currCommit = (Commit) deserialize(correct);
        currBranch.setHeadCommit(currCommit.getMyID());
        currHead.setHeadBranch(currBranch);
        TreeMap<String, String> headBlobMap = headCommit.getContents();
        TreeMap<String, String> blobMap = currCommit.getContents();
        File stagingArea = Utils.join(path, ".gitlet", "Staging Area");
        for (File f : currDir.listFiles()) {
            if (!(f.isDirectory())) {
                if (!(headBlobMap.containsKey(f.getName())) && blobMap.containsKey(f.getName())) {
                    String sha = blobMap.get(f.getName());
                    File blobPath = Utils.join(blobs, sha);
                    byte[] old = Utils.readContents(blobPath);
                    byte[] now = Utils.readContents(f);
                    if (!(Arrays.equals(old, now))) {
                        System.out.println("There is an untracked file in the way; "
                            +
                            "delete it or add it first.");
                        return;
                    }
                }
            }
        }
        for (File f : currDir.listFiles()) {
            if (!(blobMap.containsKey(f.getName())) && headBlobMap.containsKey(f.getName())) {
                f.delete(); //Removes files that are not being tracked
            }
        }
        for (String filename : blobMap.keySet()) {
            String sha1 = blobMap.get(filename);
            File currBlob = Utils.join(blobs, sha1);
            byte[] blobContents = Utils.readContents(currBlob);
            File updated = Utils.join(path, filename);
            Utils.writeContents(updated, blobContents);
        }

        for (File f : stagingArea.listFiles()) {
            f.delete();
        }

        //Delete everything being tracked and then just copy over
        File branchPath = Utils.join(path, ".gitlet", "Branches");
        File storedBranch = Utils.join(branchPath, currBranch.getName());
        storedBranch.delete();
        byte[] serializedBranch = serialize(currBranch);
        Utils.writeContents(storedBranch, serializedBranch);

        File headsRoll = Utils.join(path, ".gitlet", "Heads");
        File storedHead = Utils.join(headsRoll, "master");
        storedHead.delete();
        byte[] serialHead = serialize(currHead);
        Utils.writeContents(storedHead, serialHead); //Branch gets serialized again; is that okay?

    }

    private static boolean checkUntracked(String path,
                                          TreeMap<String, String> currBranchHeadContents,
                                  TreeMap<String, String> givenBranchHeadContents) {
        File currDir = Utils.join(path);
        for (File f : currDir.listFiles()) {
            if (!(currBranchHeadContents.containsKey(f.getName()))
                    && givenBranchHeadContents.containsKey(f.getName())) {
                File blobs = Utils.join(path, ".gitlet", "Blobs");
                String givenSha = givenBranchHeadContents.get(f.getName());
                File overwrite = Utils.join(blobs, givenSha);
                byte[] overwritten = Utils.readContents(overwrite);
                byte[] original = Utils.readContents(f);
                if (!(Arrays.equals(overwritten, original))) {
                    System.out.println("There is an untracked file in the way; "
                        +
                        "delete it or add it first.");
                    return true;
                }
            }
        }
        return false;
    }

    public static void merge(String given) throws Exception {
        String path = System.getProperty("user.dir");
        File currDir = Utils.join(path);
        File branchDir = Utils.join(path, ".gitlet", "Branches");
        File inFile = Utils.join(path, ".gitlet", "Heads", "master");
        File stagingArea = Utils.join(path, ".gitlet", "Staging Area");
        File removals = Utils.join(path, ".gitlet", "Removals");
        if (stagingArea.listFiles().length != 0 || removals.listFiles().length != 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        Head currHead = (Head) deserialize(inFile);
        Branch currBranch = currHead.getCurrHead();
        File branchFile = Utils.join(branchDir, given);
        if (failCheck(branchFile, currBranch)) {
            return;
        }
        Branch givenBranch = (Branch) deserialize(branchFile);
        String splitPointID = findSplitPoint(currBranch, givenBranch);
        File commits = Utils.join(path, ".gitlet", "Commits");
        File splitPointFile = Utils.join(commits, splitPointID);
        File currBranchHeadFile = Utils.join(commits, currBranch.getHeadCommit());
        File givenBranchHeadFile = Utils.join(commits, givenBranch.getHeadCommit());
        Commit splitPoint = (Commit) deserialize(splitPointFile);
        Commit currBranchHead = (Commit) deserialize(currBranchHeadFile);
        Commit givenBranchHead = (Commit) deserialize(givenBranchHeadFile);
        TreeMap<String, String> splitPointContents = splitPoint.getContents();
        TreeMap<String, String> currBranchHeadContents = currBranchHead.getContents();
        TreeMap<String, String> givenBranchHeadContents = givenBranchHead.getContents();
        if (checkUntracked(path, currBranchHeadContents, givenBranchHeadContents)) {
            return;
        }
        if (splitPointID.equals(givenBranch.getHeadCommit())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (checkFastForward(splitPointID, currBranch, givenBranch, path)) {
            return;
        }
        boolean conflict = false;
        ArrayList<String> conflictedFiles = new ArrayList<>();
        conflict = checkSplitPoint(splitPointContents, currBranchHeadContents,
                givenBranchHeadContents, givenBranchHead, conflictedFiles);
        //Bullet Point 4
        for (String filename : givenBranchHeadContents.keySet()) {
            if (!(splitPointContents.containsKey(filename))) {
                if (!(currBranchHeadContents.containsKey(filename))) {
                    CommandHandler.checkoutCommitID(givenBranchHead.getMyID(), filename);
                    CommandHandler.add(new String[]{null, filename});
                } else if (!(currBranchHeadContents.get(filename).equals
                        (givenBranchHeadContents.get(filename)))) {
                    conflict = true;
                    conflictedFiles.add(filename);
                }
            }
        }
        for (String filename : currBranchHeadContents.keySet()) {
            if (!(splitPointContents.containsKey(filename))) {
                if (!(givenBranchHeadContents.containsKey(filename))) {
                    continue;
                } else if (!(givenBranchHeadContents.get(filename).equals
                        (currBranchHeadContents.get(filename)))) {
                    conflict = true;
                    conflictedFiles.add(filename);
                }
            }
        }
        /** @source
         * http://www.avajava.com/tutorials/lessons/how-do-i-write-a-string-to-a-file.html*/
        if (conflict) {
            File blobs = Utils.join(path, ".gitlet", "Blobs");
            handleConflict(conflictedFiles, currBranchHeadContents,
                    givenBranchHeadContents, currDir, blobs);
            System.out.println("Encountered a merge conflict.");
            return;
        }
        String message = "Merged "
                + currBranch.getName() + " with " + givenBranch.getName() + ".";
        CommandHandler.commit(message);
    }

    private static boolean checkSplitPoint(TreeMap<String, String> splitPointContents,
                                        TreeMap<String, String> currBranchHeadContents,
                                        TreeMap<String, String> givenBranchHeadContents,
                                        Commit givenBranchHead, ArrayList<String> conflictedFiles) {
        boolean conflict = false;
        for (String filename : splitPointContents.keySet()) {
            //Bullet Point 1
            if (splitPointContents.containsKey(filename)
                    && givenBranchHeadContents.containsKey(filename)
                    && currBranchHeadContents.containsKey(filename)) {
                if (!(splitPointContents.get(filename).equals
                        (givenBranchHeadContents.get(filename)))) {
                    if (splitPointContents.get(filename).equals
                            (currBranchHeadContents.get(filename))) {
                        CommandHandler.checkoutCommitID(givenBranchHead.getMyID(), filename);
                        try {
                            CommandHandler.add(new String[]{null, filename});
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else if (!(currBranchHeadContents.get(filename).equals
                            (givenBranchHeadContents.get(filename)))) {
                        conflict = true;
                        conflictedFiles.add(filename);
                    }
                } else if (!(splitPointContents.get(filename).equals
                        (currBranchHeadContents.get(filename)))) {
                    if (splitPointContents.get(filename).equals
                            (givenBranchHeadContents.get(filename))) {
                        continue;
                    } else if (!(currBranchHeadContents.get(filename).equals
                            (givenBranchHeadContents.get(filename)))) {
                        conflict = true;
                        conflictedFiles.add(filename);
                    }

                }
                //Bullet Point 5
            } else if (splitPointContents.get(filename).equals
                    (currBranchHeadContents.get(filename))) {
                if (!(givenBranchHeadContents.containsKey(filename))) {
                    try {
                        CommandHandler.remove(filename);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                //Bullet Point 6
            } else if (splitPointContents.get(filename).equals
                    (givenBranchHeadContents.get(filename))) {
                if (!(currBranchHeadContents.containsKey(filename))) {
                    continue;
                }
            } else if (!(splitPointContents.get(filename).equals
                    (currBranchHeadContents.get(filename)))) {
                if (!(givenBranchHeadContents.containsKey(filename))
                        && currBranchHeadContents.containsKey(filename)) {
                    conflict = true;
                    conflictedFiles.add(filename);
                }
            } else if (!(splitPointContents.get(filename).equals
                    (givenBranchHeadContents.get(filename)))) {
                if (!(currBranchHeadContents.containsKey(filename))
                        && givenBranchHeadContents.containsKey(filename)) {
                    conflict = true;
                    conflictedFiles.add(filename);
                }
            }
        }
        return conflict;
    }

    private static void handleConflict(ArrayList<String> conflictedFiles,
                                       TreeMap<String, String> currBranchHeadContents,
                                       TreeMap<String, String> givenBranchHeadContents,
                                       File currDir, File blobs) {
        for (String file : conflictedFiles) {
            if (currBranchHeadContents.containsKey(file)
                    && givenBranchHeadContents.containsKey(file)) {
                File currBlobPath =
                        Utils.join(blobs, currBranchHeadContents.get(file));
                File givenBlobPath =
                        Utils.join(blobs, givenBranchHeadContents.get(file));
                String currContents =
                    usingBufferedReader(currBlobPath.getPath());
                String givenContents = usingBufferedReader(givenBlobPath.getPath());
                String merged =
                    "<<<<<<< HEAD\n" + currContents + "=======\n" + givenContents + ">>>>>>>\n";
                File mergedFile = Utils.join(currDir, file);
                try {
                    FileWriter fileWriter = new FileWriter(mergedFile);
                    fileWriter.write(merged);
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (currBranchHeadContents.containsKey(file)) {
                File currBlobPath = Utils.join(blobs, currBranchHeadContents.get(file));
                String currContents = usingBufferedReader(currBlobPath.getPath());
                String merged = "<<<<<<< HEAD\n" + currContents + "=======\n" +  ">>>>>>>\n";
                File mergedFile = Utils.join(currDir, file);
                try {
                    FileWriter fileWriter = new FileWriter(mergedFile);
                    fileWriter.write(merged);
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (givenBranchHeadContents.containsKey(file)) {
                File givenBlobPath =
                        Utils.join(blobs, givenBranchHeadContents.get(file));
                String givenContents =
                        usingBufferedReader(givenBlobPath.getPath());
                String merged = "<<<<<<< HEAD\n"  + "=======\n" + givenContents + ">>>>>>>\n";
                File mergedFile = Utils.join(currDir, file);
                try {
                    FileWriter fileWriter = new FileWriter(mergedFile);
                    fileWriter.write(merged);
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean checkFastForward(String splitPointID,
                                            Branch currBranch, Branch givenBranch, String path) {
        if (splitPointID.equals(currBranch.getHeadCommit())) {
            currBranch.setHeadCommit(givenBranch.getHeadCommit());
            File branchPath = Utils.join(path, ".gitlet", "Branches");
            File storedBranch = Utils.join(branchPath, currBranch.getName());
            byte[] serializedBranch = serialize(currBranch);
            Utils.writeContents(storedBranch, serializedBranch);
            System.out.println("Current branch fast-forwarded.");
            return true;
        }
        return false;
    }

    private static boolean failCheck(File branchFile, Branch currBranch)
            throws IOException, ClassNotFoundException {
        if (!(branchFile.exists())) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        Branch givenBranch = (Branch) deserialize(branchFile);
        if (currBranch.getName().equals(givenBranch.getName())) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        return false;
    }

    /**
     * @source https://howtodoinjava.com/java/io/java-read-file-to-string-examples/
     */
    private static String usingBufferedReader(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    private static String findSplitPoint(Branch master, Branch other)
            throws IOException, ClassNotFoundException, ParseException {
        String path = System.getProperty("user.dir");
        File commits = Utils.join(path, ".gitlet", "Commits");
        String masterCommitID = master.getHeadCommit();
        String otherCommitID = other.getHeadCommit();
        File masterCommitFile = Utils.join(commits, masterCommitID);
        File otherCommitFile = Utils.join(commits, otherCommitID);
        Commit masterCommit = (Commit) deserialize(masterCommitFile);
        Commit otherCommit = (Commit) deserialize(otherCommitFile);
        /**Remember to come back and do your checks for if they are already the same*/
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date masterDate = format.parse(masterCommit.getCommitDateTime());
        Date otherDate = format.parse(otherCommit.getCommitDateTime());
        Commit currentTraceBack;
        if (masterDate.after(otherDate)) {
            currentTraceBack = masterCommit;
        } else {
            currentTraceBack = otherCommit;
        }
        while (!(masterCommit.getMyID().equals(otherCommit.getMyID()))) {
            if (currentTraceBack.getMyID().equals(masterCommit.getMyID())) {
                String update = masterCommit.getParent();
                File updateCommit = Utils.join(commits, update);
                masterCommit = (Commit) deserialize(updateCommit);
                masterDate = format.parse(masterCommit.getCommitDateTime());
            } else {
                String update = otherCommit.getParent();
                File updateCommit = Utils.join(commits, update);
                otherCommit = (Commit) deserialize(updateCommit);
                otherDate = format.parse(otherCommit.getCommitDateTime());
            }

            if (masterDate.after(otherDate)) {
                currentTraceBack = masterCommit;
            } else {
                currentTraceBack = otherCommit;
            }
        }

        return masterCommit.getMyID();

    }

    public static void checkoutFile(String filename) { //First of the 3 checkout features
        String path = System.getProperty("user.dir");
        File address = Utils.join(path, ".gitlet", "Heads", "master");
        Head head = null;
        try {
            head = (Head) deserialize(address);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        Branch currBranch = head.getCurrHead();
        String commitID = currBranch.getHeadCommit();
        File comAdd = Utils.join(path, ".gitlet", "Commits", commitID);
        Commit currComm = null;
        try {
            currComm = (Commit) deserialize(comAdd);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        TreeMap<String, String> commitMap = currComm.getContents();
        for (String file : commitMap.keySet()) {
            if (file.equals(filename)) {
                File item = new File(file);
                Utils.restrictedDelete(item);
                File blob = Utils.join(path, ".gitlet", "Blobs", commitMap.get(filename));
                byte[] contents = Utils.readContents(blob);
                Utils.writeContents(Utils.join(path, filename), contents);
                return;
            }
        }
        System.out.println("File does not exist in that commit.");
    }

    public static void checkoutCommitID(String commitID, String filename) {
        String path = System.getProperty("user.dir");
        File commits = Utils.join(path, ".gitlet", "Commits");
        File[] matchingFiles = commits.listFiles((dir, name) -> name.startsWith(commitID));
        if (matchingFiles.length == 0) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit currComm = null;
        try {
            currComm = (Commit) deserialize(matchingFiles[0]);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        TreeMap<String, String> commitMap = currComm.getContents();
        for (String file : commitMap.keySet()) {
            if (file.equals(filename)) {
                File item = new File(file);
                Utils.restrictedDelete(item);
                File blob = Utils.join(path, ".gitlet", "Blobs", commitMap.get(filename));
                byte[] contents = Utils.readContents(blob);
                Utils.writeContents(Utils.join(path, filename), contents);
                return;
            }
        }
        System.out.println("File does not exist in that commit.");
    }

    public static void checkoutBranch(String branch) throws IOException, ClassNotFoundException {
        String path = System.getProperty("user.dir");
        File pathF = new File(path);
        File address = Utils.join(path, ".gitlet", "Branches", branch);
        if (!address.exists()) {
            System.out.println("No such branch exists."); return;
        }
        File heAddress = Utils.join(path, ".gitlet", "Heads", "master");
        Head head = null;
        head = (Head) deserialize(heAddress);
        if (head.getCurrHead().getName().equals(branch)) {
            System.out.println("No need to checkout the current branch."); return;
        }
        Branch relevant = null;
        relevant = (Branch) deserialize(address);
        String headCommit = relevant.getHeadCommit();
        File commitAddress = Utils.join(path, ".gitlet", "Commits", headCommit);
        Commit headCom = null;
        headCom = (Commit) deserialize(commitAddress);
        Branch currBranch = head.getCurrHead();
        String mostRecentCommitID = currBranch.getHeadCommit();
        File mostRecentCommitPath = Utils.join(path, ".gitlet", "Commits", mostRecentCommitID);
        Commit mostRecentCommit = null;
        mostRecentCommit = (Commit) deserialize(mostRecentCommitPath);
        TreeMap<String, String> commitMap = headCom.getContents();
        TreeMap<String, String> mostRecentCommitMap = mostRecentCommit.getContents();
        for (File f : pathF.listFiles()) {
            if (!(f.isDirectory())) {
                if (!(mostRecentCommitMap.containsKey(f.getName()))
                    && commitMap.containsKey(f.getName())) {
                    String sha = commitMap.get(f.getName());
                    File blobPath = Utils.join(path, ".gitlet", "Blobs", sha);
                    byte[] old = Utils.readContents(blobPath);
                    byte[] now = Utils.readContents(f);
                    if (!(Arrays.equals(old, now))) {
                        System.out.println(
                            "There is an untracked file in the way; " + "delete it or add it first.");
                        return;
                    }
                }
            }
        }
        for (File f : pathF.listFiles()) {
            if ((mostRecentCommitMap.containsKey(f.getName()))
                && !(commitMap.containsKey(f.getName()))) {
                Utils.restrictedDelete(f);
            }
        }
        for (String filename : commitMap.keySet()) {
            File item = new File(filename);
            Utils.restrictedDelete(item);
            File blob = Utils.join(path,
                ".gitlet", "Blobs", commitMap.get(filename));
            byte[] contents = Utils.readContents(blob);
            Utils.writeContents(Utils.join(path, filename), contents);
        }
        File stagingArea = Utils.join(path,
            ".gitlet", "Staging Area");
        for (File f : stagingArea.listFiles()) {
            f.delete();
        }
        head.setHeadBranch(relevant);
        File storedBranch = address;
        byte[] serializedBranch = serialize(relevant);
        Utils.writeContents(storedBranch, serializedBranch);
        File headsRoll = Utils.join(path, ".gitlet", "Heads");
        File storedHead = Utils.join(headsRoll, "master");
        byte[] serialHead = serialize(head);
        Utils.writeContents(storedHead, serialHead);
    }


    private static byte[] serialize(Object obj) {
        byte[] serializeObj = null;
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.close();
            serializeObj = stream.toByteArray();
        } catch (IOException excp) { //excp is an Error object you can use
            excp.getMessage();
        }
        return serializeObj;

    }

    public static Object deserialize(File inFile) throws IOException, ClassNotFoundException {
        Object result;
        try {
            ObjectInputStream inp = new ObjectInputStream(new FileInputStream(inFile));
            result = inp.readObject();
            inp.close();
        } catch (IOException | ClassNotFoundException excp) {
            excp.getMessage();
            result = null;
        }
        return result;

    }

    public static void main(String[] args) {
        String currDirectory = System.getProperty("user.dir");
        File dir = Utils.join(currDirectory, ".gitlet", "Staging Area", "test.txt");
        System.out.println(dir.getName());
    }


}
