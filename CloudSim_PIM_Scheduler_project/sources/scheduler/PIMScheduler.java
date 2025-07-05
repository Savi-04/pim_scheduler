package scheduler;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import java.util.*;

public class PIMScheduler {

    /**
     * Categorizes VMs into CPU or PIM based on their MIPS.
     * CPU VMs have MIPS >= 9000; others are PIM.
     * @param vmlist list of all available VMs
     * @return map with keys "CPU" and "PIM" mapped to VM lists
     */
    public static Map<String, List<Vm>> categorizeVMsByHost(List<Vm> vmlist) {
        Map<String, List<Vm>> categorized = new HashMap<>();
        categorized.put("CPU", new ArrayList<>());
        categorized.put("PIM", new ArrayList<>());

        for (Vm vm : vmlist) {
            double mips = vm.getMips();
            if (mips >= 9000) {
                categorized.get("CPU").add(vm);
            } else {
                categorized.get("PIM").add(vm);
            }
        }
        return categorized;
    }

    
   
    
    public static String classifyJob(Cloudlet cloudlet, int ramRequired, double deadline) {
        double length = cloudlet.getCloudletLength();
        double ratio = (double) ramRequired / length;

        // Logging the profiling details
        Log.printLine("\n--- Profiling Cloudlet ---");
        Log.printLine("Cloudlet ID: " + cloudlet.getCloudletId());
        Log.printLine("RAM Required: " + ramRequired + " MB");
        Log.printLine("Length: " + length);
        Log.printLine("RAM/Length Ratio: " + ratio);
        Log.printLine("Deadline: " + deadline + " seconds");

        // RAM-heavy and relaxed deadline jobs go to PIM
        
        
        
        
        
        if (ratio > 0.004 && deadline > 30.0) {
            Log.printLine("Classification Result: PIM\n");
            return "PIM";
        } else {
            Log.printLine("Classification Result: CPU\n");
            return "CPU";
        }
    }

    
    
    // A wrapper to hold a cloudlet with profiling metadata.
     
    
    
    
    
    public static class ProfiledCloudlet {
        public Cloudlet cloudlet;
        public int ramRequirement;
        public double deadline;

        public ProfiledCloudlet(Cloudlet cloudlet, int ram, double deadline) {
            this.cloudlet = cloudlet;
            this.ramRequirement = ram;
            this.deadline = deadline;
        }
    }
}