package main;

import com.jtattoo.plaf.aluminium.AluminiumLookAndFeel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;

/** Contains the main () thread. */
public final class QueueManager
{
	//specifies minimum major version. Examples: 5 (JRE 5), 6 (JRE 6), 7 (JRE 7) etc.
	private static final int MAJOR_VERSION = 6;
	
	//specifies minimum minor version. Examples: 12 (JRE 6u12), 23 (JRE 6u23), 2 (JRE 7u2) etc.
	private static final int MINOR_VERSION = 18;

	//returns true if the user's screen resolution is big enough to display
	//the program's window
	private static boolean isOKScreenResolution ()
	{
		//get the resolution (index 0: width, index 1: height)
		int[] resolution = GUIUtilities.getResolution ();

		//check if the size of the window would fit on the screen
		return (GUI.GUI_WIDTH < resolution[0] && GUI.GUI_HEIGHT < resolution[1]);
	}
	
	//checks if the version of the currently running JVM is bigger than
	//the minimum version required to run this program.
	//returns true if it's ok, false otherwise
	private static boolean checkVersion ()
	{
		//get the JVM version
		String version = System.getProperty ("java.version");

		//extract the major version from it
		int sys_major_version = Integer.parseInt (String.valueOf (version.charAt (2)));
		
		//if the major version is too low (unlikely !!), it's not good
		if (sys_major_version < MAJOR_VERSION)
		{
			return false;
		}
		else if (sys_major_version > MAJOR_VERSION)
		{
			return true;
		}
		else
		{
			//find the underline ( "_" ) in the version string
			int underlinepos = version.lastIndexOf ("_");

			int mv;

			try
			{
				//everything after the underline is the minor version.
				//extract that
				mv = Integer.parseInt (version.substring (underlinepos + 1));
			}
			//if it's not ok, then the version is probably not good
			catch (NumberFormatException e)
			{
				return false;
			}

			//if the minor version passes, wonderful
			return (mv >= MINOR_VERSION);
		}
	}
	
	//displays an error dialog on the screen using a temporary
	//jframe as parent and disposes that jframe when the dialog is
	//closed (will exit application if no other non-daemon threads are running (like another JFrame))
	//parameters: title (title of the dialog window), message (the error message)
	private static void displayErrorDialog (String title, String message)
	{
		//create temporary jframe
		JFrame invisibleparentframe = new JFrame ();
		
		//make it invisible, since we don't need it
		invisibleparentframe.setVisible (false);

		//display the error message
		GUIUtilities.showErrorDialog (invisibleparentframe, message, title);

		//dispose of the parent frame
		invisibleparentframe.dispose ();
	}

	/** The main () thread. */
	public static void main (String[] args)
	{
		//check if the minimum version is ok
		if (! checkVersion ())
		{
			String title = "Queue Manager Minimum Version Error";
			String message = "JVM version detected: " + System.getProperty ("java.version") + ". Minimum version required: " + MAJOR_VERSION + " Update " + MINOR_VERSION + ".";
		
			//display an error message
			displayErrorDialog (title, message);
			
			return;
		}
		
		//check if the screen resolution is OK
		if (! isOKScreenResolution ())
		{
			String title = "Queue Manager Minimum Resolution Error";
			String message = "Minimum resolution required: Width = " + GUI.GUI_WIDTH + ", Height: " + GUI.GUI_HEIGHT + ".";

			//display an error message
			displayErrorDialog (title, message);
			
			return;
		}
		
		try
		{
			UIManager.setLookAndFeel (new AluminiumLookAndFeel ());
		}
		catch (Exception e)
		{
			System.exit (29);
		}

		//everything that happens on the GUI should run
		//on swing's event dispatch thread
		javax.swing.SwingUtilities.invokeLater (new Runnable ()
		{
			@Override public void run ()
			{
				//let the look-and-feel change every element of the window (like buttons, edges etc.),
				//not just what it contains.
				JFrame.setDefaultLookAndFeelDecorated (true);
				JDialog.setDefaultLookAndFeelDecorated (true);
	
				//create the GUI
				GUI f = GUI.createGUI ();

				//get the resolution of the window
				int[] resolution = GUIUtilities.getResolution ();

				//find the required coordinates
				int xlocation = (resolution[0] - GUI.GUI_WIDTH) / 2;
				int ylocation = (resolution[1] - GUI.GUI_HEIGHT) / 2;
					
				//set the window in the middle of the screen
				f.setLocation (xlocation, ylocation);

				//make it visible
				f.setVisible (true);
			}
		});
	}
}