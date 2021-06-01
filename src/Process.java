/***
 * @author Jack Shendrikov
 *
 * Each process maintains certain times that aid in both the processing of the process and in the computation of
 * overall simulation statistics
 */

class Process {
    private double arrivalTime;      // same as the `Event` ProcessArrival eventTime.
    private double burstTime;        // obtained by passing 1/avgServiceTime as the lambda in genexp(lambda)
    private double completionTime;   //
    private double waitingTime;      //
    private double turnaroundTime;   // = completionTime - startTime
    private double startTime;        // = clock when given to CPU
    private double remainingCpuTime; // initialized to burst time and is then used to track the process's progress on the CPU and whether we may consider it complete or not.
    private boolean isReturning;     //
    private double restartTime;      //

    Process() {
        this.isReturning = false;
    }


    /* Getters and Setters */
    double getArrivalTime() {
        return arrivalTime;
    }
    void setArrivalTime(double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    double getBurstTime() {
        return burstTime;
    }
    void setBurstTime(double burstTime) {
        this.burstTime = burstTime;
    }

    double getCompletionTime() {
        return completionTime;
    }
    void setCompletionTime(double completionTime) {
        this.completionTime = completionTime;
    }

    double getWaitingTime() {
        return waitingTime;
    }
    void setWaitingTime(double waitingTime) {
        this.waitingTime = waitingTime;
    }

    double getTurnaroundTime() {
        return turnaroundTime;
    }
    void setTurnaroundTime(double turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }

    double getStartTime() {
        return startTime;
    }
    void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    double getRemainingCpuTime() {
        return remainingCpuTime;
    }
    void setRemainingCpuTime(double remainingCpuTime) {
        this.remainingCpuTime = remainingCpuTime;
    }

    boolean isReturning() {
        return isReturning;
    }
    void setIsReturning(boolean returning) {
        this.isReturning = returning;
    }

    double getRestartTime() {
        return restartTime;
    }
    void setRestartTime(double restartTime) {
        this.restartTime = restartTime;
    }
}
