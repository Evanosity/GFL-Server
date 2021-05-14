package ca.grindforloot.server;

/**
 * A simple key-value pair. The use case for this is so you can have a key-(key-value) map
 * @author Evan
 *
 * @param <K>
 * @param <V>
 */
public class Duple<K, V>{
	private K key;
	private V value;
	public Duple() {
		
	}
	public Duple(K k, V value) {
		this.key = k;
		this.value = value;
	}
	
	public K getKey() {
		return key;
	}
	public void setKey(K newKey) {
		key = newKey;
	}
	
	public V getValue() {
		return value;
	}
	public void setValue(V newValue) {
		value = newValue;
	}
}
