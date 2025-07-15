package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;
import scheduler.PIMScheduler;
import pimsim.HeterogeneousHostConfig;

import java.text.DecimalFormat;
import java.util.*;

public class CloudSimExample1 {

    public static void main(String[] args) {
        Log.printLine("Starting CloudSimExample1...");

        try {
            // 1. Initialize CloudSim
            CloudSim.init(1, Calendar.getInstance(), false);

            // 2. Create Datacenter with CPU and PIM hosts
            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            // 3. Create Broker
            DatacenterBroker broker = new DatacenterBroker("Broker");
            int brokerId = broker.getId();

            // 4. Create VMs
            List<Vm> vmList = new ArrayList<>();
            // CPU VMs
            vmList.add(new Vm(0, brokerId, 10000, 1, 2048, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            vmList.add(new Vm(1, brokerId, 9000, 1, 2048, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            vmList.add(new Vm(2, brokerId, 11000, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            // PIM VMs
            vmList.add(new Vm(3, brokerId, 8000, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            vmList.add(new Vm(4, brokerId, 7000, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            vmList.add(new Vm(5, brokerId, 8500, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            broker.submitVmList(vmList);

            // 5. Create Cloudlets
            List<Cloudlet> cloudletList = new ArrayList<>();
            UtilizationModel utilization = new UtilizationModelFull();

            // Metadata: RAM requirement and deadline
            Map<Integer, Integer> ramMap = Map.of(0, 2000, 1, 1000, 2, 256);
            Map<Integer, Double> deadlineMap = Map.of(0, 60.0, 1, 25.0, 2, 15.0);

            // Create cloudlets
            cloudletList.add(new Cloudlet(0, 10000, 1, 300, 300, utilization, utilization, utilization));
            cloudletList.add(new Cloudlet(1, 400000, 1, 300, 300, utilization, utilization, utilization));
            cloudletList.add(new Cloudlet(2, 8000, 1, 300, 300, utilization, utilization, utilization));
            for (Cloudlet cl : cloudletList) cl.setUserId(brokerId);

            // 6. Scheduler: Use PIMScheduler for profiling + assignment
            for (Cloudlet cl : cloudletList) {
                int id = cl.getCloudletId();
                int ram = ramMap.get(id);
                long len = cl.getCloudletLength();
                double ddl = deadlineMap.get(id);

                String decision = PIMScheduler.classifyJob(id, ram, len, ddl);
                Vm chosenVM = PIMScheduler.selectVM(vmList, decision);

                if (chosenVM != null) {
                    cl.setVmId(chosenVM.getId());
                    Log.printLine("Assigned Cloudlet " + id + " to VM " + chosenVM.getId() + " (" + decision + ")");
                } else {
                    Log.printLine("No suitable VM found for Cloudlet " + id);
                }
            }

            // 7. Submit jobs to broker
            broker.submitCloudletList(cloudletList);

            // 8. Run Simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // 9. Print Results
            printCloudletList(broker.getCloudletReceivedList());
            Log.printLine("CloudSimExample1 finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happened.");
        }
    }

    private static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hosts = new ArrayList<>();
        hosts.addAll(HeterogeneousHostConfig.generateMultipleCPUHosts());
        hosts.addAll(HeterogeneousHostConfig.generateMultiplePIMHosts());

        DatacenterCharacteristics dcChars = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hosts, 10.0, 3.0, 0.05, 0.001, 0.0);

        return new Datacenter(name, dcChars, new VmAllocationPolicySimple(hosts), new LinkedList<>(), 0.1);
    }

    private static void printCloudletList(List<Cloudlet> list) {
        DecimalFormat dft = new DecimalFormat("###.##");
        String indent = "    ";
        Log.printLine("\n========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Data Center ID" +
                indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        for (Cloudlet c : list) {
            Log.print(indent + c.getCloudletId() + indent);
            if (c.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS" + indent + indent + c.getResourceId() + indent + indent +
                        c.getVmId() + indent + indent + dft.format(c.getActualCPUTime()) + indent + indent +
                        dft.format(c.getExecStartTime()) + indent + indent + dft.format(c.getFinishTime()));
            }
            Log.printLine();

            double actualTime = c.getActualCPUTime();
            int id = c.getCloudletId();
            double predictedTime = PIMScheduler.getPredictedTime(id);

            if (predictedTime > 0) {
                PIMScheduler.updateThreshold(actualTime, predictedTime);
            }
        }
    }
}