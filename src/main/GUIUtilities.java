package main;

import java.awt.*;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;


class GUIUtilities
{
	private static final Font guifont = new Font ("Arial", Font.BOLD, 12);
	
	static void applyFont (JPanel p, Font font)
	{
		Component[] comps = p.getComponents ();

		for (Component c : comps)
		{
			c.setFont (font);
		}
	}
	
	static void applyFont (JPanel p)
	{
		Component[] comps = p.getComponents ();

		for (Component c : comps)
		{
			c.setFont (guifont);
		}
	}
	
	static void applyComponentFont (JComponent c)
	{
		c.setFont (guifont);
	}
	
	static void showErrorDialog (Window parent, String error, String title)
	{		
		JOptionPane.showMessageDialog (parent, error, title, JOptionPane.ERROR_MESSAGE);
	}
	
	static void showOKDialog (Window parent, String message, String title)
	{
		JOptionPane.showMessageDialog (parent, message, title, JOptionPane.INFORMATION_MESSAGE);
	}
	
	//returns the window's screen resolution. Index 0 will contain width,
	//index 1 will contain height
	static int[] getResolution ()
	{
		//get the Toolkit of this instance
		Toolkit tk = Toolkit.getDefaultToolkit ();
		
		//get the screen size as Dimension object
		Dimension resolution = tk.getScreenSize ();

		int[] rez = new int[2];

		//extract integers from that Dimension object
		rez[0] = new Double (resolution.getWidth ()).intValue ();
		rez[1] = new Double (resolution.getHeight ()).intValue ();

		return rez;
	}
}