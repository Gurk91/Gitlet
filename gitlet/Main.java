package gitlet;

/* Driver class for Gitlet, the tiny stupid version-control system.
   @author
*/
public class Main {

    /* Usage: java gitlet.Main ARGS, where ARGS contains
       <COMMAND> <OPERAND> .... */
    public static void main(String[] args) throws Exception {
        if (args[0].equals("init")) {
            CommandHandler.init();
        } else if (args[0].equals("add")) {
            CommandHandler.add(args);
        } else if (args[0].equals("commit")) {
            if (args.length < 2 || args[1].equals("")) {
                System.out.println("Please enter a commit message.");
                return;
            }
            CommandHandler.commit(args[1]);
        } else if (args[0].equals("rm")) {
            CommandHandler.remove(args[1]);
        } else if (args[0].equals("status")) {
            CommandHandler.status();
        } else if (args[0].equals("log")) {
            CommandHandler.log();
        } else if (args[0].equals("global-log")) {
            CommandHandler.globalLog();
        } else if (args[0].equals("find")) {
            CommandHandler.find(args[1]);
        } else if (args[0].equals("branch")) {
            CommandHandler.branch(args[1]);
        } else if (args[0].equals("rm-branch")) {
            CommandHandler.removeBranch(args[1]);
        } else if (args[0].equals("reset")) {
            CommandHandler.reset(args[1]);
        } else if (args[0].equals("merge")) {
            CommandHandler.merge(args[1]);
        } else if (args[0].equals("checkout")) {
            if (args[1].equals(("--"))) {
                CommandHandler.checkoutFile(args[2]);
            } else if (args.length < 3) {
                CommandHandler.checkoutBranch(args[1]);
            } else if (args[2].equals("--")) {
                CommandHandler.checkoutCommitID(args[1], args[3]);
            } else {
                System.out.println("Incorrect Operands.");
                return;
            }
        } else {
            System.out.println("Incorrect Operands.");
            return;
        }
    }

}
