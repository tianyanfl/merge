import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Endstart
 * Date: 2022-12-06
 * Time: 23:18
 */
public class Test {
    private static Random random = new Random(65041809232488L);

    public static void main(String[] args) {
        test(10000);
        test(100000);
        test(1000000);
    }

    private static void test(int count) {
        System.out.println("\n============================" + count + "============================");
        List<VehicleInformation> vehicleInformationList = generateData(count);

        System.out.println("TestAlgorithm       InsertTime     SearchTime     DeleteTime");
        System.out.printf("%-20s", "HashMap");
        testVehicleInformationSystem(new VehicleInformationSystem(new HashMap<>()), vehicleInformationList);

        System.out.printf("%-20s", "BinarySearchTree");
        testVehicleInformationSystem(new VehicleInformationSystem(new BinarySearchTree<>()), vehicleInformationList);

    }

    /**
     * Test the insert, search and delete method
     *
     * @param system
     * @param vehicleInformationList
     */
    private static void testVehicleInformationSystem(VehicleInformationSystem system, List<VehicleInformation> vehicleInformationList) {
        long startTime = System.currentTimeMillis();
        for (VehicleInformation vehicleInformation : vehicleInformationList) {
            system.insertInformation(vehicleInformation);
        }
        System.out.printf("%-15d", (System.currentTimeMillis() - startTime));

        startTime = System.currentTimeMillis();
        for (VehicleInformation vehicleInformation : vehicleInformationList) {
            system.searchInformation(vehicleInformation.getId());
        }
        System.out.printf("%-15d", (System.currentTimeMillis() - startTime));

        startTime = System.currentTimeMillis();
        for (VehicleInformation vehicleInformation : vehicleInformationList) {
            system.deleteInformation(vehicleInformation.getId());
        }
        System.out.printf("%-15d", (System.currentTimeMillis() - startTime));
        System.out.println();
    }

    private static List<VehicleInformation> generateData(int count) {
        List<VehicleInformation> vehicleInformationList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = random.nextInt(1000000000) + "" + i;
            vehicleInformationList.add(new VehicleInformation(id, "name" + i));
        }
        return vehicleInformationList;
    }
}
