/***
 * @author Jack Shendrikov
 *
 * Round Robin specialization class that inherits from abstract Scheduling Algorithm
 */

class RR extends SchedulingAlgorithm {
    RR() {
        this.setSchedulerType(SchedulerType.RR);
        myQueue = ProcessReadyQueue.createSingleProcessReadyQueueInstance(SchedulerType.RR.getSchedulerType());
    }
}
