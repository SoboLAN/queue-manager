package simulation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/** The simulation engine for the program. Contains all the logic of the simulation.
 * This simulator must be build using the inner SimulatorBuilder class and respecting the
 * Builder design pattern.
 * 
 * Since it is an Observable, it will notify all its observers with messages contained in String
 * objects (the Object parameter in the update method). These messages are as follows (examples):
 * <ul>
 * <li><code>S|S</code> - means simulation has started.</li>
 * <li><code>S|F</code> - means simulation has finished successfully.</li>
 * <li><code>S|X</code> - means simulation was stopped from the outside (by calling stopSimulation).</li>
 * <li><code>S|E</code> - means the simulator encoutered an error and was forced to terminate. To see details
 * about the error, check the simulation log.</li>
 * <li><code>Q|3|O</code> - means the 3rd queue has been opened.</li>
 * <li><code>Q|7|C</code> - means the 7th queue has been closed.</li>
 * <li><code>Q|F</code> - means all queues have reached their maximum capacity. If a new customer arrives while in
 * this state, the simulator will force a simulation shutdown and a "S|E" message will be sent.</li>
 * <li><code>C|27|A|2</code> - means the customer with ID 27 has arrived and was sent to the 2nd queue.</li>
 * <li><code>C|109|L|5</code> - means the customer with ID 109 was served at the 5th queue and left it.</li>
 * <li><code>Q|R</code> - means the customers reorganized themselves to different queues. </li>
 * </ul>
 * <br />
 * Use the method <code>MessageParser.parse</code> to transform the received message into a more user-readable form.
 * Note: The simulator uses it to parse the messages that go into the log.
 * 
 * @author Murzea Radu
 * 
 * @version 1.2
*/
public final class Simulator extends Observable
{
	public static final int DEFAULT_NR_QUEUES = 6;
	public static final int DEFAULT_MAX_QUEUE_SIZE = 10;
	public static final int DEFAULT_NR_CUSTOMERS = 40;
	public static final int DEFAULT_MIN_ARRIVAL = 4;
	public static final int DEFAULT_MAX_ARRIVAL = 8;
	public static final int DEFAULT_MIN_SERVICE = 12;
	public static final int DEFAULT_MAX_SERVICE = 20;
	public static final int DEFAULT_REORGANIZATION = 4;
	
	//storage for the queues
	private Queue[] queues;
	
	private ArrayList<Integer> closerequests;

	//stores the number of queues
	private int nrqueues;

	//the maximum size of the queues
	private int maxqueuesize;

	//stores the number of customers
	private int nrcustomers;

	//minimum arrival interval (seconds)
	private int minarrival;

	//maximum arrival interval (seconds)
	private int maxarrival;

	//minimum serving interval (seconds)
	private int minservice;

	//maximum serving interval (seconds)
	private int maxservice;
	
	private int reorganization;
	
	private long starttime;

	//timer used to to schedule customers arrival
	private Timer arrtimer;

	//timer used to schedule the serving of customers
	private Timer servtimer;
	
	private Timer reorgtimer;

	//statistics for waiting time
	private Statistics stat;
	
	//lock used for synchronization of the queues. only 1 operation
	//at a time (arriving customer, leaving customer etc.)
	private final ReentrantLock _lock = new ReentrantLock ();
	
	//used to write the log to disk
	private BufferedWriter buff;
	
	//error flag. will be set by the TimerTasks to indicate error.
	//the logging system will check for this and write it to the log if it contains any error messages
	private String errormessage = "";

	//construct and set default values (will be used if not changed)
	private Simulator ()
	{
		this.nrqueues = DEFAULT_NR_QUEUES;
		this.maxqueuesize = DEFAULT_MAX_QUEUE_SIZE;
		this.nrcustomers = DEFAULT_NR_CUSTOMERS;
		this.minarrival = DEFAULT_MIN_ARRIVAL;
		this.maxarrival = DEFAULT_MAX_ARRIVAL;
		this.minservice = DEFAULT_MIN_SERVICE;
		this.maxservice = DEFAULT_MAX_SERVICE;
		this.reorganization = DEFAULT_REORGANIZATION;

		this.queues = new Queue[this.nrqueues];
		this.stat = new Statistics (this.nrqueues);
		
		this.closerequests = new ArrayList<Integer> ();

		this.arrtimer = new Timer ();
		this.servtimer = new Timer ();
		this.reorgtimer = new Timer ();
	}
	
