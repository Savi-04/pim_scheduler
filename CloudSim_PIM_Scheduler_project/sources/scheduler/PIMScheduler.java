package scheduler;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;

import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashMap;
import java.util.Map;

public class PIMScheduler {

    private static double threshold = 0.004;
    private static final double MIN_THRESHOLD = 0.002;
    private static final double MAX_THRESHOLD = 0.01;
    private static final double ADJUST_STEP = 0.0005;
    private static final int ERROR_WINDOW = 10;
    private static Queue<Double> recentErrors = new LinkedList<>();
    private static Map<Integer, Double> predictedTimes = new HashMap<>();

    /**
     * Classifies a job (cloudlet) based on RAM/Length ratio, deadline, and simulated 10% execution time.
     */
    public static String classifyJob(int cloudletId, int ram, long length, double deadline) {
        double ratio = (double) ram / length;
        double simulated10PercentTime = (length * 0.10) / 100000.0;  // simulate on 100K MIPS VM
        savePredictedTime(cloudletId, simulated10PercentTime * 10); // store full predicted time

        Log.printLine("\n--- Profiling Cloudlet ---");
        Log.printLine("Cloudlet ID: " + cloudletId);
        Log.printLine("RAM Required: " + ram + " MB");
        Log.printLine("Length: " + length);
        Log.printLine("RAM/Length Ratio: " + ratio);
        Log.printLine("Deadline: " + deadline + " seconds");
        Log.printLine("Simulated 10% Execution Time: " + String.format("%.2f", simulated10PercentTime) + " seconds");

        if (ratio > threshold && deadline > 30.0) {
            Log.printLine("Classification Result: PIM\n");
            return "PIM";
        } else {
            Log.printLine("Classification Result: CPU\n");
            return "CPU";
        }
    }

    /**
     * Selects the lowest-energy VM from matching type (PIM or CPU).
     */
    public static Vm selectVM(List<Vm> vmList, String decision) {
        double assumedPower = 100.0; // in watts
        double minEnergy = Double.MAX_VALUE;
        Vm selectedVM = null;

        for (Vm vm : vmList) {
            boolean isPIM = vm.getMips() < 9000;
            if ((decision.equals("PIM") && isPIM) || (decision.equals("CPU") && !isPIM)) {
                double predictedTime = 1.0; // default time if unknown
                for (Map.Entry<Integer, Double> entry : predictedTimes.entrySet()) {
                    predictedTime = entry.getValue();
                    break; // just take one sample for now
                }

                double energy = predictedTime * assumedPower;
                Log.printLine("VM ID: " + vm.getId() + " | Type: " + (isPIM ? "PIM" : "CPU") + 
                              " | Predicted Exec Time: " + String.format("%.2f", predictedTime) + 
                              " sec | Estimated Energy: " + String.format("%.2f", energy) + " J");

                if (energy < minEnergy) {
                    minEnergy = energy;
                    selectedVM = vm;
                }
            }
        }

        return selectedVM;
    }

    /**
     * Updates the threshold dynamically based on prediction error feedback.
     */
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

    public static void savePredictedTime(int cloudletId, double predictedTime) {
        predictedTimes.put(cloudletId, predictedTime);
    }

    public static double getPredictedTime(int cloudletId) {
        return predictedTimes.getOrDefault(cloudletId, -1.0);
    }

    public static double getCurrentThreshold() {
        return threshold;
    }
}