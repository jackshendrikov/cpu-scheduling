/***
 * @author Jack Shendrikov
 */

enum EventType {
    ProcessArrival("ProcessArrival"),
    ProcessCompletion("ProcessCompletion"),
    TimeSliceOccurrence("TimeSliceOccurrence");

    private final String eventType;

    EventType(String eventType) {
        this.eventType = eventType;
    }
}

/***
 * @author Jack Shendrikov
 *
 * Defines what constitutes an event and internalizes the time at which it occurs.
 */
public class Event {

    private EventType eventType;
    private Double eventTime;

    Event(EventType eventType, double eventTime) {
        this.eventType = eventType;
        this.eventTime = eventTime;
    }

    double getEventTime() {
        return eventTime;
    }

    EventType getEventType() {
        return eventType;
    }

    public void setEventTime(double eventTime) {
        this.eventTime = eventTime;
    }

    @Override
    public String toString() {
        return eventType.toString() + " at time " + eventTime.toString();
    }
}