	public static SimulatorBuilder createSimulatorBuilder ()
	{
		return new SimulatorBuilder ();
	}
	
	/** Provides functionality for building the Simulator. Get an instance of this class from the Simulator
	* itself, call whichever setters are necessary and finally call the build method to get the Simulator.
	*/
	public static class SimulatorBuilder
	{
		//the simulator
		private final Simulator obj;
		
		//tells if build was called or not
		private boolean done;
		
		private SimulatorBuilder ()
		{
			done = false;
			obj = new Simulator ();
		}
		
		/** Builds the Simulator with the specified parameters and returns it.
		* 
		* @return the Simulator.
		*/
		public Simulator build ()
		{
			this.done = true;
			return this.obj;
		}
		
		/** Sets the number of queues for the Simulator.
		* 
		* @param nrqueues the number of queues in the train station. Accepted values are between
		* 1 and 10.
		* 
		* @throws IllegalArgumentException if the parameter is outside of the allowed range.
		* 
		* @throws IllegalStateException if the build method was already called.
		*/
		public void setNrQueues (int nrqueues)
		{
			this.check ();

			if (nrqueues < 1 || nrqueues > 10)
			{
				throw new IllegalArgumentException ("nr of queues out of range");
			}

			this.obj.nrqueues = nrqueues;
			this.obj.queues = new Queue[nrqueues];
			this.obj.stat = new Statistics (nrqueues);
		}

		/** Sets the maximum number of Customers a Queue can hold.
		* 
		* @param maxqueuesize the maximum number of customers. Accepted values are between 1 and 10.
		* 
		* @throws IllegalArgumentException if the parameter is outside of the allowed range.
		* 
		* @throws IllegalStateException if the build method was already called.
		*/
		public void setMaxQueueSize (int maxqueuesize)
		{
			this.check ();
			
			if (maxqueuesize < 1 || maxqueuesize > 10)
			{
				throw new IllegalArgumentException ("max queue size out of range");
			}
			
			this.obj.maxqueuesize = maxqueuesize;
		}
		 
		/** Sets the number of Customers for which the simulation should run.
		* 
		* @param nrcustomers the number of customers for which to simulate. Any value greater
		* than 0 is accepted.
		* 
		* @throws IllegalArgumentException if the parameter is less than 1.
		* 
		* @throws IllegalStateException if the build method was already called.
		*/
		public void setNrCustomers (int nrcustomers)
		{
			this.check ();
			
			if (nrcustomers < 1)
			{
				throw new IllegalArgumentException ("nr of customers less than 1");
			}
			
			this.obj.nrcustomers = nrcustomers;
		}

		/** Sets the interval at which the Customers arrive.
		* 
		* @param minarrival the minimum arrival interval (seconds).
		* 
		* @param maxarrival the maximum arrival interval (seconds).
		* 
		* @throws IllegalArgumentException if minarrival is less than 1 or bigger than maxarrival.
		* 
		* @throws IllegalStateException if the build method was already called.
		*/
		public void setArrivalInterval (int minarrival, int maxarrival)
		{
			this.check ();
			
			if (minarrival < 1 || minarrival > maxarrival)
			{
				throw new IllegalArgumentException ("arrival out of range");
			}
			
			this.obj.minarrival = minarrival;
			this.obj.maxarrival = maxarrival;
		}

		/** Sets the minimum and maximum service time needed by the Customers.
		* 
		* @param minservice the minimum service needed (seconds).
		* 
		* @param maxservice the maximum service needed (seconds).
		* 
		* @throws IllegalArgumentException if minservice is less than 1 or bigger than maxservice.
		* 
		* @throws IllegalStateException if the build method was already called.
		*/
		public void setServiceAmount (int minservice, int maxservice)
		{
			this.check ();
			
			if (minservice < 1 || minservice > maxservice)
			{
				throw new IllegalArgumentException ("service out of range");
			}
			
			this.obj.minservice = minservice;
			this.obj.maxservice = maxservice;
		}
		
