class PSJF extends SchedulingAlgorithm {
    PSJF() {
        this.setSchedulerType(SchedulerType.PSJF);
        myQueue = ProcessReadyQueue.createSingleProcessReadyQueueInstance(SchedulerType.PSJF.getSchedulerType());
    }
}
