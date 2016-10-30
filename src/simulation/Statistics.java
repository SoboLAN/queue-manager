package simulation;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

/** This class is used to store statistics about the results of the simulation. This class is thread-safe.
* Multiple threads can call methods for recording or retrieving results without any worry about external
* synchronization.
* 
* @author Murzea Radu
* 
* @version 1.2
*/
public final class Statistics implements Serializable
{
	//stores the waiting time of the customers
	private ArrayList<Long> waittimes;

	//id useful for serializable interface
	private static final long serialVersionUID = 3787936803099968485L;
	
	//temporarily stores customer records.
	//added when the customer is recorded to arrive
	//removed when a customer leaves the queue
	private ArrayList<CustomerRecord> passingcustomers;

	//stores the service amounts needed by the customers
	private ArrayList<Integer> servicetimes;
	
	//holds queue records, more specifically how much time the queues stayed open and empty
	private QueueRecord[] qrecords;
	
	//lock for customer related methods
	private transient ReentrantLock lock_c = new ReentrantLock ();
	
	//lock for queue related methods
	private transient ReentrantLock lock_q = new ReentrantLock ();
	
	/** Creates a <code>Statistics</code> object.
	*
	* @param nrqueues specifies for how many <code>Queue</code>s the statistics should be recorded.
	*/
    public Statistics (int nrqueues)
    {
		//initialize fields
		waittimes = new ArrayList<Long> ();
		servicetimes = new ArrayList<Integer> ();
		passingcustomers = new ArrayList<CustomerRecord> ();
		qrecords = new QueueRecord[nrqueues];
		
		//queue records must be created
		for (int i = 0; i < nrqueues; i++)
		{
			qrecords[i] = new QueueRecord ();
		}
    }
	
	/** Returns the number of <code>Customer</code>s this object has processed. A processed
	* <code>Customer</code> is a <code>Customer</code> that has been recorded to both arrive and leave
	* a <code>Queue</code> (not necessarily the same <code>Queue</code>).
	* 
	* @return the number of processed <code>Customer</code>s.
	* 
	* @since 1.0
	*/
	public int getNrOfProcessedCustomers ()
	{
		return waittimes.size ();
	}

	/** Records a <code>Customer</code> that arrives at a <code>Queue</code>.
	* 
	* @param c the <code>Customer</code>.
	* 
	* @throws NullPointerException if c is null.
	* 
	* @throws IllegalArgumentException if the <code>Customer</code> was already recorded to be
	* arrived, but not to leave.
	* 
	* @since 1.2
	*/
	public void recordArrivingCustomer (Customer c)
	{
		if (c == null)
		{
			throw new NullPointerException ("expected Customer, null provided");
		}
		
		//acquire lock because of the iterator below
		lock_c.lock ();
		
		try
		{
			//if the customer was already recorded to arrive.... well, sorry.
			for (Iterator<CustomerRecord> it = passingcustomers.iterator (); it.hasNext ();)
			{
				if (it.next ().getCustomer ().equals (c))
				{
					throw new IllegalArgumentException ("customer already recorded as arrived");
				}
			}

			//create a record for the customer
			CustomerRecord cr = new CustomerRecord (c);
		
			//add customer record to temporary collection
			passingcustomers.add (cr);
		}
		finally
		{
			lock_c.unlock ();
		}
	}
	