		/** Sets the interval at which the Customers will reorganize themselves to smaller Queues.
		* In the real world, customers do this to minimize waiting time.
		* Note: if the simulation is done on just 1 Queue, this feature will be disabled.
		* 
		* @param reorganization the (fixed) interval of Customers reorganization. Set to 0 or a negative
		* value to disable this feature.
		* 
		* @throws IllegalStateException if the build method was already called.
		*/
		public void setReorganization (int reorganization)
		{
			this.check ();
			this.obj.reorganization = reorganization;
		}
		
		//checks if the simulator was built or not
		private void check ()
		{
			if (this.done)
			{
				throw new IllegalStateException ("simulator already built");
			}
		}
	}
	
	//schedules all arrivals and servings of the customers
	private void scheduleCustomerArrivals ()
	{
		long now = System.currentTimeMillis ();

		//random number generator for scheduling
		Random rand = new Random (now / 1000);

		//schedule each customer to arrive
		for (int i = 0; i < this.nrcustomers; i++)
		{
			now += rand.nextInt (1000 * (this.maxarrival - this.minarrival + 1)) + 1000 * this.minarrival;
			
			arrtimer.schedule (new CustomerArriver (), new Date (now));
		}
	}
	
	private void scheduleReorganizations ()
	{
		if (this.reorganization <= 0)
		{
			return;
		}

		reorgtimer.scheduleAtFixedRate (new CustomerReorganizer (),
										1000 * this.reorganization,
										1000 * this.reorganization);
	}

	/** Starts the simulation.
	*/
	public void simulate ()
	{
		Customer.resetIDs ();
		
		createLogFile ();
		
		//the statistics storage place
		stat = new Statistics (this.nrqueues);

		//schedule arrivals etc.
		scheduleCustomerArrivals ();
		
		scheduleReorganizations ();

		//create and open all necessary queues
		for (int i = 0; i < nrqueues; i++)
		{
			this.queues[i] = new Queue (this.maxqueuesize);
			this.queues[i].open ();
			
			stat.recordEmptyQueue (i, true);
		}
		
		starttime = System.currentTimeMillis ();
		
		logAndNotify ("S|S");
	}
	
	/** Returns the amount of time elapsed from the start of simulation.
	* 
	* @return the elapsed time since the start of the simulation. Expressed in seconds.
	*/
	public int getElapsedTime ()
	{
		int rez = (int) ((System.currentTimeMillis () - starttime) / 1000);
		
		return rez;
	}
	
	private void logAndNotify (String message)
	{
		String currenttime = new SimpleDateFormat ("[K:mm:ss]").format (Calendar.getInstance ().getTime ());

		try
		{
			if (errormessage.length () > 0)
			{
				buff.write (currenttime + " " + errormessage);
				buff.newLine ();
			}
			else
			{	
				buff.write (currenttime + " " + MessageParser.parse (message));
				buff.newLine ();

				setChanged ();
				notifyObservers (message);
			}
		}
		catch (IOException ioe){}
	}
	
	/** Tells if a queue is open or not.
	* 
	* @param nr the queue which to check
	* 
	* @return true if the queue is open, false otherwise.
	*/
	public boolean isOpenQueue (int nr)
	{
		if (nr < 0 || nr >= queues.length)
		{
			throw new IndexOutOfBoundsException ("queue doesnt exist");
		}

		return this.queues[nr].isOpen ();
	}
	
	public int getNrOfQueues ()
	{
		return this.nrqueues;
	}
	
	/** Returns the size of the queue at the specified index.
	* 
	* @param index the location of the queue.
	* 
	* @throws IndexOutOfBoundsException if the queue at location <code>index</code> doesn't exist.
	* 
	* @return the size of the queue
	*/
	public int getQueueSize (int index)
	{
		if (index < 0 || index >= queues.length)
		{
			throw new IndexOutOfBoundsException ("queue doesnt exist");
		}
		
		return this.queues[index].getSize();
	}
	
