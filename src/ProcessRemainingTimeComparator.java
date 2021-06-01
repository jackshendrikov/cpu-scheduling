import java.util.Comparator;

/***
 * @author Jack Shendrikov
 *
 * Used by PSJF scheduler.
 * Compares processes by their remainingCpuTime, so the process with the least remaining time is first in the queue.
 */
public class ProcessRemainingTimeComparator implements Comparator<Process> {

    @Override
    public int compare(Process p1, Process p2) {
        return Double.compare(p1.getRemainingCpuTime(), p2.getRemainingCpuTime());
    }
}
