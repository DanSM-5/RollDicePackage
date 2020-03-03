package RollingDemo;

import RollingDiceLib.CancelationProcess;
import RollingDiceLib.RollingProcess;

public class Main {
    public static int previousReport = 0;
    public static void main(String[] args) {
        System.out.println("starting...");
        RollingProcess process = new RollingProcess(6,8000000);

        //process.rollDiceSyncBlocking(Main::report,Main::done, new CancelationProcess());
        //process.rollDiceAsyncNonBlocking(Main::report, Main::done, new CancelationProcess());
        process.rollDiceAsyncParallel(Main::report,Main::done,new CancelationProcess());
    }

    public static void report(int progress, int percentage){
        if(previousReport < percentage) {
            if (percentage % 5 == 0) {
                previousReport = percentage;
                System.out.println(String.format("%d counted that represents %d%%", progress, percentage));
            }
        }
    }
    public static void done(int diceNumber, int result){
        System.out.println(String.format("Dice %d has a value of %d", diceNumber+1, result));
    }
}
