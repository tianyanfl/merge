public interface StoreSystem<K,V> {
    /**
     * insert
     * @param value
     */
    boolean insert(K key, V value);

    /**
     * search
     * @param id
     * @return
     */
    V search(K id);

    /**
     * delete
     * @param id
     * @return
     */
    V delete(K id);
}
