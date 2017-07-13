package mines.util;

import java.util.Arrays;

/**
 * An array-based list for primitive int.
 */
public class IntList {

	private int[] array;	//the list.
	private int size;		//the number of elements in the list.

	/**
	 * Create a new list.
	 */
	public IntList() {
		array = new int[11];
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
	public void add(int e) {
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
	 * Get whether the list is empty.
	 *
	 * @return	true if the size is 0,
	 *			false otherwise.
	 */
	public boolean isEmpty() {
		return (size == 0);
	}

	/**
	 * Get an element from the list.
	 *
	 * @param	ind	the index of the element.
	 * @return	the element.
	 * @throws	IndexOutOfBoundException if the index is negative,
	 *			or is greater than the size - 1.
	 */
	public int get(int ind) {
		if (ind >= size) {
			throw new IndexOutOfBoundsException();
		}
		return array[ind];
	}
}