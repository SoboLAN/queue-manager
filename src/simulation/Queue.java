package simulation;

import java.util.ArrayList;

/** This class represents a queue. It can be opened and closed. <code>Customer</code>s can be
* added and removed. The add operation runs in constant time (O (1)), the remove operations run in linear time (O (n)).
* 
* @author Murzea Radu
* 
* @version 1.1
*/
public class Queue
{
	//place holder for the customers
	private ArrayList<Customer> customers;

	//specifies if the queue is opened or not.
	private boolean opened;

	//specifies the capacity of the queue
	private int maxcustomers;

	/** Create a <code>Queue</code>. If the <code>Queue</code> is full, <code>Customer</code>s must be
	* removed in order to add new ones. The <code>Queue</code> is initially closed.
	*
	* @param maxcustomers capacity of the queue. 
	* 
	* @throws IllegalArgumentException if <code>maxcustomers</code> is less than 1.
	*/
	public Queue (int maxcustomers)
	{
		if (maxcustomers < 1)
		{
			throw new IllegalArgumentException ("parameter is less than 1.");
		}
		
		this.maxcustomers = maxcustomers;
		customers = new ArrayList<Customer> ();

		//initially the queue is closed.
		opened = false;
	}

	/** Opens the <code>Queue</code>.
	* 
	* @since 1.0
	*/
	public final void open ()
	{
		opened = true;
	}

	/** Closes the <code>Queue</code>. If the <code>Queue</code> is already closed, nothing happens.
	*
	* @throws IllegalStateException if the <code>Queue</code> still contains <code>Customer</code>s.
	* 
	* @since 1.0
	*/
	public final void close ()
	{
		if (customers.size () > 0)
		{
			throw new IllegalStateException ();
		}
		else
		{
			opened = false;
		}
	}

	/** Tells if the <code>Queue</code> is open or not.
	*
	* @return true if the <code>Queue</code> is open, false otherwise.
	* 
	* @since 1.0
	*/
	public final boolean isOpen ()
	{
		return opened;
	}

	/** Returns the size of the <code>Queue</code>.
	*
	* @return the number of <code>Customers</code> in the <code>Queue</code>.
	* 
	* @since 1.0
	*/
	public final int getSize ()
	{
		return customers.size ();
	}

	/** Returns the maximum capacity of the <code>Queue</code>.
	*
	* @return the capacity of the <code>Queue</code>.
	* 
	* @since 1.0
	*/
	public final int getMaxSize ()
	{
		return maxcustomers;
	}

	/** Adds a <code>Customer</code> to the back of the <code>Queue</code>.
	*
	* @param a the <code>Customer</code> to be added.
	*
	* @throws IllegalStateException if the <code>Queue</code> is closed or if the <code>Queue</code> is full.
	* 
	* @throws NullPointerException if a is null.
	* 
	* @since 1.0
	*/
	public final void addCustomer (Customer a)
	{
		if (opened == false)
		{
			throw new IllegalStateException ("queue is closed");
		}
		else if (customers.size () == maxcustomers)
		{
			throw new IllegalStateException ("queue is full");
		}
		else if (a == null)
		{
			throw new NullPointerException ("customer expected, null provided");
		}

		customers.add (a);
	}

	/** Removes the <code>Customer</code> from the head of the <code>Queue</code>.
	*
	* @throws IllegalStateException if the <code>Queue</code> has no <code>Customer</code>s.
	* 
	* @since 1.0
	*/
	public final void removeFirstCustomer ()
	{
		if (customers.isEmpty ())
		{
			throw new IllegalStateException ("no customers in queue");
		}
		else
		{
			customers.remove (0);
		}
	}
	
	/** Removes the <code>Customer</code> from the tail of the <code>Queue</code>.
	*
	* @throws IllegalStateException if the <code>Queue</code> has no <code>Customer</code>s.
	* 
	* @since 1.1
	*/
	public final void removeLastCustomer ()
	{
		if (customers.isEmpty ())
		{
			throw new IllegalStateException ("no customers in queue");
		}
		else
		{
			customers.remove (customers.size () - 1);
		}
	}

	/** Returns the <code>Customer</code> at location a.
	*
	* @param a the index of the desired <code>Customer</code>. Head is 0, then 1 and so on.
	*
	* @return The <code>Customer</code> at location a.
	*
	* @throws IndeOutOfBoundsException if there is no <code>Customer</code> at location a.
	* 
	* @since 1.0
	*/
	public final Customer getCustomer (int a)
	{
		if (a < 0 || a >= customers.size ())
		{
			throw new IndexOutOfBoundsException ("there is no customer at index a.");
		}
		else
		{
			return customers.get (a);
		}
	}
}