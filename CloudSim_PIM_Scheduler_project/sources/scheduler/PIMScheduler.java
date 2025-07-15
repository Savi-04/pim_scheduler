package scheduler;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;

import java.util.List;
import java.util.LinkedList;
import java.util.Queue;

public class PIMScheduler {

    private static double threshold = 0.004;
    private static final double MIN_THRESHOLD = 0.002;
    private static final double MAX_THRESHOLD = 0.01;
    private static final double ADJUST_STEP = 0.0005;
    private static final int ERROR_WINDOW = 10;
    private static Queue<Double> recentErrors = new LinkedList<>();

    /**
     * Classifies a job (cloudlet) based on RAM/Length ratio, deadline, and simulated 10% execution time.
     *
     * @param cloudletId ID of the cloudlet
     * @param ram        RAM requirement in MB
     * @param length     Total length of the cloudlet (in MI)
     * @param deadline   Deadline in seconds
     * @return           "PIM" or "CPU"
     */
    public static String classifyJob(int cloudletId, int ram, long length, double deadline) {
        double ratio = (double) ram / length;
        double simulated10PercentTime = (length * 0.10) / 100000.0;  // simulate on 100K MIPS VM

        Log.printLine("\n--- Profiling Cloudlet ---");
        Log.printLine("Cloudlet ID: " + cloudletId);
        Log.printLine("RAM Required: " + ram + " MB");
        Log.printLine("Length: " + length);
        Log.printLine("RAM/Length Ratio: " + ratio);
        Log.printLine("Deadline: " + deadline + " seconds");
        Log.printLine("Simulated 10% Execution Time: " + String.format("%.2f", simulated10PercentTime) + " seconds");

        // Heuristic + 10% execution guidance
        if (ratio > threshold && deadline > 30.0) {
            Log.printLine("Classification Result: PIM\n");
            return "PIM";
        } else {
            Log.printLine("Classification Result: CPU\n");
            return "CPU";
        }
    }

    /**
     * Selects the first available VM from the list that matches the type (CPU/PIM).
     *
     * @param vmList  List of VMs
     * @param decision "PIM" or "CPU"
     * @return        The first matching VM or null
     */
    public static Vm selectVM(List<Vm> vmList, String decision) {
        for (Vm vm : vmList) {
            boolean isPIM = vm.getMips() < 9000;
            if ((decision.equals("PIM") && isPIM) || (decision.equals("CPU") && !isPIM)) {
                return vm;
            }
        }
        return null; // No matching VM
    }

    public static void updateThreshold(double actualTime, double predictedTime) {
        double error = Math.abs(actualTime - predictedTime) / actualTime;
        recentErrors.add(error);
        if (recentErrors.size() > ERROR_WINDOW) {
            recentErrors.poll(); // remove oldest
        }

        double sum = 0;
        for (double e : recentErrors) {
            sum += e;
        }
        double avgError = sum / recentErrors.size();

        if (avgError > 0.2 && threshold < MAX_THRESHOLD) {
            threshold += ADJUST_STEP;
            Log.printLine("Threshold increased to: " + threshold);
        } else if (avgError < 0.05 && threshold > MIN_THRESHOLD) {
            threshold -= ADJUST_STEP;
            Log.printLine("Threshold decreased to: " + threshold);
        }
    }
}