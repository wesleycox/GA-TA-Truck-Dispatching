package mines.util;

import java.util.Arrays;

/**
 * An array-based list for primitive doubles.
 */
public class DoubleList {

	private double[] array;	//the list.
	private int size;		//the number of elements in the list.

	/**
	 * Create a new list.
	 */
	public DoubleList() {
		array = new double[11];
		size = 0;
	}

	/**
	 * Doubles available space if the list is full.
	 */
	private void ensureSize() {
		if (size == array.length) {
			array = Arrays.copyOf(array,size * 2);
		}
	}

	/**
	 * Append an element to the end of the list.
	 *
	 * @param	e	the element.
	 */
	public void add(double e) {
		ensureSize();
		array[size] = e;
		size++;
	}

	/**
	 * Sort the list into increasing order.
	 */
	public void sort() {
		Arrays.sort(array,0,size);
	}

	/**
	 * Empty the list.
	 */
	public void clear() {
		size = 0;
	}

	/**
	 * Get the number of elements in the list.
	 *
	 * @return	the size.
	 */
	public int size() {
		return size;
	}

	/**
	 * Get an element from the list.
	 *
	 * @param	ind	the index of the element.
	 * @return	the element.
	 * @throws	IndexOutOfBoundException if the index is negative,
	 *			or is greater than the size - 1.
	 */
	public double get(int ind) {
		if (ind >= size) {
			throw new IndexOutOfBoundsException();
		}
		return array[ind];
	}

	/**
	 * Get whether the list is empty.
	 *
	 * @return	true if the size is 0,
	 *			false otherwise.
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Get the an array form of the list.
	 *
	 * @return	an array of length equal to the size.
	 * @see toArray
	 */
	public double[] asArray() {
		return toArray();
	}

	/**
	 * Get the an array form of the list.
	 *
	 * @return	an array of length equal to the size.
	 * @see asArray
	 */
	public double[] toArray() {
		return Arrays.copyOf(array,size);
	}

	/**
	 * Append all elements of a list to the end of this one.
	 *
	 * @param	list	another DoubleList.
	 */
	public void addAll(DoubleList list) {
		for (int i=0; i<list.size; i++) {
			this.add(list.get(i));
		}
	}
}