	/** Records a <code>Customer</code> that was served and left the <code>Queue</code>.
	* 
	* @param c the <code>Customer</code>.
	* 
	* @throws NullPointerException if <code>c</code> is null.
	* 
	* @throws IllegalArgumentException if the arrival of the <code>Customer</code> was not
	* previously recorded.
	* 
	* @since 1.2
	*/
	public void recordLeavingCustomer (Customer c)
	{
		if (c == null)
		{
			throw new NullPointerException ("expected Customer, null provided");
		}
		
		//since the code below uses an iterator, a lock should be first acquired.
		lock_c.lock ();

		boolean found = false;

		try
		{
			//get all the customers that were recorded to arrive but not to leave
			for (Iterator<CustomerRecord> it = passingcustomers.iterator (); it.hasNext ();)
			{
				CustomerRecord cr = it.next ();

				//if the customer is in the collection,
				//then it means the customer was recorded to arrive
				if (! cr.getCustomer ().equals (c))
				{
					continue;
				}
				
				//record the waiting time of the customer
				//the service time must be subtracted to reflect the real waiting time
				waittimes.add (new Long (System.currentTimeMillis () -
										cr.getArrivalTime ()) -
										1000 * cr.getCustomer ().getAmountOfNeededService ());

				//record the service time
				servicetimes.add (new Integer (cr.getCustomer ().getAmountOfNeededService ()));

				//everything is done, we don't need this customer anymore
				passingcustomers.remove (cr);

				found = true;

				break;
			}
		}
		finally
		{
			lock_c.unlock ();
		}
		
		//if the loop was not terminated by the break, then it means the collection
		//doesn't contain that particular record. which the means the customer was not
		//previously recorded.
		if (! found)
		{
			throw new IllegalArgumentException ("customer arrival was not recorded");
		}
	}
	
	/** Starts or stops recording how much time a <code>Queue</code> remained open but with no <code>Customer</code>s.
	* If empty is specified to be true, recording starts. If set to false, recording stops.
	* 
	* @param queueid the <code>Queue</code> for which to record.
	* 
	* @param empty specifies if the recording should start or stop.
	* 
	* @throws IndexOutOfBoundsException if <code>queueid</code> specifies a <code>Queue</code> that doesn't exist.
	* 
	* @since 1.2
	*/
	public void recordEmptyQueue (int queueid, boolean empty)
	{
		if (queueid < 0 || queueid >= qrecords.length)
		{
			throw new IndexOutOfBoundsException ("queue id out of bounds");
		}
		
		lock_q.lock ();
		
		try
		{
			if (empty)
			{
				qrecords[queueid].setEmpty ();
			}
			else
			{
				qrecords[queueid].setNotEmpty ();
			}
		}
		finally
		{
			lock_q.unlock ();
		}
	}
	
	/** Calculates and returns the average service amounts needed by the <code>Customers</code>.
	* Only <code>Customer</code>s that were recorded to both arrive and leave are taken into consideration
	* for this.
	* 
	* @param decimalplaces the number of decimal places the result should have. Accepted values are
	* between 0 and 3 inclusively.
	* 
	* @throws IllegalArgumentException if <code>decimalplaces</code> is out of the specified bounds.
	* 
	* @return the average service amounts needed by the <code>Customer</code>s. The value is expressed in seconds.
	* 
	* @since 1.2
	*/
	public double getAverageServiceAmounts (int decimalplaces)
	{
		if (decimalplaces < 0 || decimalplaces > 3)
		{
			throw new IllegalArgumentException ("parameter out of bounds");
		}
		
		double avg = 0;

		lock_c.lock ();
		
		try
		{
			//add all service times together
			for (Iterator<Integer> it = servicetimes.iterator (); it.hasNext ();)
			{
				Integer x = it.next ();
				avg += x;
			}
		}
		finally
		{
			lock_c.unlock ();
		}
		
		//nr of elements in the collection
		int nr = servicetimes.size ();

		//calculate the average, but protect against divisions by 0
		avg = (nr == 0) ? 0 : 1.0 * avg / nr;
		
		//format the result as requested
		DecimalFormat df = new DecimalFormat ();
		df.setMaximumFractionDigits (decimalplaces);
		
		String r = df.format (avg);
		
		return Double.parseDouble (r);
	}
	
