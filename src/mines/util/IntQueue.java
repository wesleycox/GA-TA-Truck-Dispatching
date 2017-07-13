package mines.util;

import java.util.NoSuchElementException;

/**
 * An array-based double-ended queue for primitive ints.
 */
public class IntQueue {

	private int[] array;	//the queue.
	private int size;		//the number of elements.
	private int front;		//the index of the first element.

	/**
	 * Create an empty queue.
	 */
	public IntQueue() {
		array = new int[11];
		size = 0;
		front = 0;
	}

	/**
	 * Remove and return the first element in the queue.
	 * 
	 * @return	the front of the queue.
	 * @throws	NoSuchElementException if the queue is empty.
	 */
	public int poll() {
		if (size == 0) {
			throw new NoSuchElementException("Queue is empty");
		}
		int out = array[front];
		size--;
		front = (front + 1) % array.length;
		return out;
	}

	/**
	 * Remove and return the last element in the queue.
	 * 
	 * @return	the end of the queue.
	 * @throws	NoSuchElementException if the queue is empty.
	 */
	public int pollLast() {
		if (size == 0) {
			throw new NoSuchElementException("Queue is empty");
		}
		int out = array[(front + size - 1) % array.length];
		size--;
		return out;
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
	 * Return but don't remove the first element in the queue.
	 * 
	 * @return	the front of the queue.
	 * @throws	NoSuchElementException if the queue is empty.
	 */
	public int peek() {
		if (size == 0) {
			throw new NoSuchElementException("Queue is empty");
		}
		return array[front];
	}

	/**
	 * Append an element to the end of the queue.
	 *
	 * @param	e	the element.
	 */
	public void add(int e) {
		ensureSize();
		int end = (front + size) % array.length;
		array[end] = e;
		size++;
	}

	/**
	 * Append an element to the front of the queue.
	 *
	 * @param	e	the element.
	 */
	public void addFront(int e) {
		ensureSize();
		front = (front + array.length - 1) % array.length;
		array[front] = e;
		size++;
	}

	/**
	 * Doubles available space if the queue is full.
	 */
	private void ensureSize() {
		if (size == array.length) {
			int newlength = array.length * 2;
			int[] newarray = new int[newlength];
			for (int i=0; i<size; i++) {
				newarray[i] = array[(front + i) % array.length];
			}
			array = newarray;
			front = 0;
		}
	}

	/**
	 * Empty the queue.
	 */
	public void clear() {
		size = 0;
		front = 0;
	}

	/**
	 * Append all elements of a queue to the end of this one.
	 *
	 * @param	c	another IntQueue.
	 */
	public void addAll(IntQueue c) {
		for (int i=0; i<c.size; i++) {
			add(c.array[(c.front + i) % c.array.length]);
		}
	}

	/**
	 * Get the number of elements in the queue.
	 *
	 * @return	the size.
	 */
	public int size() {
		return size;
	}

	/**
	 * Get an element from the queue.
	 *
	 * @param	ind	the index of the element.
	 * @return	the element.
	 * @throws	IndexOutOfBoundException if the index is negative,
	 *			or is greater than the size - 1.
	 */
	public int get(int index) {
		if (index >= 0 && index < size) {
			return array[(front + index) % array.length];
		}
		else {
			throw new IndexOutOfBoundsException();
		}
	}
}