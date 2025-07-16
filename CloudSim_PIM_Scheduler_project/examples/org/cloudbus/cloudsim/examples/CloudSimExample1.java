package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;
import scheduler.PIMScheduler;
import pimsim.HeterogeneousHostConfig;

import java.text.DecimalFormat;
import java.util.*;
import java.io.*;

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
            vmList.add(new Vm(2, brokerId, 8000, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            // PIM VMs
            vmList.add(new Vm(3, brokerId, 8000, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            vmList.add(new Vm(4, brokerId, 7000, 1, 4096, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            vmList.add(new Vm(5, brokerId, 7000, 1, 3072, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
            broker.submitVmList(vmList);

            // 5. Create Cloudlets
            List<Cloudlet> cloudletList = new ArrayList<>();
            UtilizationModel utilization = new UtilizationModelFull();

            List<Integer> lengths = new ArrayList<>();
            List<Integer> rams = new ArrayList<>();
            List<Double> deadlines = new ArrayList<>();
            int numCloudlets = 300;

            Random rand = new Random(42); // Seed for reproducibility

            for (int i = 0; i < numCloudlets; i++) {
                int len = 1000 + rand.nextInt(500000); // Length from 1k to 500k
                int ram = 256 + rand.nextInt(4096);    // RAM from 256MB to ~4GB
                double ddl = 10.0 + rand.nextDouble() * 90.0; // Deadline from 10 to 100 sec

                lengths.add(len);
                rams.add(ram);
                deadlines.add(ddl);
            }

            Map<Integer, Integer> ramMap = new HashMap<>();
            Map<Integer, Double> deadlineMap = new HashMap<>();

            for (int i = 0; i < numCloudlets; i++) {
                Cloudlet cl = new Cloudlet(i, lengths.get(i), 1, 300, 300, utilization, utilization, utilization);
                cl.setUserId(brokerId);
                cloudletList.add(cl);
                ramMap.put(i, rams.get(i));
                deadlineMap.put(i, deadlines.get(i));
            }

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

        // Write results to CSV file
        try (PrintWriter writer = new PrintWriter(new FileWriter("results_dynamic.csv"))) {
            writer.println("CloudletID,VMID,Type,PredictedTime,ActualTime,Error,Threshold");

            for (Cloudlet c : list) {
                double actualTime = c.getActualCPUTime();
                int id = c.getCloudletId();
                double predictedTime = PIMScheduler.getPredictedTime(id);

                if (predictedTime > 0) {
                    PIMScheduler.updateThreshold(actualTime, predictedTime);
                }

                double error = (predictedTime > 0) ? Math.abs(actualTime - predictedTime) / actualTime : 0;
                String type = (c.getVmId() == 0 || c.getVmId() == 1 || c.getVmId() == 2) ? "CPU" : "PIM";
                double threshold = PIMScheduler.getCurrentThreshold();

                writer.printf("%d,%d,%s,%.2f,%.2f,%.4f,%.5f%n",
                        id, c.getVmId(), type, predictedTime, actualTime, error, threshold);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}