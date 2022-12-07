public class HashMap<K, V> implements StoreSystem<K, V> {
    class Node<K, V> {
        private K key;
        private V value;
        private Node<K, V> next;

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private Node[] array = new Node[16];
    private int size = 0;

    /**
     * Constructor
     */
    public HashMap() {
    }

    /**
     * Constructor
     *
     * @param capacity
     */
    public HashMap(int capacity) {
        this.array = new Node[capacity];
    }

    @Override
    public boolean insert(K key, V value) {
        if (size * 1.0D / array.length >= 0.75) {
            rehash();
        }
        if (search(key) != null) {
            // already added
            return false;
        }

        // insert new value to the link head
        Node<K, V> newNode = new Node<>(key, value);
        int index = hashIndex(key);
        newNode.next = array[index];
        array[index] = newNode;
        size++;
        return true;
    }

    @Override
    public V search(K key) {
        // get node links
        int index = hashIndex(key);
        Node node = array[index];
        // search the value in the target link
        while (node != null) {
            if (node.key.equals(key)) {
                return (V) node.value;
            }
            node = node.next;
        }
        return null;
    }

    @Override
    public V delete(K key) {
        int index = hashIndex(key);
        Node node = array[index];
        if (node == null) {
            return null;
        }

        if (node.key.equals(key)) {
            // the key is the link's head
            array[index] = node.next;
            size--;
            return (V) node.value;
        }

        while (node.next != null) {
            if (node.next.key.equals(key)) {
                V value = (V) node.next.value;
                node.next = node.next.next;
                size--;
                return value;
            }
            node = node.next;
        }
        return null;
    }

    /**
     * hashIndex
     *
     * @param key
     * @return
     */
    private int hashIndex(K key) {
        int hash = key.hashCode();
        if (hash < 0) {
            hash = -hash;
        }
        return hash % array.length;
    }

    /**
     * rehash, make the array bigger
     */
    private void rehash() {
        HashMap<K, V> newMap = new HashMap<>(array.length * 2);
        for (Node node : array) {
            while (node != null) {
                newMap.insert((K) node.key, (V) node.value);
                node = node.next;
            }
        }
        this.array = newMap.array;
    }
}
