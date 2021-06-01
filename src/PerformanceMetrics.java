/***
 * @author Jack Shendrikov
 *
 * Provides a contract for the type of metrics all schedulers will need to calculate. Requires continuously updating
 * certain intermediate variables (using values obtained as each Process is dealt with) throughout the simulation.
 */

public interface PerformanceMetrics {

    double avgTurnaroundTime(double totalSimTime);

    double throughput(double totalSimTime);

    double cpuUtilization(double totalSimTime);

    double avgProcessesInReadyQueue(int lambda);

    double avgWaitingTime(double totalSimTime);
}
