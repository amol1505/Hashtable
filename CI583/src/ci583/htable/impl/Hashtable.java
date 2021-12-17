package ci583.htable.impl;

/**
 * A HashTable with no deletions allowed. Duplicates overwrite the existing value. Values are of
 * type V and keys are strings -- one extension is to adapt this class to use other types as keys.
 * 
 * The underlying data is stored in the array `arr', and the actual values stored are pairs of 
 * (key, value). This is so that we can detect collisions in the hash function and look for the next 
 * location when necessary.
 */

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



public class Hashtable<V> {

	private Object[] arr; //an array of Pair objects, where each pair contains the key and value stored in the hashtable
	private int max; //the size of arr. This should be a prime number
	private int itemCount; //the number of items stored in arr
	private final double maxLoad = 0.6; //the maximum load factor
	private static final int FNV1_32_INIT = 0x01000193; //initial seed for 32 bit hashes
	private static final int FNV1_PRIME_32 = 16777619; //32 bit prime value

	public static enum PROBE_TYPE {
		LINEAR_PROBE, QUADRATIC_PROBE, DOUBLE_HASH;
	}

	PROBE_TYPE probeType; //the type of probe to use when dealing with collisions
	private final BigInteger DBL_HASH_K = BigInteger.valueOf(8);

	/**
	 * Create a new Hashtable with a given initial capacity and using a given probe type
	 * @param initialCapacity
	 * @param pt
	 */
	public Hashtable(int initialCapacity, PROBE_TYPE pt) {
		itemCount = 0; //no items are present as its just been created so it's set to 0
		max = nextPrime(initialCapacity); //nextPrime returns initialCapacity if its prime or the next prime number if not
		probeType = pt; //setting the probe type to the given type 
		arr = new Object[max]; //creating the array with the size of the prime number
	}
	
	/**
	 * Create a new Hashtable with a given initial capacity and using the default probe type
	 * @param initialCapacity
	 */
	public Hashtable(int initialCapacity) {
		itemCount = 0;
		max = nextPrime(initialCapacity);
		probeType = PROBE_TYPE.LINEAR_PROBE; //sets the probe type to linear probe
		arr = new Object[max];
	}

	/**
	 * Store the value against the given key. If the loadFactor exceeds maxLoad, call the resize 
	 * method to resize the array. the If key already exists then its value should be overwritten.
	 * Create a new Pair item containing the key and value, then use the findEmpty method to find an unoccupied 
	 * position in the array to store the pair. Call findEmmpty with the hashed value of the key as the starting
	 * position for the search, stepNum of zero and the original key.
	 * containing   
	 * @param key
	 * @param value
	 */
	public void put(String key, V value) {
		//call the resize method if the load factor exceeds the max load so that the load factor can be decreased
		if (getLoadFactor() > maxLoad) {
			resize();
		}
		//throw an exception if the key is null so that no null values can be inserted
		if (key == null) {
			 throw new IllegalArgumentException("key cannot be null");
		}
		int hashkey = hash(key); //storing the hashed value of the key
		int position = findEmpty(hashkey, 0, key); //the next empty position using the hashed key as the starting position
		if (arr[position] == null) {
			itemCount++; //increment item count for new keys only as the value would be overwritten if the key was already present
		}
		arr[position] = new Pair(key, value); //stores the key and its associated value
	}

	/**
	 * Get the value associated with key, or return null if key does not exists. Use the find method to search the
	 * array, starting at the hashed value of the key, stepNum of zero and the original key.
	 * @param key
	 * @return
	 */
	public V get(String key) {
		//calls the find method which returns the value associated with the key or null if the key doesnt exist
		return find(hash(key), key, 0);
	}

	/**
	 * Return true if the Hashtable contains this key, false otherwise 
	 * @param key
	 * @return
	 */
	//calls the get function where it returns true if the key contains a value or false otherwise 
	public boolean hasKey(String key) {
		return get(key) != null;
	}

	/**
	 * Return all the keys in this Hashtable as a collection
	 * @return
	 */
	public Collection<String> getKeys() {
		List<String> keys = new ArrayList<>(); //ArrayList created to store the keys
		for (int i = 0; i < max; i++) { //loops through the entire array to go through each index
			if (arr[i] != null) {
				Pair item = (Pair) arr[i];
				keys.add(item.key); //store the key to the ArrayList if the index isn't null
			}
		}
			
		return keys; //returns the keys in the ArrayList

	}

	/**
	 * Return the load factor, which is the ratio of itemCount to max
	 * @return
	 */
	public double getLoadFactor() {
		//casts itemCount as a double so that the result given is a double rather than int
		return ((double) itemCount)/max;
	}

	/**
	 * return the maximum capacity of the Hashtable
	 * @return
	 */
	public int getCapacity() {
		return max;
	}
	
	/**
	 * Find the value stored for this key, starting the search at position startPos in the array. If
	 * the item at position startPos is null, the Hashtable does not contain the value, so return null. 
	 * If the key stored in the pair at position startPos matches the key we're looking for, return the associated 
	 * value. If the key stored in the pair at position startPos does not match the key we're looking for, this
	 * is a hash collision so use the getNextLocation method with an incremented value of stepNum to find 
	 * the next location to search (the way that this is calculated will differ depending on the probe type 
	 * being used). Then use the value of the next location in a recursive call to find.
	 * @param startPos
	 * @param key
	 * @param stepNum
	 * @return
	 */
	private V find(int startPos, String key, int stepNum) {
		Pair item = (Pair) arr[startPos];
		if (item == null) {
			return null; //returns null when the item is null meaning the value isnt present
		}
		else if (item.key.equals(key)) {
			return item.value; //return the associated value if the key matches the key we're looking for
		}
		else {
			int nextPos = getNextLocation(startPos, stepNum++, key); //next position in the array past the starting position
			return find(nextPos, key, stepNum); //recursive call to search for the value with next position in the array
		}
	}

