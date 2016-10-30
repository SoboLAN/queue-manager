package simulation;

import java.util.Random;

/** Represents a customer. Every <code>Customer</code> has an ID and a service amount associated with it.
* The IDs will be assigned to each instance of this class in the order of creation starting from 1.
* No more than 2^31 - 1 <code>Customer</code>s may be created during the lifetime of the program without calling
* the <code>resetIDs</code> method. Failure to respect this will trigger a <code>RuntimeException</code>.
* 
* @author Murzea Radu
* 
* @version 1.1
*/
public class Customer implements Comparable<Customer>
{
	//random number generator used for generating service amounts.
	//usable by all Customer objects.
	private static Random rand = new Random (System.currentTimeMillis () / 1000);

	//holds the ID of the customers for continuity.
	private static int IDCounter = 0;

	//the ID of "this" customer
	private int ID;

	//stores the amount of service needed (seconds)
	private int service_needed;

	/** Creates a <code>Customer</code> that needs a service amount in the specified interval. A random amount
	* will be picked between <code>minservice</code> (inclusively) and <code>maxservice</code> (inclusively).
	* Also, each <code>Customer</code> will have a unique ID during the lifetime of the program.
	*
	* @param minservice the minimum service needed. Expressed in seconds.
	* 
	* @param maxservice the maximum service. Expressed in seconds.
	* 
	* @throws IllegalArgumentException if <code>minservice</code> is strictly bigger than <code>maxservice</code>.
	* 
	* @throws RuntimeException if more than 2^31 - 1 <code>Customer</code>s have been created
	* without calling <code>resetIDs</code>.
	*/
	public Customer (int minservice, int maxservice)
	{
		if (minservice > maxservice)
		{
			throw new IllegalArgumentException ("invalid range");
		}

		this.service_needed = rand.nextInt (maxservice - minservice + 1) + minservice;

		if (IDCounter == Integer.MAX_VALUE)
		{
			throw new RuntimeException ("ID too high");
		}

		IDCounter++;
		this.ID = IDCounter;
	}

	/** Resets the ID counter. New <code>Customer</code>s created after calling this will have IDs 1, 2, 3 etc.
	* 
	* @since 1.1
	*/
	public static void resetIDs ()
	{
		IDCounter = 0;
	}

	/** Returns the amount of service needed by this <code>Customer</code>.
	* 
	* @return the amount of service needed. Expressed in seconds.
	* 
	* @since 1.1
	*/
	public final int getAmountOfNeededService ()
	{
		return this.service_needed;
	}

	/** Returns the ID of this <code>Customer</code>.
	* 
	* @return the ID of this <code>Customer</code>.
	* 
	* @since 1.1
	*/
	public final int getID ()
	{
		return this.ID;
	}

	@Override public int hashCode()
	{
		int hash = 5;
		hash = 61 * hash + this.ID;
		hash = 61 * hash + this.service_needed;

		return hash;
	}

	/** Compares this object with another <code>Customer</code> for equality. Two <code>Customer</code>s are considered
	* equal if they have the same ID and the same service need.
	* 
	* @param o the <code>Customer</code> to be compared with.
	* 
	* @return true if the 2 <code>Customer</code>s are equal, false otherwise.
	*/
	@Override public boolean equals (Object o)
	{
		//null object means not equal
		if (o == null)
		{
			return false;
		}
		//if the same object is passed as parameter, no further check is necessary
		else if (o == this)
		{
			return true;
		}

		//check if the parameter is really a Customer so it can be safely downcasted
		if (! getClass ().getName ().equals (o.getClass ().getName ()))
		{
			return false;
		}

		//perform downcast... it should not throw any ClassCastExceptions because of the check above
		Customer c = (Customer) o;

		//if the ID and the amount of needed service are equal, then the objects are equal
		return (this.ID == c.getID () && this.service_needed == c.getAmountOfNeededService ());
	}
	
	/** Compares this <code>Customer</code> with the one provided for equality. Returns a negative integer, zero or
	* positive integer as this <code>Customer</code> is less than, equal or greater than the
	* specified <code>Customer</code>. Note: this class has a natural ordering that is consistent with
	* equals, BUT only for <code>Customer</code>s with the same ID.
	* 
	* @param c the <code>Customer</code> to be compared.
	* 
	* @throws NullPointerException if <code>c</code> is null.
	* 
	* @return a negative integer, zero or positive integer as this <code>Customer</code> is less than,
	* equal or greater than the specified <code>Customer</code>.
	*/
	public int compareTo (Customer c)
	{
		if (c == null)
		{
			throw new NullPointerException ("Customer expected, null provided");
		}
		
		return this.service_needed - c.getAmountOfNeededService ();
	}
}