/***
 * @author Jack Shendrikov
 *
 * `Clock` is shared throughout the simulation and only one value is valid, hence static simulation time.
 *
 * A simple setter is provided, invoked in the main driving loop of the simulator. The clock time is obtained
 * from the head of the `EventQueue` (i.e., the earliest arriving event, which is "next" to be processed sequentially).
 */

class Clock {
    private static double simulationTime;

    double getSimulationTime() {
        return simulationTime;
    }

    void setSimulationTime(double _simulationTime) {
        simulationTime = _simulationTime;
    }
}