	/**
	 * Find the first unoccupied location where a value associated with key can be stored, starting the
	 * search at position startPos. If startPos is unoccupied, return startPos. Otherwise use the getNextLocation
	 * method with an incremented value of stepNum to find the appropriate next position to check 
	 * (which will differ depending on the probe type being used) and use this in a recursive call to findEmpty.
	 * @param startPos
	 * @param stepNum
	 * @param key
	 * @return
	 */
	private int findEmpty(int startPos, int stepNum, String key) {
		Pair item = (Pair) arr[startPos];
		if (arr[startPos] == null || item.key.equals(key)) {
			return startPos; //return the starting position if its null as its the first unoccupied location or matches the key
		}
		else {
			int nextPos = getNextLocation(startPos, stepNum++, key);
			return findEmpty(nextPos, stepNum, key); //recursive call to find the next empty position using the next position of the array
		}
	}

	/**
	 * Finds the next position in the Hashtable array starting at position startPos. If the linear
	 * probe is being used, we just increment startPos. If the double hash probe type is being used, 
	 * add the double hashed value of the key to startPos. If the quadratic probe is being used, add
	 * the square of the step number to startPos.
	 * @param i
	 * @param stepNum
	 * @param key
	 * @return
	 */
	private int getNextLocation(int startPos, int stepNum, String key) {
		int step = startPos;
		switch (probeType) {
		case LINEAR_PROBE:
			step++;
			break;
		case DOUBLE_HASH:
			step += doubleHash(key);
			break;
		case QUADRATIC_PROBE:
			step += stepNum * stepNum;
			break;
		default:
			break;
		}
		return step % max;
	}

	/**
	 * A secondary hash function which returns a small value (less than or equal to DBL_HASH_K)
	 * to probe the next location if the double hash probe type is being used
	 * @param key
	 * @return
	 */
	private int doubleHash(String key) {
		BigInteger hashVal = BigInteger.valueOf(key.charAt(0) - 96);
		for (int i = 0; i < key.length(); i++) {
			BigInteger c = BigInteger.valueOf(key.charAt(i) - 96);
			hashVal = hashVal.multiply(BigInteger.valueOf(27)).add(c);
		}
		return DBL_HASH_K.subtract(hashVal.mod(DBL_HASH_K)).intValue();
	}

	/**
	 * Return an int value calculated by hashing the key. See the lecture slides for information
	 * on creating hash functions. The return value should be less than max, the maximum capacity 
	 * of the array
	 * @param key
	 * @return
	 */
	//Fowler-Noll-Vo (FNV-1a) hash function, https://github.com/prasanthj/hasher/blob/master/src/main/java/hasher/FNV1a.java
	private int hash(String key) {
		final byte[] keyBytes = key.getBytes(); //key is encodes the string into a byte array using the default charset
		int hash = FNV1_32_INIT; //hash set to the seed for 32 bit hashes
		
		for (int i=0; i< keyBytes.length; i++) {
			hash ^= (keyBytes[i] & 0xff); //XOR the hash with the byte which is split into 0xff octets
			hash *= FNV1_PRIME_32; //hash is multiplied with the 32 bit prime value
			
		}
		return Math.abs(hash) % max; //return the hash value as an absolute number so negative integers arent returned and undergoes modulus as it needs to be less than the capacity of the array
	}

	/**
	 * Return true if n is prime
	 * @param n
	 * @return
	 */
	private boolean isPrime(int n) {
		if (n <= 2) {
			return true; //return true as two is a prime number
		}
		else if (n % 2==0) {
			return false; //return false as even numbers cannot be prime 
		}
		else {
			for (int i = 3; i*i <= n; i+=2) { //loops through odd numbers beginning from three
				if (n % i == 0) { 
					return false; //return false as its not prime if there is no remainder when dividing n by the odd number 
				}	
			}
		}
		return true; //return true if other scenarios are not met
	}

	/**
	 * Get the smallest prime number which is larger than n
	 * @param n
	 * @return
	 */
	private int nextPrime(int n) {
		if (n%2==0) {
			n++; //increment n to make it an even number as it's currently not prime
		}
		else if (isPrime(n)) {
			return n; //return n as it is prime
		}
		return nextPrime (n+=2); //recursive call with another odd number if other conditions not met
	}

	/**
	 * Resize the hashtable, to be used when the load factor exceeds maxLoad. The new size of
	 * the underlying array should be the smallest prime number which is at least twice the size
	 * of the old array.
	 */
	private void resize() {
		max = nextPrime(arr.length*2); //altering the capacity to double its previous capacity if its prime, or the next prime value if not so that load factor is decreased
		Object[] oldarr = arr; //saving the old array to oldarr 
		arr = new Object[max]; //creating a new array with the new capacity
		itemCount=0; //resetting the item count as there's currently no new items in the new array
		for(int i =0; i < oldarr.length; i++){ //looping through each index of the old array
			if(oldarr[i] != null){
				Pair item = (Pair) oldarr[i];
				put(item.key, item.value); //storing the items that aren't null from the old array into the new one
			}
		}
	}

	
	/**
	 * Instances of Pair are stored in the underlying array. We can't just store
	 * the value because we need to check the original key in the case of collisions.
	 * @author jb259
	 *
	 */
	private class Pair {
		private String key;
		private V value;

		public Pair(String key, V value) {
			this.key = key;
			this.value = value;
		}
	}

}