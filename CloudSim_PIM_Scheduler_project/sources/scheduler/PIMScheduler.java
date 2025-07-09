package scheduler;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;

import java.util.List;

public class PIMScheduler {

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
        if (ratio > 0.004 && deadline > 30.0) {
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
}