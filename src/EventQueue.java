import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;

/***
 * @author Jack Shendrikov
 */

public class EventQueue {
    private static PriorityQueue<Event> priorityQueue;

    EventQueue() {
        Comparator<Event> comparator = new EventTimeComparator();
        priorityQueue = new PriorityQueue<>(10, comparator);
    }

    void insertEvent(Event e) {
        priorityQueue.add(e);
    }

    // Retrieve and Remove head of queue
    Event returnAndRemoveHeadEvent() {
        return priorityQueue.poll();
    }

    Event safelyPeekAtNextEvent() {
        return priorityQueue.peek();
    }

    double getSystemTimeFromHead() {
        return Objects.requireNonNull(priorityQueue.peek()).getEventTime();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (Event e : priorityQueue) {
            s.append(e.toString()).append(" | ");
        }
        return s.toString();
    }
}
