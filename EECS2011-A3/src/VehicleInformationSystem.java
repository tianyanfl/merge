public class VehicleInformationSystem {

    private StoreSystem<String, VehicleInformation> storeSystem;

    public VehicleInformationSystem(StoreSystem<String, VehicleInformation> storeSystem) {
        this.storeSystem = storeSystem;
    }

    /**
     * insert
     *
     * @param information
     */
    void insertInformation(VehicleInformation information) {
        storeSystem.insert(information.getId(), information);
    }

    /**
     * search
     *
     * @param id
     * @return
     */
    VehicleInformation searchInformation(String id) {
        return storeSystem.search(id);
    }

    /**
     * delete
     *
     * @param id
     * @return
     */
    boolean deleteInformation(String id) {
        return storeSystem.delete(id) != null;
    }
}
