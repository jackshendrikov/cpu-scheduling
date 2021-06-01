/***
 * @author Jack Shendrikov
 *
 */

enum SchedulerType {
    PSJF(1), RR(2);

    private final int schedulerType;

    SchedulerType(int schedulerType) {
        this.schedulerType = schedulerType;
    }

    public int getSchedulerType() {
        return schedulerType;
    }
}

/**
 * @author Jack Shendrikov
 * Main class definition for what constitutes a scheduling algorithm.
 * Abstract definition of both properties and behavior that all schedulers share.
 */
public abstract class SchedulingAlgorithm implements PerformanceMetrics {

    private SchedulerType schedulerType;
    ProcessReadyQueue myQueue;

    static double runningTurnaroundSum = 0;
    static double runningBurstTimeSum = 0;
    static double runningWaitTimeSum = 0;

    // default constructor to be overwritten by specialization classes PSJF, RR
    SchedulingAlgorithm() {}

    // implement methods from interface as required
    @Override
    public double avgTurnaroundTime(double totalSimTime) {
      return runningTurnaroundSum / 10000;
    }
    @Override
    public double throughput(double totalSimTime) {
      return 10000 / totalSimTime;
    }
    @Override
    public double cpuUtilization(double totalSimTime) {
      return runningBurstTimeSum / totalSimTime;
    }
    @Override
    public double avgProcessesInReadyQueue(int lambda) {
      return lambda * (runningWaitTimeSum / 10000);
    }
    @Override
    public double avgWaitingTime(double totalSimTime) {
        return runningWaitTimeSum / 10000;
    }


    SchedulerType getSchedulerType() {
        return schedulerType;
    }

    void setSchedulerType(SchedulerType schedulerType) {
        this.schedulerType = schedulerType;
    }

    Process getNextProcessForCPU() {
        return myQueue.returnAndRemoveHeadProcess();
    }

    Process safelyPeekAtNextProcess() { return myQueue.peek(); }

    void addProcessToReadyQueue(Process p) {
        myQueue.insertProcess(p);
    }

}
