import java.util.Comparator;

/***
 * @author Jack Shendrikov
 *
 * Events are compared by their time of arrival (which is later on used to set the Process' `arrivalTime` field)
 * so they may be processed sequentially and maintain the right `Clock` value for simulation time.
 */
public class EventTimeComparator implements Comparator<Event> {

    @Override
    public int compare(Event e1, Event e2) {
        return Double.compare(e1.getEventTime(), e2.getEventTime());
    }
}
