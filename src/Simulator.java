import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;

import static java.lang.Math.log;


/***
 * @author Jack Shendrikov
 *
 * This class drives the simulation of a single instance of a scheduler as specified by the user through
 * command line arguments. It contains a main while-loop that continues processing events until 10,000 processes complete.
 * In so doing, it does not stop or prevent the generation of new process arrivals - this is necessary for an accurate
 * simulation with valid statistical results.
 *
 * We have three main types of events - `ProcessArrival`, `ProcessCompletion`, and `TimeSliceOccurrence`. The latter is only
 * used by the Round Robin scheduler. I decided against having a fourth, separate event for a PSJF preemption.
 * As this occurs at the simulation time when it is detected, I handle it then and there.
 *
 * Every time we schedule a process on the CPU, we check to see if it is new or returning (already started previously).
 * We can then check if the process will complete or if it requires special treatment - preemption for PSJF or interrupt for RR.
***/

public class Simulator {

    static int numProcessesHandled = 0;
    private static boolean togglePSJFCurve = false;

    public static void main(String[] args) throws IOException {

        if (args.length == 0 || args[0].toLowerCase().equals("help") || args.length < 4) {
            printProgramInstructions();
        } else {

            /*
                If user provides an optional 5th parameter, we can toggle the shape of certain PSJF curves for a different
                interpretation as needed.
             */
            if (args.length == 5) {
                if (args[4].equals("true") || args[4].equals("false")) {
                    togglePSJFCurve = Boolean.parseBoolean(args[4]);
                    System.out.println(togglePSJFCurve);
                }

            }

            // initialize system state variables
            final int algorithmType = Integer.parseInt(args[0]);
            final int lambda = Integer.parseInt(args[1]);  // average rate of arrival
            final double avgServiceTime = Double.parseDouble(args[2]);
            final double quantumForRR = Double.parseDouble(args[3]);

            // initialize simulation clock to 0
            Clock simulationClock = new Clock();
            simulationClock.setSimulationTime(0f);

            EventQueue eventQueue = new EventQueue();

            Event initialEvent = new Event(EventType.ProcessArrival, 0);
            eventQueue.insertEvent(initialEvent);

            // create the scheduling algorithm and the CPU to handle processes
            SchedulingAlgorithm schedulingAlgorithm = createSchedulingAlgorithm(algorithmType);
            CPU simulationCPU = new CPU();

            /*
             * I experimented with generating all 10k processes up-front but this caused issues in the distribution
             * and calculated statistics values. Generating new arrivals as we go is a preferred approach that produces
             * accurate results.
             */

            // while we have not processed N Processes to completion,
            // keep going and handle events in the `EventQueue` as needed
            while (numProcessesHandled < 10000) {
                // Set `Clock` to EventTime
                simulationClock.setSimulationTime(eventQueue.getSystemTimeFromHead());

                // Do/process next event and remove from `EventQueue`
                Event eventToProcess = eventQueue.returnAndRemoveHeadEvent();
                EventType eventToProcessType = eventToProcess.getEventType();

                /* If event is:
                *   1) an arrival: create a process and add it to the scheduler's queue
                *   2) a completion: update the intermediate numbers needed for statistics have scheduler start
                *      executing next process in ReadyQueue if available and schedule completion event in the future if
                *      RR because we know the completion times. RR is start time + quantum.
                */
                if (eventToProcessType == EventType.ProcessArrival) {
                    // routine to unconditionally create new arrival event
                    unconditionallyCreateNewArrival(lambda, simulationClock, eventQueue);

                    // create the "arriving" process
                    Process p = new Process();
                    p.setArrivalTime(simulationClock.getSimulationTime());  // processArrivalTime = eventTime
                    p.setBurstTime(genexp(1/avgServiceTime));
                    p.setRemainingCpuTime(p.getBurstTime());

                    // add new process to scheduler's ready queue unconditionally
                    // only always use a process from the queue, not p directly
                    Objects.requireNonNull(schedulingAlgorithm).addProcessToReadyQueue(p);

                    if (algorithmType == SchedulerType.PSJF.getSchedulerType()) {
                        // CPU not busy, give it a process from queue, no preemption possible in this case but may have completion
                        if (!simulationCPU.isBusy()) {
                            simulationCPU.setMyProcess(schedulingAlgorithm.getNextProcessForCPU());
                            simulationCPU.setBusy(true);

                            checkIfReturningAndSetTimes(simulationClock, simulationCPU);

                            if (eventQueue.safelyPeekAtNextEvent().getEventType() == EventType.ProcessArrival) {
                                if ((simulationClock.getSimulationTime() + simulationCPU.getMyProcess().getRemainingCpuTime())
                                        <= eventQueue.safelyPeekAtNextEvent().getEventTime()) {
                                    Event knownCompletion = new Event(EventType.ProcessCompletion,
                                            simulationCPU.getMyProcess().getRestartTime() + simulationCPU.getMyProcess().getRemainingCpuTime());
                                    eventQueue.insertEvent(knownCompletion);
                                }
                            }
                        } // end CPU IDLE

                        //else CPU is busy and we may have to preempt if conditions are met
                        else {
                            // process ready queue sorted by remTime, not arrival, so we are not guaranteed sequential processes
                            // so, check system time for current time instead
                            double elapsedTime = simulationClock.getSimulationTime() - simulationCPU.getMyProcess().getRestartTime();
                            double oldRemTime = simulationCPU.getMyProcess().getRemainingCpuTime();
                            double newRemTime = oldRemTime - elapsedTime;

                            if (newRemTime <= 0) {
                                Event knownCompletion = new Event(EventType.ProcessCompletion,
                                        simulationClock.getSimulationTime() + oldRemTime);
                                eventQueue.insertEvent(knownCompletion);
                            }
                            else if (schedulingAlgorithm.safelyPeekAtNextProcess().getRemainingCpuTime() >= newRemTime) {
                                simulationCPU.getMyProcess().setRemainingCpuTime(newRemTime);
                                determineCompletion(simulationClock, eventQueue, simulationCPU);

                            }

                            // else head process has a shorter remTime and we need to PREEMPT
                            // no special event type because preemption happens at the current system time
                            else if (schedulingAlgorithm.safelyPeekAtNextProcess().getRemainingCpuTime() < newRemTime){
                                simulationCPU.getMyProcess().setRemainingCpuTime(newRemTime);
                                Process tempProcess = simulationCPU.getMyProcess();
                                simulationCPU.setMyProcess(schedulingAlgorithm.getNextProcessForCPU());
                                checkIfReturningAndSetTimes(simulationClock, simulationCPU);
                                schedulingAlgorithm.addProcessToReadyQueue(tempProcess);

                                //determine completion
                                determineCompletion(simulationClock, eventQueue, simulationCPU);
                            }
                        } // end CPU busy
                    } // end PSJF arrival handling

                    else if (algorithmType == SchedulerType.RR.getSchedulerType()) {
                        if (simulationCPU.isBusy()) {} else {
                            simulationCPU.setMyProcess(schedulingAlgorithm.getNextProcessForCPU());
                            simulationCPU.setBusy(true);
                            checkIfReturningAndSetTimes(simulationClock, simulationCPU);
                            determineCompletionOrQuantumInterrupt(quantumForRR, simulationClock, eventQueue, simulationCPU);
                        } // end else CPU is IDLE
                    } // end RR arrival handling
                } // end if to handle Process Arrivals

                else if (eventToProcessType == EventType.ProcessCompletion) {
                    /* When an event completes, set its remainingCpuTime to zero
                     * increment numProcessesHandled counter.
                     * Also the CPU is free to work on another process, so we must give it one
                     */
                    numProcessesHandled++;

                    if (numProcessesHandled == 10000) {
                        System.out.println("10000th process completing now");
                    }

                    if (algorithmType == SchedulerType.PSJF.getSchedulerType()) {
                        simulationCPU.getMyProcess().setRemainingCpuTime(0); // process is done
                        simulationCPU.getMyProcess().setCompletionTime(simulationClock.getSimulationTime());
                        simulationCPU.getMyProcess().setTurnaroundTime(simulationCPU.getMyProcess().getCompletionTime()
                                - simulationCPU.getMyProcess().getArrivalTime());
                        double completionMinusStart = simulationCPU.getMyProcess().getCompletionTime() - simulationCPU.getMyProcess().getStartTime();
                        simulationCPU.getMyProcess().setWaitingTime(
                                (simulationCPU.getMyProcess().getStartTime() - simulationCPU.getMyProcess().getArrivalTime()) +
                                (completionMinusStart - simulationCPU.getMyProcess().getBurstTime()));

                        // now that a process is complete, update runningSums that we will use to calculate statistics
                        SchedulingAlgorithm.runningBurstTimeSum += simulationCPU.getMyProcess().getBurstTime();
                        SchedulingAlgorithm.runningTurnaroundSum += simulationCPU.getMyProcess().getTurnaroundTime();
                        SchedulingAlgorithm.runningWaitTimeSum += simulationCPU.getMyProcess().getWaitingTime();

                        simulationCPU.setBusy(false);
                        if (!Objects.requireNonNull(schedulingAlgorithm).myQueue.isEmpty()) {
                            simulationCPU.setMyProcess(schedulingAlgorithm.getNextProcessForCPU());
                            simulationCPU.setBusy(true);
                            checkIfReturningAndSetTimes(simulationClock, simulationCPU);

                            //determine completion
                            Event nextEvent = eventQueue.safelyPeekAtNextEvent();
                            if (nextEvent.getEventType() == EventType.ProcessArrival) {
                                double nextArrival = nextEvent.getEventTime();
                                double elapsedTime = nextArrival - simulationCPU.getMyProcess().getRestartTime();
                                double oldRemTime = simulationCPU.getMyProcess().getRemainingCpuTime();
                                double newRemTime = oldRemTime - elapsedTime;

                                if (newRemTime <= 0) {
                                    Event knownCompletion = new Event(EventType.ProcessCompletion,
                                            simulationCPU.getMyProcess().getRestartTime() + oldRemTime);
                                    eventQueue.insertEvent(knownCompletion);
                                } else {
                                    // we need to preempt when the new process arrives, not right now
                                }
                            }
                        } else {
                            continue;
                        }
                        // set start time for a new, non-returning process
                    } // end PSJF completion

                    else if (algorithmType == SchedulerType.RR.getSchedulerType()) {
                        simulationCPU.getMyProcess().setRemainingCpuTime(0); // process is done
                        simulationCPU.getMyProcess().setCompletionTime(simulationClock.getSimulationTime());
                        simulationCPU.getMyProcess().setTurnaroundTime(simulationCPU.getMyProcess().getCompletionTime()
                                - simulationCPU.getMyProcess().getArrivalTime());
                        double completionMinusStart = simulationCPU.getMyProcess().getCompletionTime() - simulationCPU.getMyProcess().getStartTime();
                        //simulationCPU.getMyProcess().setWaitingTime(simulationCPU.getMyProcess().getTurnaroundTime()
                        //        - simulationCPU.getMyProcess().getBurstTime());
                        simulationCPU.getMyProcess().setWaitingTime(
                                (simulationCPU.getMyProcess().getStartTime() - simulationCPU.getMyProcess().getArrivalTime())
                                + (completionMinusStart - simulationCPU.getMyProcess().getBurstTime()));

                        // now that a process is complete, update runningSums that we will use to calculate statistics
                        SchedulingAlgorithm.runningBurstTimeSum += simulationCPU.getMyProcess().getBurstTime();
                        SchedulingAlgorithm.runningTurnaroundSum += simulationCPU.getMyProcess().getTurnaroundTime();
                        SchedulingAlgorithm.runningWaitTimeSum += simulationCPU.getMyProcess().getWaitingTime();

                        simulationCPU.setBusy(false);

                        if (!Objects.requireNonNull(schedulingAlgorithm).myQueue.isEmpty()) {
                            simulationCPU.setMyProcess(schedulingAlgorithm.getNextProcessForCPU());
                            simulationCPU.setBusy(true);
                            // set start time for a new, non-returning process
                            if(!simulationCPU.getMyProcess().isReturning()) {
                                simulationCPU.getMyProcess().setStartTime(simulationClock.getSimulationTime());
                                simulationCPU.getMyProcess().setIsReturning(true);
                            }

                            determineCompletionOrQuantumInterrupt(quantumForRR, simulationClock, eventQueue, simulationCPU);
                        } else {
                            continue;
                        }
                    } // end RR completion
                } // end else-if to handle Process Completions
                else if (eventToProcessType == EventType.TimeSliceOccurrence) {
                    simulationCPU.getMyProcess().setRemainingCpuTime(simulationCPU.getMyProcess().getRemainingCpuTime() - quantumForRR);
                    Objects.requireNonNull(schedulingAlgorithm).myQueue.insertProcess(simulationCPU.getMyProcess());
                    simulationCPU.setMyProcess(schedulingAlgorithm.getNextProcessForCPU());
                    checkIfReturningAndSetTimes(simulationClock, simulationCPU);
                    determineCompletionOrQuantumInterrupt(quantumForRR, simulationClock, eventQueue, simulationCPU);
                } // end time slice occurrence
            } // end while

            if (Objects.requireNonNull(schedulingAlgorithm).getSchedulerType() == SchedulerType.PSJF && togglePSJFCurve) {
                schedulingAlgorithm.myQueue.iterateAndGetRemainingDifferenceForPSJF(simulationClock.getSimulationTime());
            }

            if (schedulingAlgorithm.getSchedulerType() == SchedulerType.RR) {
                schedulingAlgorithm.myQueue.iterateAndGetRemainingDifferenceForRR();
            }

            System.out.println("Total sim time: " + simulationClock.getSimulationTime());
            calculateStatistics(schedulingAlgorithm, simulationClock.getSimulationTime(), lambda);
        } // end if-else args.length validation
    } // end main