	/** Opens the required queue. If the queue was scheduled to be closed, that schedule will be cancelled.
	* Calling this (even multiple times) for queues that are already open will have no effect.
	* 
	* @param index the queue which to open.
	* 
	* @throws IndexOutOfBoundsException if <code>index</code> specifies a queue that doesn't exist.
	*/
	public void openQueue (int index)
	{
		//check if queue exists
		if (index < 0 || index >= nrqueues)
		{
			throw new IndexOutOfBoundsException ("parameter out of bounds");
		}

		//see if queue was previously scheduled to be closed. if yes, cancel that
		if (closerequests.contains (new Integer (index)))
		{
			closerequests.remove (new Integer (index));
		}

		//if queue is not open, open it
		if (! queues[index].isOpen ())
		{
			_lock.lock ();

			try
			{
				queues[index].open ();
				
				stat.recordEmptyQueue (index, true);
			}
			finally
			{
				_lock.unlock ();
			}

			logAndNotify ("Q|" + index + "|O");
		}
	}
	
	/** Schedules a queue to be closed as soon as possible. The queue will be closed as soon as there
	* are no more customers waiting to be served by it. Arriving customers are not aware
	* that the queue wants to close. Note: calling this multiple times for a queue will have no effect.
	* 
	* @param index the queue which to close.
	* 
	* @throws IndexOutOfBoundsException if <code>index</code> specifies a queue that doesn't exist.
	*/
	public void scheduleClosing (int index)
	{
		//check if queue exists
		if (index < 0 || index >= nrqueues)
		{
			throw new IndexOutOfBoundsException ("queue doesnt exist");
		}
		
		//if the closing was already scheduled for this queue... do nothing.
		if (closerequests.contains (new Integer (index)))
		{
			return;
		}
		
		//check if queue is empty
		if (queues[index].getSize () == 0)
		{
			_lock.lock ();
			
			try
			{
				//close queue
				queues[index].close ();
				
				stat.recordEmptyQueue (index, false);
			}
			finally
			{
				_lock.unlock ();
			}
			
			//notify observers
			logAndNotify ("Q|" + index + "|C");
		}
		else
		{
			//if queue isn't empty, remember that a client wants it closed
			closerequests.add (new Integer (index));
		}
	}
	
	private void createLogFile ()
	{
		SimpleDateFormat sdf = new SimpleDateFormat ("dd.MMM.yyyy");
		String filename = "simulator." + sdf.format (Calendar.getInstance ().getTime ()) + ".log";
		
		try
		{
			buff = new BufferedWriter (new FileWriter (filename));

			buff.write ("Simulation of Queues Log");
			buff.newLine ();
			buff.newLine ();
			buff.write ("PARAMETERS");
			buff.newLine ();
			buff.write ("--------------");
			buff.newLine ();
			buff.write ("Number of Queues = " + this.nrqueues);
			buff.newLine ();
			buff.write ("Number of Customers = " + this.nrcustomers);
			buff.newLine ();
			buff.write ("Maximum Queue Size = " + this.maxqueuesize);
			buff.newLine ();
			buff.write ("Customers Arrival Interval = [" + this.minarrival + "," + this.maxarrival + "]");
			buff.newLine ();
			buff.write ("Customers Service Need Interval = [" + this.minservice + "," + this.maxservice + "]");
			buff.newLine ();
			buff.write ("Customers Reorganization Period = " + (this.reorganization <= 0 ? "disabled" : this.reorganization));
			buff.newLine ();
			buff.write ("--------------");
			buff.newLine ();
		}
		catch (IOException e) {}
	}

	private void writeStatistics ()
	{
		double avgservice = 0, avgwait = 0;
		double[] qemptytimes = new double[nrqueues];
		
		try
		{
			avgservice = stat.getAverageServiceAmounts (2);
			avgwait = stat.getAverageWaitingTimes (2);

			for (int i = 0; i < nrqueues; i++)
			{
				qemptytimes[i] = stat.getQueueEmptyTime (i, 2);
			}
		}
		catch (IllegalArgumentException e)
		{
			try
			{
				buff.write ("ERROR IN STATISTICS MODULE");
			}
			catch (IOException ioe){}
		}

		try
		{
			buff.newLine ();
			buff.write ("-------------");
			buff.newLine ();
			buff.write ("STATISTICS");
			buff.newLine ();
			buff.write ("-------------");
			buff.newLine ();
			buff.write ("Simulation Running time = " + getElapsedTime () + " seconds");
			buff.newLine ();
			buff.write ("Average Service Need of Customers = " + avgservice);
			buff.newLine ();
			buff.write ("Average Waiting Time of Customers = " + avgwait);
			buff.newLine ();
			
			for (int i = 0; i < nrqueues; i++)
			{
				buff.write ("Total Empty Time of Queue " + i + " = " + qemptytimes[i]);
				buff.newLine ();
			}
		}
		catch (IOException e){}
	}