	/** Calculates and returns the average time the <code>Customer</code>s have spent waiting
	* in <code>Queue</code> before it was their turn to be served.
	* Only <code>Customers</code> that were recorded to both arrive and leave are taken into consideration
	* for this.
	* 
	* @param decimalplaces the number of decimal places the result should have. Accepted values are
	* between 0 and 3 inclusively.
	* 
	* @throws IllegalArgumentException if <code>decimalplaces</code> is out of the specified bounds.
	* 
	* @return the average service amounts needed by the <code>Customers</code>. The value is expressed in seconds.
	* 
	* @since 1.2
	*/
	public double getAverageWaitingTimes (int decimalplaces)
	{
		if (decimalplaces < 0 || decimalplaces > 3)
		{
			throw new IllegalArgumentException ("parameter out of bounds");
		}

		double avg = 0;

		lock_c.lock ();

		try
		{
			for (Iterator<Long> it = waittimes.iterator (); it.hasNext ();)
			{
				Long x = it.next ();
				avg += x / 1000.0;
			}
		}
		finally
		{
			lock_c.unlock ();
		}
		
		//nr of elements in the collection
		int nr = waittimes.size ();

		//protect against divisions by 0
		avg = (nr == 0) ? 0 : 1.0 * avg / nr;
		
		DecimalFormat df = new DecimalFormat ();
		df.setMaximumFractionDigits (decimalplaces);
		
		String r = df.format (avg);
		
		return Double.parseDouble (r);
	}
	
	/** Calculates the total recorded time a <code>Queue</code> has stayed opened but served no <code>Customer</code>s.
	* Note that uncomitted time will not be considered. This means that, if <code>recordEmptyQueue (index, true)</code>
	* was called for that particular <code>Queue</code>, then, in order to have an accurate result, you should do
	* the following:<pre>
	* <code>statistics.recordEmptyQueue (index, false); //commit the time<br />
	* double result = statistics.getQueueEmptyTime (index, 2); //get the result<br />
	* statistics.recordEmptyQueue (index, true); // (optional) move back to original state and keep recording<br />
	* </code></pre>
	* 
	* @param queue the <code>Queue</code> for which the time should be calculated.
	* 
	* @param decimalplaces the number of decimal places the result should have. Accepted values are
	* between 0 and 3 inclusively.
	* 
	* @throws IndexOutOfBoundsException if <code>queue</code> specifies a <code>Queue</code> that
	* doesn't exist
	* 
	* @throws IllegalArgumentException if <code>decimalplaces</code> has a value outside the accepted range.
	* 
	* @return the total time the <code>Queue</code> has stayed open without serving any <code>Customer</code>s.
	* Expressed in seconds.
	* 
	* @since 1.2
	*/
	public double getQueueEmptyTime (int queue, int decimalplaces)
	{
		if (queue < 0 || queue >= qrecords.length)
		{
			throw new IndexOutOfBoundsException ("queue out of bounds");
		}
		else if (decimalplaces < 0 || decimalplaces > 3)
		{
			throw new IllegalArgumentException ("decimalplaces out of bounds");
		}
		
		double val;
		
		lock_q.lock ();
		
		try
		{
			val = qrecords[queue].getEmptyTime ();
		}
		finally
		{
			lock_q.unlock ();
		}
		
		DecimalFormat df = new DecimalFormat ();
		df.setMaximumFractionDigits (decimalplaces);
		
		String r = df.format (val);
		
		return Double.parseDouble (r);
	}

	//class used to store arrival times for each customer
	private class CustomerRecord
	{
		private Customer c;
		private long arrivaltime;
		
		CustomerRecord (Customer c)
		{
			this.c = c;
			this.arrivaltime = System.currentTimeMillis ();
		}
		
		Customer getCustomer ()
		{
			return c;
		}
		
		long getArrivalTime ()
		{
			return arrivaltime;
		}
	}
	
	//class used to store empty times for each queue
	private class QueueRecord
	{
		private long emptytimestamp;
		private long emptytime;
		
		private boolean empty;
		
		QueueRecord ()
		{
			this.empty = false;
			this.emptytimestamp = 0;
			this.emptytime = 0;
		}
		
		void setEmpty ()
		{
			if (empty)
			{
				return;
			}

			this.emptytimestamp = System.currentTimeMillis ();
			this.empty = true;
		}
		
		void setNotEmpty ()
		{
			if (! empty)
			{
				return;
			}
	
			this.empty = false;
			
			this.emptytime += (System.currentTimeMillis () - this.emptytimestamp);
			
			this.emptytimestamp = 0;
		}
		
		double getEmptyTime ()
		{
			return this.emptytime / 1000.0;
		}
	}
}