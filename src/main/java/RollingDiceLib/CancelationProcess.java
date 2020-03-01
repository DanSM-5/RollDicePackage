package RollingDiceLib;

public class CancelationProcess {
    public boolean isCancellationRequested = false;

    public void cancel(){
        setCancellationRequested(true);
    }

    public boolean isCancellationRequested() {
        return isCancellationRequested;
    }

    public void setCancellationRequested(boolean cancellationRequested) {
        isCancellationRequested = cancellationRequested;
    }
}
