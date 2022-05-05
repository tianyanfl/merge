import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;

/**
 * Round Robin Scheduler
 *
 * @version 2017
 */
public class RRScheduler extends AbstractScheduler {

    private int timeQuantum;
    private Queue<Process> readyQueue;

    /**
     * Construct
     */
    public RRScheduler() {
        this.timeQuantum = 0;
        this.readyQueue = new LinkedList<>();
    }

    /**
     * Initializes the scheduler from the given parameters
     */
    @Override
    public void initialize(Properties parameters) {
        timeQuantum = Integer.parseInt(parameters.getProperty("timeQuantum"));
    }

    /**
     * Returns whether the scheduler is preemptive
     */
    public boolean isPreemptive() {
        return true;
    }

    /**
     * Returns the time quantum of this scheduler
     * or -1 if the scheduler does not require a timer interrupt.
     */
    @Override
    public int getTimeQuantum() {
        return timeQuantum;
    }

    /**
     * Adds a process to the ready queue.
     * usedFullTimeQuantum is true if process is being moved to ready
     * after having fully used its time quantum.
     */
    public void ready(Process process, boolean usedFullTimeQuantum) {
        readyQueue.offer(process);
    }

    /**
     * Removes the next process to be run from the ready queue
     * and returns it.
     * Returns null if there is no process to run.
     */
    public Process schedule() {
        System.out.println("Scheduler selects process " + readyQueue.peek());
        return readyQueue.poll();
    }
}