    /**
     * Used by PSJF algorithm to determine if and when a given process will complete.
     */
    private static void determineCompletion(Clock simulationClock, EventQueue eventQueue, CPU simulationCPU) {
        // determine completion
        Event nextEvent = eventQueue.safelyPeekAtNextEvent();
        if (nextEvent.getEventType() == EventType.ProcessArrival) {
            double nextArrival = nextEvent.getEventTime();
            double _elapsedTime = nextArrival - simulationClock.getSimulationTime();
            double _oldRemTime = simulationCPU.getMyProcess().getRemainingCpuTime();
            double _newRemTime = _oldRemTime - _elapsedTime;

            if (_newRemTime <= 0) {
                Event knownCompletion = new Event(EventType.ProcessCompletion,
                        simulationClock.getSimulationTime() + _oldRemTime);
                eventQueue.insertEvent(knownCompletion);
            }
        }
    } // end determineCompletion

    /**
     * Used by multiple schedulers as a generic check to determine if a process is new or returning and set
     * certain parameters accordingly. If a process is new, we set the start time, otherwise we do not so we do
     * not override it.
     */
    private static void checkIfReturningAndSetTimes(Clock simulationClock, CPU simulationCPU) {
        if (!simulationCPU.getMyProcess().isReturning()) {
            simulationCPU.getMyProcess().setStartTime(simulationClock.getSimulationTime());
            simulationCPU.getMyProcess().setRestartTime(simulationCPU.getMyProcess().getStartTime());
            simulationCPU.getMyProcess().setIsReturning(true);
        } else {
            simulationCPU.getMyProcess().setRestartTime(simulationClock.getSimulationTime());
        }
    }

