package pimsim;

import java.util.*;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.power.*;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.*;

public class HeterogeneousHostConfig {

    // Modified method to create a CPU Host with configurable MIPS and RAM
    public static Host createCPUHost(int id, int mips, int ram) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // PE with configurable MIPS

        long storage = 1_000_000; // 1 TB
        int bw = 10000;

        PowerModel powerModel = new PowerModelLinear(200, 250); // High power for CPU

        return new PowerHost(
            id,
            new RamProvisionerSimple(ram),
            new BwProvisionerSimple(bw),
            storage,
            peList,
            new VmSchedulerTimeShared(peList),
            powerModel
        );
    }

    // Modified method to create a PIM Host with configurable MIPS and RAM
    public static Host createPIMHost(int id, int mips, int ram) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // PE with configurable MIPS

        long storage = 1_000_000; // 1 TB
        int bw = 10000;

        PowerModel powerModel = new PowerModelLinear(80, 120); // Lower power for PIM

        return new PowerHost(
            id,
            new RamProvisionerSimple(ram),
            new BwProvisionerSimple(bw),
            storage,
            peList,
            new VmSchedulerTimeShared(peList),
            powerModel
        );
    }

    // Helper: Generate a list of CPU Hosts with varying configurations
    public static List<Host> generateMultipleCPUHosts() {
        List<Host> cpuHosts = new ArrayList<>();
        cpuHosts.add(createCPUHost(0, 12000, 16384)); // High-end
        cpuHosts.add(createCPUHost(1, 11000, 8192));  // Mid-tier
        cpuHosts.add(createCPUHost(2, 10000, 4096));  // Low-tier
        return cpuHosts;
    }

    // Helper: Generate a list of PIM Hosts with varying configurations
    public static List<Host> generateMultiplePIMHosts() {
        List<Host> pimHosts = new ArrayList<>();
        pimHosts.add(createPIMHost(3, 9000, 32768));  // High-end PIM
        pimHosts.add(createPIMHost(4, 8500, 16384));  // Mid-tier
        pimHosts.add(createPIMHost(5, 8000, 8192));   // Low-tier
        return pimHosts;
    }
}
