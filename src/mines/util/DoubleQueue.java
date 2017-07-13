package mines.util;

import java.util.NoSuchElementException;

/**
 * An array-based queue for primitive doubles.
 */
public class DoubleQueue {

	private double[] array;	//the queue.
	private int size;		//the number of elements.
	private int front;		//the index of the first element.

	/**
	 * Create an empty queue.
	 */
	public DoubleQueue() {
		array = new double[11];
		size = 0;
		front = 0;
	}

	/**
	 * Remove and return the first element in the queue.
	 * 
	 * @return	the front of the queue.
	 * @throws	NoSuchElementException if the queue is empty.
	 */
	public double poll() {
		if (size == 0) {
			throw new NoSuchElementException("Queue is empty");
		}
		double out = array[front];
		size--;
		front = (front + 1) % array.length;
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
	public double peek() {
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
	public void add(double e) {
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
	public void addFront(double e) {
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
			double[] newarray = new double[newlength];
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
	 * @param	c	another DoubleQueue.
	 */
	public void addAll(DoubleQueue c) {
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
	public double get(int ind) {
		if (ind < 0 || ind >= size) {
			throw new IndexOutOfBoundsException();
		}
		int pos = (front + ind) % array.length;
		return array[pos];
	}
}