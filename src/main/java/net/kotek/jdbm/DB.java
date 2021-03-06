package net.kotek.jdbm;

import java.util.*;

/**
 * Database is root class for creating and loading persistent collections. It also contains
 * transaction operations.
 * //TODO just write some readme
 * <p/>
 *
 * @author Jan Kotek
 * @author Alex Boisvert
 * @author Cees de Groot
 */
public interface DB {

    /**
     * Closes the DB and release resources.
     * DB can not be used after it was closed
     */
    void close();

    /**
     * Clear cache and remove all entries it contains.
     * This may be useful for some Garbage Collection when reference cache is used.
     */
    void clearCache();

    /**
     * Defragments storage so it consumes less space.
     * It also rearranges collections and optimizes it for sequence reads.
     * <p/>
     * This commits any uncommited data.
     */
    void defrag();

    /**
     * Commit (make persistent) all changes since beginning of transaction.
     * JDBM supports only single transaction.
     */
    void commit();

    /**
     * Rollback (cancel) all changes since beginning of transaction.
     * JDBM supports only single transaction.
     * This operations affects all maps created or loaded by this DB.
     */
    void rollback();

    /**
     * This calculates some database statistics such as collection sizes and record distributions.
     * Can be useful for performance optimalisations and trouble shuting.
     * This method can run for very long time.
     *
     * @return statistics contained in string
     */
    String calculateStatistics();


    /**
     * Get a <code>Map</code> which was already created and saved in DB.
     * This map uses disk based H*Tree and should have similar performance
     * as <code>HashMap</code>.
     *
     * @param name of hash map
     *
     * @return map
     */
    <K, V> Map<K, V> getHashMap(String name);

    /**
     * Creates or load existing Map which persists data into DB.
     *
     * @param name record name
     * @return
     */
    <K, V> Map<K, V> createHashMap(String name);


    /**
     * Creates or load existing Primary Hash Map which persists data into DB.
     * Map will use custom serializers for Keys and Values.
     * Leave keySerializer null to use default serializer for keys
     *
     * @param <K>             Key type
     * @param <V>             Value type
     * @param name            record name
     * @param keySerializer   serializer to be used for Keys, leave null to use default serializer
     * @param valueSerializer serializer to be used for Values
     * @return
     */
    <K, V> Map<K, V> createHashMap(String name, Serializer<K> keySerializer, Serializer<V> valueSerializer);

    <K> Set<K> createHashSet(String name);

    <K> Set<K> getHashSet(String name);

    <K> Set<K> createHashSet(String name, Serializer<K> keySerializer);

    <K, V> SortedMap<K, V> getTreeMap(String name);

    /**
     * Creates or load existing Primary TreeMap which persists data into DB.
     *
     * @param <K>  Key type
     * @param <V>  Value type
     * @param name record name
     * @return
     */
    <K extends Comparable, V> SortedMap<K, V> createTreeMap(String name);

    /**
     * Creates or load existing TreeMap which persists data into DB.
     *
     * @param <K>             Key type
     * @param <V>             Value type
     * @param name            record name
     * @param keyComparator   Comparator used to sort keys
     * @param keySerializer   Serializer used for keys. This may reduce disk space usage     *
     * @param valueSerializer Serializer used for values. This may reduce disk space usage
     * @return
     */
    <K, V> SortedMap<K, V> createTreeMap(String name,
                                         Comparator<K> keyComparator, Serializer<K> keySerializer, Serializer<V> valueSerializer);

    <K> SortedSet<K> getTreeSet(String name);

    <K> SortedSet<K> createTreeSet(String name);

    <K> SortedSet<K> createTreeSet(String name, Comparator<K> keyComparator, Serializer<K> keySerializer);

    <K> List<K> createLinkedList(String name);

    <K> List<K> createLinkedList(String name, Serializer<K> serializer);

    <K> List<K> getLinkedList(String name );

}
