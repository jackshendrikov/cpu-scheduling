/***
 * @author Jack Shendrikov
 *
 * CPU receives a process and:
 *      - sets the start time if this is the first time the process has been serviced;
 *      - setting process' boolean flag `isReturning` from false to true, so the next time it would be ID'd as a returning process.
 *      - if CPU is working on a process, it sets its boolean flag `isBusy` to true.
 */

public class CPU {

    private boolean isBusy;
    private Process myProcess;

    CPU() {
        isBusy = false;
    }

    boolean isBusy() {
        return isBusy;
    }

    void setBusy(boolean busy) {
        isBusy = busy;
    }

    public void tick() {
        this.myProcess.setRemainingCpuTime(this.myProcess.getRemainingCpuTime() - 0.01f);
    }

    Process getMyProcess() {
        return myProcess;
    }

    void setMyProcess(Process myProcess) {
        this.myProcess = myProcess;
    }
}
