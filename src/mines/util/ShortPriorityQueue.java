package mines.util;

import java.util.*;

/**
 * A priority queue that uses insertion sort.
 * Intended for small numbers of elements.
 */
public class ShortPriorityQueue<E extends Comparable<E>> {

	private LinkedList<E> queue;	//underlying queue.

	/**
	 * Create an empty queue.
	 */
	public ShortPriorityQueue() {
		queue = new LinkedList<>();
	}

	/**
	 * Add an element to the queue in its appropriate position.
	 *
	 * @param	e	the element.
	 */
	public void add(E e) {
		if (queue.isEmpty()) {
			queue.add(e);
		}
		else {
			ListIterator<E> it = queue.listIterator(0);
			while (it.hasNext()) {
				E last = it.next();
				if (e.compareTo(last) <= 0) {
					it.previous();
					it.add(e);
					return;
				}
			}
			it.add(e);
		}
	}

	/**
	 * Get whether the list is empty.
	 *
	 * @return	true if the size is 0,
	 *			false otherwise.
	 */
	public boolean isEmpty() {
		return queue.isEmpty();
	}

	/**
	 * Remove and return the first element in the queue.
	 * 
	 * @return	the front of the queue.
	 * @throws	NoSuchElementException if the queue is empty.
	 */
	public E poll() {
		return queue.poll();
	}

	/**
	 * Remove and return the last element in the queue.
	 * 
	 * @return	the end of the queue.
	 * @throws	NoSuchElementException if the queue is empty.
	 */
	public E pollLast() {
		return queue.pollLast();
	}

	/**
	 * Empty the queue.
	 */
	public void clear() {
		queue.clear();
	}

	/**
	 * Return but don't remove the first element in the queue.
	 * 
	 * @return	the front of the queue.
	 * @throws	NoSuchElementException if the queue is empty.
	 */
	public E peek() {
		return queue.peek();
	}

	/**
	 * Return but don't remove the last element in the queue.
	 * 
	 * @return	the end of the queue.
	 * @throws	NoSuchElementException if the queue is empty.
	 */
	public E peekLast() {
		return queue.peekLast();
	}

	/**
	 * Insert all elements of a collection into this queue.
	 *
	 * @param	c	a Collection.
	 */
	public void addAll(Collection<E> c) {
		for (E e : c) {
			add(e);
		}
	}

}