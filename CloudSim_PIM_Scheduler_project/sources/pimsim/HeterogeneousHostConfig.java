package pimsim;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.power.*;

import java.util.*;

public class HeterogeneousHostConfig {

    public static Host createCPUHost(int id) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(10000))); // 10,000 MIPS

        int ram = 16384; // 16 GB
        long storage = 1_000_000; // 1 TB
        int bw = 10000;

        PowerModel powerModel = new PowerModelLinear(200, 250); // CPU = high power

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

    public static Host createPIMHost(int id) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(8000))); // 8,000 MIPS

        int ram = 32768; // 32 GB
        long storage = 1_000_000;
        int bw = 10000;

        PowerModel powerModel = new PowerModelLinear(80, 120); // PIM = low power

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
}