import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Ideal Shortest Job First Scheduler
 *
 * @version 2017
 */
public class IdealSJFScheduler extends AbstractScheduler {

    private List<Process> readyQueue;

    public IdealSJFScheduler() {
        readyQueue = new LinkedList<>();
    }

    /**
     * Adds a process to the ready queue.
     * usedFullTimeQuantum is true if process is being moved to ready
     * after having fully used its time quantum.
     */
    public void ready(Process process, boolean usedFullTimeQuantum) {
        readyQueue.add(process);
    }

    /**
     * Removes the next process to be run from the ready queue
     * and returns it.
     * Returns null if there is no process to run.
     */
    public Process schedule() {
        if (readyQueue.size() <= 0) {
            return null;
        }

        Process targetProcess = null;
        for (Process process : readyQueue) {
            if (targetProcess == null || process.getNextBurst() < targetProcess.getNextBurst()) {
                targetProcess = process;
            }
        }

        readyQueue.remove(targetProcess);
        System.out.println("Scheduler selects process " + targetProcess.toString());
        return targetProcess;
    }

}
