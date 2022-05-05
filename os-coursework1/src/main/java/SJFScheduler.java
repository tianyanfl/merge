import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Queue;

/**
 * Shortest Job First Scheduler
 *
 * @version 2017
 */
public class SJFScheduler extends AbstractScheduler {

    private double alphaBurstEstimate;
    private double initBurstEstimate;
    private Queue<Process> readyQueue;

    /**
     * Construct
     */
    public SJFScheduler() {
        this.alphaBurstEstimate = 0;
        this.initBurstEstimate = 0;
        this.readyQueue = new PriorityQueue<>();
    }

    /**
     * Initializes the scheduler from the given parameters
     */
    @Override
    public void initialize(Properties parameters) {
        this.initBurstEstimate = Integer.parseInt(parameters.getProperty("initialBurstEstimate"));
        this.alphaBurstEstimate = Double.parseDouble(parameters.getProperty("alphaBurstEstimate"));
    }

    /**
     * Adds a process to the ready queue.
     * usedFullTimeQuantum is true if process is being moved to ready
     * after having fully used its time quantum.
     */
    public void ready(Process process, boolean usedFullTimeQuantum) {
        double priority = process.getPriority() == 0 ? (1 - alphaBurstEstimate) * initBurstEstimate :
                (1 - alphaBurstEstimate) * process.getPriority() + alphaBurstEstimate * process.getRecentBurst();
        process.setPriority((int) Math.round(priority));
        readyQueue.add(process);
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