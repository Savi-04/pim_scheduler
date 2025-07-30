package scheduler;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;

import java.util.List;

public class BaselineScheduler {

    private static final double STATIC_THRESHOLD = 0.004;  // fixed threshold

    /**
     * Classifies job based only on RAM/Length ratio and fixed threshold.
     */
    public static String classifyJob(int cloudletId, int ram, long length, double deadline) {
        double ratio = (double) ram / length;

        Log.printLine("\n--- Baseline Profiling ---");
        Log.printLine("Cloudlet ID: " + cloudletId);
        Log.printLine("RAM: " + ram + ", Length: " + length);
        Log.printLine("RAM/Length Ratio: " + ratio);
        Log.printLine("Deadline: " + deadline);

        if (ratio > STATIC_THRESHOLD) {
            Log.printLine("Classification Result: PIM\n");
            return "PIM";
        } else {
            Log.printLine("Classification Result: CPU\n");
            return "CPU";
        }
    }

    /**
     * Selects the first available VM that matches the job type.
     */
    public static Vm selectVM(List<Vm> vmList, String decision) {
        for (Vm vm : vmList) {
            boolean isPIM = vm.getMips() < 9000;
            if ((decision.equals("PIM") && isPIM) || (decision.equals("CPU") && !isPIM)) {
                return vm;
            }
        }
        return null; // fallback
    }

    public static double getFixedThreshold() {
        return STATIC_THRESHOLD;
    }
}