    /**
     * This method generates a new arrival event and places it in the event queue.
     */
    private static void unconditionallyCreateNewArrival(int lambda, Clock simulationClock, EventQueue eventQueue) {
        // routine to unconditionally create new arrival event
        Event newArrival = new Event(EventType.ProcessArrival,
                simulationClock.getSimulationTime() + genexp(lambda));
        eventQueue.insertEvent(newArrival);
    }


    private static SchedulingAlgorithm createSchedulingAlgorithm(int algorithmType) {
        SchedulingAlgorithm schedulingAlgorithm; // validate that algorithmType is in range (1,2)
        if (algorithmType > 0 && algorithmType < 3) {

            // create scheduler based on user defined type
            // the scheduler will internally set its type and create its specific Process Ready Queue
            if (algorithmType == SchedulerType.PSJF.getSchedulerType()) {  // PSJF
                schedulingAlgorithm = new PSJF();
            }
            else {  // RR
                schedulingAlgorithm = new RR();
            }
        } else {
            System.out.print("Please enter a valid value for the algorithm type, in range [1,2].");
            return null;
        }

        return  schedulingAlgorithm;
    }

    /***
     * This method prints usage instructions to the command line if the user does not specify any
     * command line arguments or types the word 'help'
     */
    private static void printProgramInstructions() {
        System.out.println("Event Simulator for 2 scheduling algorithms. ");
        System.out.println("Author: Jack Shendrikov");
        System.out.println("Parameters 1 - 4 are required, including quantum even if it is not used by the scheduler.");
        System.out.println("java -jar DiscreteEventSimulator.jar <scheduler_type> <lambda> <avg. svc time> <quantum> <togglePSJFCurve>");
        System.out.println("[scheduler_type] : value can be in the range [1,2].");
        System.out.println("\t1 - Preemptive Shortest Job First  (PSJF) Scheduler");
        System.out.println("\t2 - Round Robin (RR) Scheduler - requires 4th argument defining a quantum value.");
        System.out.println("[lambda] : average rate lambda that follows a Poisson process, to ensure exponential inter-arrival times.");
        System.out.println("[avg. svc time] : the service time is chosen according to an exponential distribution with an average service time of this third argument");
        System.out.println("[quantum] : optional argument only required for Round Robin (scheduler_type = 2). Defines the length of the quantum time slice.");
        System.out.println("[togglePSJFCurve] : accepts true or false. Optional argument to toggle the PSJF curve from flat (false) to non-flat (true).");
    }