	//class whose code is executed each time a customer arrives in the train station
	//and wants to go to a queue
	private class CustomerArriver extends TimerTask
	{
		private boolean isQueuesFull ()
		{
			//first, let's check if we have room
			for (int i = 0; i < nrqueues; i++)
			{
				if (queues[i].getSize () < 10)
				{
					return false;
				}
			}
			
			return true;
		}
		
		private int emptiestQueue ()
		{
			int min_size = Integer.MAX_VALUE;
			int min_loc = Integer.MAX_VALUE;

			//determine size of smallest opened queue and its index
			for (int i = 0; i < nrqueues; i++)
			{
				if (queues[i].isOpen ())
				{
					if (queues[i].getSize () < min_size)
					{
						min_size = queues[i].getSize ();
						min_loc = i;
					}
				}
			}
			
			return min_loc;
		}

		//entry point of execution
		public void run ()
		{
			_lock.lock ();
			
			try
			{
				if (isQueuesFull ())
				{
					errormessage = "a new customer arrived, no empty slot was found";
					logAndNotify ("");
					errormessage = "";
					
					//log before stopping, because stop closes the log
					logAndNotify ("S|E");
					
					stop ();
					
					return;
				}

				//the new customer
				Customer cust = new Customer (minservice, maxservice);

				//determine the smallest queue and add customer to it
				int new_location = emptiestQueue ();
				
				if (new_location == Integer.MAX_VALUE)
				{
					throw new IllegalStateException ("no open queue");
				}
				
				if (queues[new_location].getSize () == 0)
				{
					stat.recordEmptyQueue (new_location, false);
				}

				queues[new_location].addCustomer (cust);
				
				logAndNotify ("C|" +
							Integer.toString (cust.getID ()) +
							"|A|" +
							Integer.toString (new_location)
							);

				stat.recordArrivingCustomer (cust);

				//if the customer is the first at the queue, schedule his serving
				if (queues[new_location].getSize () == 1)
				{
					servtimer.schedule(new CustomerServer (new_location),
										new Date (System.currentTimeMillis () + 1000 * cust.getAmountOfNeededService ()));
				}
				
				if (isQueuesFull ())
				{
					logAndNotify ("Q|F");
				}
			}
			catch (Exception e)
			{
				errormessage = e.getMessage ();
				logAndNotify ("");
				errormessage = "";
				logAndNotify ("S|E");
				stop ();
			}
			finally
			{
				_lock.unlock ();
			}
		}
	}

	//class whose code is executed every time a customer gets served and leaves the queue
	private class CustomerServer extends TimerTask
	{
		private int whichqueue;
		
		CustomerServer (int queue)
		{
			this.whichqueue = queue;
		}
		
		//main execution entry point
		public void run ()
		{
			_lock.lock ();
			
			try
			{
				if (queues[this.whichqueue].getSize () == 0)
				{
					throw new IllegalStateException ("no customer at queue " + Integer.toString (this.whichqueue));
				}

				Customer cust = queues[this.whichqueue].getCustomer (0);

				stat.recordLeavingCustomer (cust);
				queues[this.whichqueue].removeFirstCustomer ();

				logAndNotify ("C|" +
								Integer.toString (cust.getID ()) +
								"|L|" +
								Integer.toString (this.whichqueue)
								);

				//if after the customer, there are more customers at the queue,
				//schedule the next customer to be served
				if (queues[this.whichqueue].getSize () > 0)
				{
					cust = queues[this.whichqueue].getCustomer (0);

					servtimer.schedule (new CustomerServer (this.whichqueue),
										new Date (System.currentTimeMillis () + 1000 * cust.getAmountOfNeededService ()));
				}
				
				//if the queue is left empty, record it
				//but not if it's scheduled to be closed
				if (queues[this.whichqueue].getSize () == 0)
				{
					if (closerequests.contains (new Integer (this.whichqueue)))
					{
						queues[this.whichqueue].close ();
						
						stat.recordEmptyQueue (whichqueue, false);
						
						logAndNotify ("Q|" + this.whichqueue + "|C");
					}
					else
					{
						stat.recordEmptyQueue (this.whichqueue, true);
					}
				}
				
				//if all customers have been processed:
				//- stop queue recording
				//- notify the stop
				//- write statistics
				//- stop the simulator.
				if (stat.getNrOfProcessedCustomers () == nrcustomers)
				{
					for (int i = 0; i < nrqueues; i++)
					{
						stat.recordEmptyQueue (i, false);
					}
					
					logAndNotify ("S|F");
					
					writeStatistics ();
					
					stop ();
				}
			}
			catch (Exception e)
			{
				errormessage = e.getMessage ();
				logAndNotify ("");
				errormessage = "";
				logAndNotify ("S|E");
				stop ();
			}
			finally
			{
				_lock.unlock ();
			}
		}
	}
	
