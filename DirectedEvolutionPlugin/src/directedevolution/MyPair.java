package directedevolution;

import java.io.Serializable;

/**
 * <p>A convenience class to represent name-value pairs.</p>
 *
 * @since JavaFX 2.0
 */
public class MyPair<K, V> implements Serializable {

    /**
     * Key of this <code>MyPair</code>.
     */
    private K key;

    /**
     * Gets the key for this pair.
     *
     * @return key for this pair
     */
    public K getKey() {
        return key;
    }

    /**
     * Value of this this <code>MyPair</code>.
     */
    private V value;

    /**
     * Gets the value for this pair.
     *
     * @return value for this pair
     */
    public V getValue() {
        return value;
    }

    /**
     * Creates a new pair
     *
     * @param key   The key for this pair
     * @param value The value to use for this pair
     */
    public MyPair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * <p><code>String</code> representation of this
     * <code>MyPair</code>.</p>
     * <p>
     * <p>The default name/value delimiter '=' is always used.</p>
     *
     * @return <code>String</code> representation of this <code>MyPair</code>
     */
    @Override
    public String toString() {
        return key + "=" + value;
    }

    /**
     * <p>Generate a hash code for this <code>MyPair</code>.</p>
     * <p>
     * <p>The hash code is calculated using both the name and
     * the value of the <code>MyPair</code>.</p>
     *
     * @return hash code for this <code>MyPair</code>
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (key != null ? key.hashCode() : 0);
        hash = 31 * hash + (value != null ? value.hashCode() : 0);
        return hash;
    }

    /**
     * <p>Test this <code>MyPair</code> for equality with another
     * <code>Object</code>.</p>
     * <p>
     * <p>If the <code>Object</code> to be tested is not a
     * <code>MyPair</code> or is <code>null</code>, then this method
     * returns <code>false</code>.</p>
     * <p>
     * <p>Two <code>MyPair</code>s are considered equal if and only if
     * both the names and values are equal.</p>
     *
     * @param o the <code>Object</code> to test for
     *          equality with this <code>MyPair</code>
     * @return <code>true</code> if the given <code>Object</code> is
     * equal to this <code>MyPair</code> else <code>false</code>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof MyPair) {
            MyPair pair = (MyPair) o;
            if (key != null ? !key.equals(pair.key) : pair.key != null)
                return false;
            if (value != null ? !value.equals(pair.value) : pair.value != null)
                return false;
            return true;
        }
        return false;
    }
}
