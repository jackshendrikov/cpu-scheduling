import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

/***
 * @author Jack Shendrikov
 *
 * This class behaves as `static` - declared `final` to prevent extension of the class.
 * Private constructor prevents instantiation by client code as we do not want to instantiate.
 * Make all the members and functions of the class static - since the class cannot be instantiated,
 * so no instance methods can be called or instance fields accessed.
 *
 * The process ready queue is implemented in different way depending on the scheduler.
 *      1) If PSJF - use remaining time to sort queue
 *      2) If RR - starts off with arrival time but an early arriving process with a long burst time may end up at the back
 *         of the line, so need a simple list to maintain order. A pre-empted process will go to the tail. After quantum
 *         expires, next process is obtained from the head.
 */

final class ProcessReadyQueue {

    // serves as single instance of PRQ to be used throughout simulation
    private static ProcessReadyQueue obj = null;

    private static Queue<Process> schedulerPriorityQueue;

    // private Constructor will prevent the instantiation of this class directly
    private ProcessReadyQueue(int schedulerType) {
        createProcessReadyQueue(schedulerType);
    }

    /**
     * @return the instantiated object of our process ready queue, the only one permitted
     */
    static ProcessReadyQueue createSingleProcessReadyQueueInstance(int schedulerType) {

        // this logic will ensure that no more than one object can be created at a time
        if (obj == null) {
            obj = new ProcessReadyQueue(schedulerType);
        }
        return obj;
    }

    /**
     * @param schedulerType
     * depending on the argument passed to the constructor, create a Process Ready Queue for the scheduler
     * Each has it's own comparator:
     *      - PSJF -> uses remainingBurst times.
     *      - RR -> queue is a simple list where we insert at the tail and retrieve from the head
     */
    private void createProcessReadyQueue(int schedulerType) {
        // PSJF
        if (schedulerType == 1) {
            ProcessRemainingTimeComparator PSJFComparator = new ProcessRemainingTimeComparator();
            schedulerPriorityQueue = new PriorityQueue<>(10, PSJFComparator);
        }

        // RR
        else if (schedulerType == 2) {
            schedulerPriorityQueue = new LinkedList<>();
        }
    }

    void insertProcess(Process p) {
        // adds to queue or if List, to end of list
        schedulerPriorityQueue.add(p);
    }

    Process returnAndRemoveHeadProcess() {
        // retrieve and remove head
        return schedulerPriorityQueue.poll();
    }

    boolean isEmpty() {
        return schedulerPriorityQueue.isEmpty();
    }

    Process peek() {
        return schedulerPriorityQueue.peek();
    }

    /**
     * This method is optional and allows the simulator to produce a "non-flat" curve.
     */
    void iterateAndGetRemainingDifferenceForPSJF(double finalTime) {
        for(Process p : schedulerPriorityQueue) {
            Simulator.numProcessesHandled++;
            p.setCompletionTime(finalTime);
            double completionMinusStart = p.getCompletionTime() - p.getStartTime();
            p.setTurnaroundTime(p.getCompletionTime() - p.getArrivalTime());
            p.setWaitingTime((p.getStartTime() - p.getArrivalTime())
                    + (completionMinusStart - p.getBurstTime()));
            if (p.isReturning()) {
                SchedulingAlgorithm.runningBurstTimeSum += p.getBurstTime();
            }
            SchedulingAlgorithm.runningTurnaroundSum += p.getTurnaroundTime();
            SchedulingAlgorithm.runningWaitTimeSum += p.getWaitingTime();
        }
    }

    /**
     * Unlike the preceeding method for PSJF, this method is necessary for proper calculation of RR CPU Utilization.
     * The reason is - after lambda = 16.667 (approximation from curve, also via 1/0.06 = 16.667), the ready queue
     * gets backed up from too many arriving processes. Round Robin begins to degenerate and provides quantums of service
     * to some processes that never get a chance to complete. But the amount of CPU service they receive needs to be accounted.
     * Here, we simply add it back in to the numerator to obtain the correct result.
     */
    void iterateAndGetRemainingDifferenceForRR() {
        double workPerformed;
        for(Process p : schedulerPriorityQueue) {
            if (p.isReturning()) {
                workPerformed = p.getBurstTime() - p.getRemainingCpuTime();
                SchedulingAlgorithm.runningBurstTimeSum += workPerformed;
            }
        }
    }
}
