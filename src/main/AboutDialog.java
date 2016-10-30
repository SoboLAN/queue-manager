package main;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;


class AboutDialog
{
	private JFrame parent;
	
	private JDialog aboutdialog;
	
	AboutDialog (JFrame parent)
	{
		this.parent = parent;
	}
	
	//creates the about box (a JPanel with an image, some text and an OK button)
	private JPanel createAboutBox ()
	{
		//create the panel with no layout
		JPanel panel = new JPanel ();
		panel.setLayout (null);

		//label which contains an image. create, position and add
		JLabel aboutinfo = new JLabel (new ImageIcon (getClass ().getResource ("images/info.png")));
		aboutinfo.setBounds (10, 10, 125, 125);
		panel.add (aboutinfo);

		//label with name of program. create, position and add
		JLabel aboutname = new JLabel ("Queue Manager");
		aboutname.setBounds (160, 10, 300, 40);
		panel.add (aboutname);

		//label with version of program. create, position and add
		JLabel aboutversion = new JLabel ("Version: 1.3 build 216");
		aboutversion.setBounds (160, 60, 170, 20);
		panel.add (aboutversion);

		//label with author name. create, position and add
		JLabel aboutauthor = new JLabel ("Author: Murzea Radu (sobolanx@gmail.com)");
		aboutauthor.setBounds (160, 90, 260, 20);
		panel.add (aboutauthor);

		//label with build. create, position and add
		JLabel aboutdate = new JLabel ("Build Date: 13 April 2012");
		aboutdate.setBounds (160, 120, 170, 20);
		panel.add (aboutdate);

		//character that represents the copyright sign.
		//used here to avoid issues from compilers/obfuscators/etc.
		//used in the next label (see below)
		char copyright_char = '\u00A9';
		
		//label for copyright. create, position and add
		JLabel aboutcopyright = new JLabel ("JavaFling " + copyright_char + " 2012. All rights reserved.");
		aboutcopyright.setBounds (25, 150, 250, 20);
		panel.add (aboutcopyright);

		//close button. create, position and add.
		JButton aboutclose = new JButton ("OK");
		aboutclose.setBounds (185, 180, 70, 30);
		panel.add (aboutclose);

		//when the close button is pressed, the dialog is disposed
		aboutclose.addActionListener (new ActionListener ()
		{
			@Override public void actionPerformed (ActionEvent a)
			{
				aboutdialog.dispose ();
			}
		});

		GUIUtilities.applyFont (panel);

		//but the name of the program must be bigger
		aboutname.setFont (new Font ("Verdana", Font.BOLD, 24));
		
		return panel;
	}
	
	void display ()
	{
		final int DWIDTH = 450;
		final int DHEIGHT = 250;
		
		aboutdialog = new JDialog (parent, "About", true);
		aboutdialog.setSize (DWIDTH, DHEIGHT);
		aboutdialog.setResizable (false);
		aboutdialog.setDefaultCloseOperation (JDialog.DISPOSE_ON_CLOSE);
		
		aboutdialog.setContentPane (createAboutBox ());

		aboutdialog.setLocationRelativeTo (parent);
		aboutdialog.setVisible (true);
	}
}
