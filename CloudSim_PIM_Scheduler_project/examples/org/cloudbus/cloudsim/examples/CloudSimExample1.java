package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.*;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.*;

import pimsim.HeterogeneousHostConfig;

public class CloudSimExample1 {

    public static void main(String[] args) {
        Log.printLine("Starting CloudSimExample1...");

        try {
            // 1. Initialize CloudSim
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;
            CloudSim.init(num_user, calendar, trace_flag);

            // 2. Create Datacenter with multiple CPU and PIM hosts
            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            // 3. Create Broker
            DatacenterBroker broker = new DatacenterBroker("Broker");
            int brokerId = broker.getId();

            // 4. Create VMs
            List<Vm> vmList = new ArrayList<>();
            // Create 3 CPU VMs and 3 PIM VMs corresponding to the hosts
            vmList.add(new Vm(0, brokerId, 10000, 1, 2048, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            vmList.add(new Vm(1, brokerId, 9000, 1, 2048, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            vmList.add(new Vm(2, brokerId, 11000, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));

            vmList.add(new Vm(3, brokerId, 8000, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            vmList.add(new Vm(4, brokerId, 7000, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            vmList.add(new Vm(5, brokerId, 8500, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));

            broker.submitVmList(vmList);

            // 5. Create Cloudlets
            List<Cloudlet> cloudletList = new ArrayList<>();
            UtilizationModel utilizationModel = new UtilizationModelFull();
            Map<Integer, Integer> ramMap = new HashMap<>();
            Map<Integer, Double> deadlineMap = new HashMap<>();

            Cloudlet cl0 = new Cloudlet(0, 10000, 1, 300, 300, utilizationModel, utilizationModel, utilizationModel);
            cl0.setUserId(brokerId); cloudletList.add(cl0); ramMap.put(0, 2000); deadlineMap.put(0, 60.0);

            Cloudlet cl1 = new Cloudlet(1, 400000, 1, 300, 300, utilizationModel, utilizationModel, utilizationModel);
            cl1.setUserId(brokerId); cloudletList.add(cl1); ramMap.put(1, 1000); deadlineMap.put(1, 25.0);

            Cloudlet cl2 = new Cloudlet(2, 8000, 1, 300, 300, utilizationModel, utilizationModel, utilizationModel);
            cl2.setUserId(brokerId); cloudletList.add(cl2); ramMap.put(2, 256); deadlineMap.put(2, 15.0);

            // 6. Schedule cloudlets based on RAM/Length ratio and deadline
            for (Cloudlet c : cloudletList) {
                int ram = ramMap.get(c.getCloudletId());
                double deadline = deadlineMap.get(c.getCloudletId());
                double ratio = (double) ram / c.getCloudletLength();
                String decision = "CPU";
                if (ratio > 0.004 && deadline > 30.0) decision = "PIM";

                // Find the first available matching VM
                for (Vm vm : vmList) {
                    boolean isPIM = vm.getMips() < 9000;
                    if ((decision.equals("PIM") && isPIM) || (decision.equals("CPU") && !isPIM)) {
                        c.setVmId(vm.getId());
                        Log.printLine("Assigned Cloudlet " + c.getCloudletId() + " to VM " + vm.getId() + " (" + decision + ")");
                        break;
                    }
                }
            }

            // 7. Submit cloudlets after assigning VM IDs
            broker.submitCloudletList(cloudletList);

            // 8. Start simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // 9. Display output
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList);
            Log.printLine("CloudSimExample1 finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<>();
        hostList.addAll(HeterogeneousHostConfig.generateMultipleCPUHosts());
        hostList.addAll(HeterogeneousHostConfig.generateMultiplePIMHosts());

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            "x86", "Linux", "Xen", hostList, 10.0, 3.0, 0.05, 0.001, 0.0
        );

        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0.1);
    }

    private static void printCloudletList(List<Cloudlet> list) {
        String indent = "    ";
        DecimalFormat dft = new DecimalFormat("###.##");
        Log.printLine("\n========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Data Center ID" + indent + "VM ID" +
                      indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        for (Cloudlet c : list) {
            Log.print(indent + c.getCloudletId() + indent);
            if (c.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS" + indent + indent + c.getResourceId() + indent + indent + c.getVmId()
                          + indent + indent + dft.format(c.getActualCPUTime()) + indent + indent
                          + dft.format(c.getExecStartTime()) + indent + indent + dft.format(c.getFinishTime()));
            }
            Log.printLine();
        }
    }
}