package scheduler;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;

import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashMap;
import java.util.Map;

public class PIMScheduler {

    private static double threshold = 0.008;
    private static final double MAX_THRESHOLD = 5.0;    //because the threshold change not needs to be capped at a very low value
    private static final double MIN_THRESHOLD = 0.001;
    private static Map<Integer, Double> predictedTimes = new HashMap<>();
    private static Map<Integer, Double> thresholdMap = new HashMap<>();
    private static Queue<Double> recentErrors = new LinkedList<>();
    private static final int ERROR_WINDOW = 10;

    public static void resetScheduler() {
        threshold = 0.008;
        predictedTimes.clear();
        thresholdMap.clear();
        recentErrors.clear();
    }

    public static String classifyJob(int cloudletId, int ram, long length, double deadline) {
        double ratio = (double) ram / length;
        double simulated10PercentTime = (length * 0.10) / 100000.0;  // simulate on 100K MIPS VM
        double noise = 0.8 + Math.random() * 0.4; // Random noise between 0.8 and 1.2
        double predictedFullTime = simulated10PercentTime * 10 * noise;
        storePredictedTime(cloudletId, predictedFullTime);
        thresholdMap.put(cloudletId, threshold);

        Log.printLine("\n--- Profiling Cloudlet ---");
        Log.printLine("Cloudlet ID: " + cloudletId);
        Log.printLine("RAM Required: " + ram + " MB");
        Log.printLine("Length: " + length);
        Log.printLine("RAM/Length Ratio: " + ratio);
        Log.printLine("Deadline: " + deadline + " seconds");
        Log.printLine("Predicted Execution Time: " + String.format("%.4f", predictedFullTime));

        if (ratio > threshold && deadline > 30.0) {
            Log.printLine("Classification Result: PIM\n");
            return "PIM";
        } else {
            Log.printLine("Classification Result: CPU\n");
            return "CPU";
        }
    }

    public static Vm selectVM(List<Vm> vmList, String decision) {
        double assumedPower = 100.0; // in watts
        double minEnergy = Double.MAX_VALUE;
        Vm selectedVM = null;

        for (Vm vm : vmList) {
            boolean isPIM = vm.getMips() < 9000;
            if ((decision.equals("PIM") && isPIM) || (decision.equals("CPU") && !isPIM)) {
                double predictedTime = predictedTimes.values().stream().findFirst().orElse(1.0);
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

    public static void updateThreshold(double actualTime, double predictedTime) {
        double error = Math.abs(actualTime - predictedTime) / actualTime;
        recentErrors.add(error);
        if (recentErrors.size() > ERROR_WINDOW) {
            recentErrors.poll();
        }

        double avgError = recentErrors.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        Log.printLine("Actual: " + actualTime + " Predicted: " + predictedTime + " Error: " + error);
        Log.printLine("Threshold before: " + threshold);
        Log.printLine("Average Error: " + avgError);

        if (avgError > 0.2 && threshold < MAX_THRESHOLD) {
            threshold *= 1.1;
            if (threshold > MAX_THRESHOLD) threshold = MAX_THRESHOLD;
            Log.printLine("Threshold increased to: " + threshold);
        } else if (avgError < 0.05 && threshold > MIN_THRESHOLD) {
            threshold *= 0.9;
            if (threshold < MIN_THRESHOLD) threshold = MIN_THRESHOLD;
            Log.printLine("Threshold decreased to: " + threshold);
        }
    }

    public static void storePredictedTime(int cloudletId, double predictedTime) {
        predictedTimes.put(cloudletId, predictedTime);
    }

    public static double getPredictedTime(int cloudletId) {
        return predictedTimes.getOrDefault(cloudletId, -1.0);
    }

    public static double getLatestPredictedTime() {
        return predictedTimes.isEmpty() ? -1.0 :
                predictedTimes.get(predictedTimes.keySet().stream().max(Integer::compareTo).orElse(-1));
    }

    public static double getCurrentThreshold() {
        return threshold;
    }

    public static double getCloudletThreshold(int cloudletId) {
        return thresholdMap.getOrDefault(cloudletId, threshold);
    }
}