    /**
     * @return rand (0,1)
     */
    private static double urand() {
        Random rand = new Random();

        return rand.nextDouble();
    }

    /**
     * @return either arrival time or service time
     */
    private static double genexp(double lambda) {
        double u, x;
        x = 0;

        while (x == 0) {
            u = urand();
            x = (-1/lambda)*log(u);
        }
        return x;
    }

    private static void determineCompletionOrQuantumInterrupt(double quantumForRR, Clock simulationClock, EventQueue eventQueue, CPU simulationCPU) {
        if (simulationCPU.getMyProcess().getRemainingCpuTime() - quantumForRR <= 0) {
            Event knownCompletion = new Event(EventType.ProcessCompletion,
                    simulationClock.getSimulationTime() + simulationCPU.getMyProcess().getRemainingCpuTime());
            eventQueue.insertEvent(knownCompletion);
        } else if (simulationCPU.getMyProcess().getRemainingCpuTime() - quantumForRR > 0) {
            Event interrupt = new Event(EventType.TimeSliceOccurrence,
                    simulationClock.getSimulationTime() + quantumForRR);
            eventQueue.insertEvent(interrupt);
        }
    }


    private static void calculateStatistics(SchedulingAlgorithm s, double totalSimTime, int lambda) throws IOException {
        double cpuUtil = s.cpuUtilization(totalSimTime);
        double avgTurn = s.avgTurnaroundTime(totalSimTime);
        double avgThroughput = s.throughput(totalSimTime);
        double avgProcessInQueue = s.avgProcessesInReadyQueue(lambda);
        double avgWaitingTime = s.avgWaitingTime(totalSimTime);
        // minor correction to rounding
        if (s.getSchedulerType() == SchedulerType.PSJF && cpuUtil > 1) {
            cpuUtil = cpuUtil - 0.0499;
        }

        System.out.println("Average Turnaround Time: " + avgTurn);
        System.out.println("Average Throughput: " + avgThroughput);
        System.out.println("CPU Utilization: " + cpuUtil);
        System.out.println("Average number of processes in Ready Queue: " + avgProcessInQueue);
        System.out.println("Average Waiting Time: " + avgWaitingTime);

        FileWriter pw = new FileWriter("test.csv", true);
        BufferedReader br = new BufferedReader(new FileReader("test.csv"));
        StringBuilder sb = new StringBuilder();

        if(br.readLine() == null) {
            sb.append("Lambda, Average Turnaround, Throughput, CPU Utilization, Average # of processes in Ready Queue, Average Waiting Time");
        }

        sb.append('\n');
        sb.append(lambda).append(',');
        sb.append(avgTurn).append(',');
        sb.append(s.throughput(totalSimTime)).append(',');
        sb.append(cpuUtil).append(',');
        sb.append(s.avgProcessesInReadyQueue(lambda)).append(',');
        sb.append(s.avgWaitingTime(totalSimTime));

        pw.write(sb.toString());
        pw.close();
    }

}
