package simulation;

/** Provides a utility method for transforming the update message received from the <code>Simulator</code> into
* a more user-readable form. This class does not check the messages for validity, so parse the message without
* first modifying it, otherwise unexpected results may be returned.
*
* @author Murzea Radu
* 
* @version 1.0
*/
public final class MessageParser
{
	/** Parses the <code>message</code> to a more user-readable form.
	* 
	* @param message the message to be parsed.
	* 
	* @throws NullPointerException if <code>message</code> is null.
	* 
	* @return the parsed message. If <code>message</code> is an empty
	* <code>String</code>, <code>message</code> is returned.
	* 
	* @since 1.0
	*/
	public static String parse (String message)
	{
		if (message == null)
		{
			throw new NullPointerException ("string expected, null provided");
		}
		else if (message.isEmpty ())
		{
			return message;
		}
		
		String result = "";
		
		if (message.equals ("S|S"))
		{
			result = "Simulation Started";
			
			return result;
		}
		else if (message.equals ("S|F"))
		{
			result = "Simulation Finished Successfully";
			
			return result;
		}
		else if (message.equals ("S|E"))
		{
			result = "Simulation Finished as the Result of an Error";
			
			return result;
		}
		else if (message.equals ("S|X"))
		{
			result = "Simulation was stopped manually.";
		}
		else if (message.equals ("Q|F"))
		{
			result = "All Queues are Full";
			
			return result;
		}
		else if (message.equals ("Q|R"))
		{
			result = "The Customers have reorganized themselves to other Queues";
			
			return result;
		}
		
		//if no matches were made up to this point,
		//then it means we have a more complicated message to deal with
		String[] msgelements = message.split ("\\|");
		
		if (msgelements[0].equals ("Q"))
		{
			if (msgelements[2].equals ("O"))
			{
				result = "Queue " + msgelements[1] + " was opened.";
				
				return result;
			}
			else if (msgelements[2].equals ("C"))
			{
				result = "Queue " + msgelements[1] + " was closed.";
				
				return result;
			}
		}
		
		if (msgelements[0].equals ("C"))
		{
			result = "Customer " + msgelements[1];
			
			if (msgelements[2].equals ("A"))
			{
				result += " has arrived at Queue " + msgelements[3];
			}
			else if (msgelements[2].equals ("L"))
			{
				result += " was served at Queue " + msgelements[3] + " and left.";
			}
		}
		
		return result;
	}
}