	//class whose code is executed when a reorganization is scheduled
	//will move customers from big queues to small queues
	private class CustomerReorganizer extends TimerTask
	{
		//first is size, second is index
		private int[] getMax ()
		{	
			int[] rez = new int[2];
			rez[0] = rez[1] = -1;
			
			for (int i = 0; i < nrqueues; i++)
			{
				if (queues[i].isOpen ())
				{
					if (queues[i].getSize () > rez[0])
					{
						rez[0] = queues[i].getSize ();
						rez[1] = i;
					}
				}
			}
			
			return rez;
		}
		
		//first is size, second is index;
		private int[] getMin ()
		{	
			int[] rez = new int[2];
			rez[0] = rez[1] = maxqueuesize + 1;
			
			for (int i = 0; i < nrqueues; i++)
			{
				if (queues[i].isOpen ())
				{
					if (queues[i].getSize () < rez[0])
					{
						rez[0] = queues[i].getSize ();
						rez[1] = i;
					}
				}
			}
			
			return rez;
		}
		
		private int getNrOfOpenedQueues ()
		{
			int rez = 0;
			
			for (int i = 0; i < nrqueues; i++)
			{
				if (queues[i].isOpen ())
				{
					rez++;
				}
			}
			
			return rez;
		}
		
		public void run ()
		{
			_lock.lock ();
			
			try
			{
				if (getNrOfOpenedQueues () < 2)
				{
					return;
				}
				
				boolean moved = false;
				
				int[] min = getMin ();
				int[] max = getMax ();
				
				while (max[0] - min[0] > 2)
				{
					//get the last customer in the biggest queue
					Customer c = queues[max[1]].getCustomer (max[0] - 1);
					
					//remove the customer
					queues[max[1]].removeLastCustomer ();
					
					//move him to the smallest queue
					queues[min[1]].addCustomer (c);
					
					if (queues[min[1]].getSize () == 1)
					{
						stat.recordEmptyQueue (min[1], false);
					}
					
					//if the queue was empty, adding a new customer to it means also begginning
					//to serve that customer
					if (queues[min[1]].getSize () == 1)
					{
						servtimer.schedule (new CustomerServer (min[1]),
											new Date (System.currentTimeMillis () + 1000 * c.getAmountOfNeededService ()));
					}
					
					moved = true;
					
					//recalculate and repeat
					min = getMin ();
					max = getMax ();
				}
				
				if (moved)
				{
					logAndNotify ("Q|R");
				}
			}
			catch (Exception e)
			{
				errormessage = e.getMessage ();
				logAndNotify ("");
				errormessage = "";
				logAndNotify ("S|E");
				stop ();
			}
			finally
			{
				_lock.unlock ();
			}
		}
	}

	/** Stops the simulation.
	*/
	public void stopSimulation ()
	{
		logAndNotify ("S|X");

		stop ();
	}
	
	//will close the log and cancel everything... it's called stop... doooooh
	private void stop ()
	{
		try
		{
			buff.close ();
		}
		catch (IOException e) {}

		arrtimer.cancel ();
		servtimer.cancel ();
		reorgtimer.cancel ();
		
		arrtimer.purge ();
		servtimer.purge ();
		reorgtimer.purge ();
		
		for (int i = 0; i < nrqueues; i++)
		{
			stat.recordEmptyQueue (i, false);
		}
	}
}
