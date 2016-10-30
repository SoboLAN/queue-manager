package main;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Observable;
import java.util.Observer;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import simulation.MessageParser;
import simulation.Simulator;

/** The GUI of the program.
*/
public final class GUI implements Observer
{
	//the main frame
	private JFrame mainframe;
	
	/** Stores the width of the GUI.
	*/
	public static final int GUI_WIDTH = 910;
	
	/** Stores the height of the GUI.
	*/
	public static final int GUI_HEIGHT = 740;
	
	//stores instance to this object. part of singleton implementation
	private static GUI _instance;

	//maximum capacity of the queues.
	private final int MAX_QUEUE_SIZE = 10;
	
	//maximum number of queues
	private final int MAX_QUEUES = 10;

	//the panels of the GUI
	private JPanel inputpanel, queuespanel, eventspanel, buttonspanel;

	//textfields for user input
	private JTextField txtnrqueues, txtnrcust, txtminarr, txtmaxarr, txtminser, txtmaxser, txtreorg;

	//buttons for controling the program
	private JButton startsim, stopsim, openqueue, closequeue, showgraphbutton;
	
	//drop box used for selecting which queue explicitly to close or open
	private JComboBox cbox;

	//"events" will be displayed here
	private JTextArea txta;

	//these labels will help display the queues
	private JLabel queuelabels[][];

	//the simulation engine
	private Simulator simulator;
	
	private boolean isRunning;
	
	private XYSeries[] series;
	
	/** Creates a GUI object and returns it. Subsequent call of this method will return the same object.
	* 
	* @return the GUI.
	*/
	public static GUI createGUI ()
	{
		if (_instance == null)
		{
			_instance = new GUI ();
		}
		
		return _instance;
	}

	// creates the GUI.
	private GUI ()
	{
		isRunning = false;
		
		mainframe = new JFrame ("Queue Manager");
		
		//set the size.
		mainframe.setSize (GUI_WIDTH, GUI_HEIGHT);

		//by resizing, it doesn't look so good. so let's disable it.
		mainframe.setResizable (false);

		//absolute positioning of the panels will be used.
		mainframe.setLayout (null);
		
		//a custom window listener will be used for closing, so set to "do nothing"
		mainframe.setDefaultCloseOperation (JFrame.DO_NOTHING_ON_CLOSE);
		
		//icon of the program
		mainframe.setIconImage (Toolkit.getDefaultToolkit ().getImage (getClass ().getResource ("images/qmicon.jpg")));
		
		//set the custom window listener
		mainframe.addWindowListener (new MyCloseListener ());
		
		//set the menu bar
		mainframe.setJMenuBar (createMenuBar ());
		
		//create the input panel and add to the frame
		inputpanel = createInputPanel ();
		inputpanel.setBounds (10, 440, 370, 240);
		mainframe.add (inputpanel);

		//create the queues panel and add it to the frame
		queuespanel = createQueuesPanel ();
		queuespanel.setBounds (390, 10, 500, 500);
		mainframe.add (queuespanel);

		//create the events panel and add it to the frame
		eventspanel = createEventsPanel ();
		eventspanel.setBounds (10, 10, 370, 425);
		mainframe.add (eventspanel);

		//create the buttons panel and add it to the frame
		buttonspanel = createButtonsPanel ();
		buttonspanel.setBounds (390, 520, 500, 160);
		mainframe.add (buttonspanel);

		//make all visible... oooh yeah..
		mainframe.setVisible (true);
	}
	
	/** Sets this Window visible or invisible
	*
	* @param on specify if the window should be visible or not
	*/
	public void setVisible (boolean on)
	{
		this.mainframe.setVisible (on);
	}
	
	/** Sets the location of the Window on the screen relative to
	* the top-left corner.
	*
	* @param x the number of pixels to move to the right
	*
	* @param y the number of pixels to move to the bottom
	*/
	public void setLocation (int x, int y)
	{
		this.mainframe.setLocation (x, y);
	}
	
	//creates the menubar for the window, then returns it
	private JMenuBar createMenuBar ()
	{
		//the menu-bar
		JMenuBar mybar = new JMenuBar ();

		//create the menus and add mnemonics to them
		JMenu filemenu = new JMenu ("File");
		filemenu.setMnemonic (KeyEvent.VK_F);
		JMenu commandmenu = new JMenu ("Commands");
		commandmenu.setMnemonic (KeyEvent.VK_C);
		JMenu helpmenu = new JMenu ("Help");
		helpmenu.setMnemonic (KeyEvent.VK_H);

		//create the menu-items
		JMenuItem exitaction = new JMenuItem ("Exit");
		JMenuItem emptyaction = new JMenuItem ("Empty Events Area");
		JMenuItem aboutaction = new JMenuItem ("About");

		//the Exit menu-item causes the main window to be disposed (and therefore exit the application)
		exitaction.addActionListener (new ActionListener ()
		{
			@Override public void actionPerformed (ActionEvent a)
			{
				mainframe.dispose ();
			}
		});

		//the Empty menu-item cleares the results area.
		emptyaction.addActionListener (new ActionListener ()
		{
			@Override public void actionPerformed (ActionEvent a)
			{
				txta.setText ("");
			}
		});
		
		//the About menu-item creates the about box (the panel contained in a dialog)
		//and displays it
		aboutaction.addActionListener (new ActionListener ()
		{
			@Override public void actionPerformed (ActionEvent a)
			{
				new AboutDialog (mainframe).display ();
			}
		});

		//add menu items to the menu
		filemenu.add (exitaction);
		commandmenu.add (emptyaction);
		helpmenu.add (aboutaction);

		//add the menus to the menu-bar
		mybar.add (filemenu);
		mybar.add (commandmenu);
		mybar.add (helpmenu);
		
		//return the menu-bar
		return mybar;
	}

	//creates the panel with the user input fields
	private JPanel createInputPanel ()
	{
		//create the panel
		JPanel panel = new JPanel ();

		//let's set the layout of the elements to gridlayout. won't look perfect, but at
		//least we get rid of absolute positioning
		panel.setLayout (null);

		//create a border (with title) for the panel
		panel.setBorder (BorderFactory.createTitledBorder (BorderFactory.createEtchedBorder (),
															"Parameters (Optional)",
															TitledBorder.CENTER,
															TitledBorder.TOP));

		//create the labels
		JLabel labnrqueues = new JLabel ("Number of Queues: ");
		JLabel labnrcust = new JLabel ("Number of Customers: ");
		JLabel labminarr = new JLabel ("Minimum Arrival Interval: ");
		JLabel labmaxarr = new JLabel ("Maximum Arrival Interval: ");
		JLabel labminser = new JLabel ("Minimum Service Time: ");
		JLabel labmaxser = new JLabel ("Maximum Service Time: ");
		JLabel labreorg = new JLabel ("Reorganization Period: ");
		
		JLabel defaultnrqueues = new JLabel ("(default: 6)");
		JLabel defaultnrcust = new JLabel ("(default: 40)");
		JLabel defaultminarr = new JLabel ("(default: 4)");
		JLabel defaultmaxarr = new JLabel ("(default: 8)");
		JLabel defaultminser = new JLabel ("(default: 12)");
		JLabel defaultmaxser = new JLabel ("(default: 20)");
		JLabel defaultreorg = new JLabel ("(default: 4)");

		//create the input fields
		txtnrqueues = new JTextField (2);
		txtnrcust = new JTextField (4);
		txtminarr = new JTextField (3);
		txtmaxarr = new JTextField (3);
		txtminser = new JTextField (3);
		txtmaxser = new JTextField (3);
		txtreorg = new JTextField (3);

		//add everything to the panel
		//------------------------------------
		labnrqueues.setBounds (10, 25, 120, 20);
		panel.add (labnrqueues);
		
		txtnrqueues.setBounds (170, 25, 60, 25);
		panel.add (txtnrqueues);
		
		defaultnrqueues.setBounds (240, 25, 80, 20);
		panel.add (defaultnrqueues);
		//------------------------------------
		labnrcust.setBounds (10, 55, 145, 20);
		panel.add (labnrcust);
		
		txtnrcust.setBounds (170, 55, 60, 25);
		panel.add (txtnrcust);
		
		defaultnrcust.setBounds (240, 55, 80, 20);
		panel.add (defaultnrcust);
		//------------------------------------
		labminarr.setBounds (10, 85, 160, 20);
		panel.add (labminarr);
		txtminarr.setBounds (170, 85, 60, 25);
		panel.add (txtminarr);
		
		defaultminarr.setBounds (240, 85, 80, 20);
		panel.add (defaultminarr);
		//------------------------------------
		labmaxarr.setBounds (10, 115, 160, 20);
		panel.add (labmaxarr);
		
		txtmaxarr.setBounds (170, 115, 60, 25);
		panel.add (txtmaxarr);
	
		defaultmaxarr.setBounds (240, 115, 80, 20);
		panel.add (defaultmaxarr);
		//------------------------------------
		labminser.setBounds (10, 145, 160, 20);
		panel.add (labminser);
		txtminser.setBounds (170, 145, 60, 25);
		panel.add (txtminser);

		defaultminser.setBounds (240, 145, 80, 20);
		panel.add (defaultminser);
		//------------------------------------
		labmaxser.setBounds (10, 175, 160, 20);
		panel.add (labmaxser);
		
		txtmaxser.setBounds (170, 175, 60, 25);
		panel.add (txtmaxser);
		
		defaultmaxser.setBounds (240, 175, 80, 20);
		panel.add (defaultmaxser);
		//------------------------------------
		labreorg.setBounds (10, 205, 160, 20);
		panel.add (labreorg);
		
		txtreorg.setBounds (170, 205, 60, 25);
		panel.add (txtreorg);
		
		defaultreorg.setBounds (240, 205, 80, 20);
		panel.add (defaultreorg);
		//------------------------------------
		GUIUtilities.applyFont (panel);
		
		return panel;
	}

	//creates the panel where the queues will be displayed
	private JPanel createQueuesPanel ()
	{
		//create the panel
		JPanel panel = new JPanel ();

		//gridlayout for displaying the queue "elements". they will practically be JLabels with icons
		panel.setLayout (new GridLayout (MAX_QUEUES, MAX_QUEUE_SIZE + 1));

		//create a border (with title) for the panel
		panel.setBorder (BorderFactory.createTitledBorder (BorderFactory.createEtchedBorder (),
															"Queues",
															TitledBorder.CENTER,
															TitledBorder.TOP));

		queuelabels = new JLabel[MAX_QUEUES][MAX_QUEUE_SIZE + 1];

		//the queue head is an image, everything else is blank (a.k.a. invisible)
		for (int i = 0; i < MAX_QUEUES; i++)
		{
			for (int j = 0; j < MAX_QUEUE_SIZE + 1; j++)
			{
				queuelabels[i][j] = (j == MAX_QUEUE_SIZE)
									?
									new JLabel (new ImageIcon (getClass ().getResource ("images/disabled.png")))
									:
									new JLabel (" ");

				panel.add (queuelabels[i][j]);
			}
		}
		
		GUIUtilities.applyFont (panel);
		
		return panel;
	}

	//create the panel where the "events" happen
	private JPanel createEventsPanel ()
	{
		JPanel panel = new JPanel ();
		panel.setLayout (null);
		panel.setBorder (BorderFactory.createTitledBorder (BorderFactory.createEtchedBorder (),
															"Simulator Events",
															TitledBorder.CENTER,
															TitledBorder.TOP));

		//create the text area
		txta = new JTextArea ();
		txta.setEditable (false);
		txta.setLineWrap (true);
		txta.setWrapStyleWord (true);

		//wrap it around a scroll pane.
		JScrollPane jsp = new JScrollPane (txta);

		jsp.setBounds (10, 20, 350, 395);
		panel.add (jsp);
		
		GUIUtilities.applyFont (panel);
		
		return panel;
	}

	//creates the panel with the buttons
	private JPanel createButtonsPanel ()
	{
		JPanel panel = new JPanel ();
		panel.setLayout(null);
		panel.setBorder (BorderFactory.createTitledBorder (BorderFactory.createEtchedBorder (),	//type of border
															"Controls",							//title of the border
															TitledBorder.CENTER,				//position of the title
															TitledBorder.TOP));					//position of the title

		//create the buttons
		startsim = new JButton ("Start Simulation");
		stopsim = new JButton ("Stop Simulation");
		showgraphbutton = new JButton ("Show Graph");

		//add the action listeners for the buttons
		startsim.addActionListener (new StartListener ());
		stopsim.addActionListener (new StopListener ());
		showgraphbutton.addActionListener (new ShowGraphListener ());

		//next, add the buttons to the panel

		startsim.setBounds (20, 20, 140, 40);
		panel.add (startsim);

		stopsim.setBounds (340, 20, 140, 40);
		panel.add (stopsim);

		//an array of all integers up to MAX_QUEUE_SIZE - 1 is needed for the drop box
		Integer[] arr = new Integer[MAX_QUEUE_SIZE];
		for (int i = 0; i < MAX_QUEUE_SIZE; i++)
		{
			arr[i] = new Integer (i);
		}
		
		//create the drop box
		cbox = new JComboBox (arr);
		
		cbox.setBounds (10, 100, 55, 35);
		panel.add (cbox);
		
		openqueue = new JButton ("Open Queue");
		closequeue = new JButton ("Close Queue");
		
		openqueue.addActionListener (new OpenListener ());
		closequeue.addActionListener (new CloseListener ());
		
		openqueue.setBounds (80, 100, 120, 40);
		panel.add (openqueue);
		
		closequeue.setBounds (220, 100, 120, 40);
		panel.add (closequeue);
		
		showgraphbutton.setBounds (360, 100, 120, 40);
		showgraphbutton.setEnabled (false);
		panel.add (showgraphbutton);

		//at first, some buttons are disabled
		startsim.setEnabled (true);
		stopsim.setEnabled (false);
		openqueue.setEnabled (false);
		closequeue.setEnabled (false);
		
		GUIUtilities.applyFont (panel);
		
		return panel;
	}
	
	/** Receives an update message from the Simulator and refreshes the queues panel.
	*
	* @param obs the Simulator.
	* 
	* @param x the message received.
	*/
	public void update (Observable obs, Object x)
	{
		//downcast to String
		String msg = (String) x;
		
		//parse the received message
		String parsedmsg = MessageParser.parse (msg);
		
		//log the event to the events panel
		logSimulation (parsedmsg);

		//if the simulator stopped (for any reason), enable/disable the relevant buttons
		if (msg.equals ("S|F") || msg.equals ("S|E") || msg.equals ("S|X"))
		{
			isRunning = false;
			
			startsim.setEnabled (true);
			stopsim.setEnabled (false);
			
			closequeue.setEnabled (false);
			openqueue.setEnabled (false);
			
			if (msg.equals ("S|F"))
			{
				showgraphbutton.setEnabled (true);
			}
		}
		else if (msg.startsWith ("C"))
		{
			String[] msgelements = msg.split ("\\|");
			
			drawGraph (Integer.parseInt (msgelements[3]));
			redrawQueues ();
		}
		else if (msg.equals ("Q|R"))
		{
			drawReorganization ();
			redrawQueues ();
		}
	}
	
	//adds information to the XY series, called when customers reorganize
	private void drawReorganization ()
	{
		for (int i = 0; i < simulator.getNrOfQueues (); i++)
		{
			drawGraph (i);
		}
	}
	
	//adds information to the XY series for the specified queue
	private void drawGraph (int queue)
	{
		series[queue].add (simulator.getElapsedTime (), simulator.getQueueSize (queue));
	}
	
	//appends event to the events area
	private void logSimulation (String logmessage)
	{
		String currenttime = new SimpleDateFormat ("[K:mm:ss]").format (Calendar.getInstance ().getTime ());	
		txta.append (currenttime + " " + logmessage + "\n");
	}

	//redraws the queus (in the queues panel) after a customer arrived or left
	private void redrawQueues ()
	{
		int i, j;

		//go through every queue
		for (i = 0; i < simulator.getNrOfQueues (); i++)
		{
			if (simulator.isOpenQueue (i))
			{
				//if the queue is open, put the "open" icon
				queuelabels[i][MAX_QUEUE_SIZE].setIcon (new ImageIcon (getClass ().getResource ("images/enabled.png")));

				//next, display all customers... or empty spots if there are no customers in that particular area
				int qsize = simulator.getQueueSize (i);

				for (j = 0; j < MAX_QUEUE_SIZE; j++)
				{
					if (MAX_QUEUE_SIZE - qsize > j)
					{
						queuelabels[i][j].setIcon (null);
						queuelabels[i][j].setText (" ");
					}
					else
					{
						queuelabels[i][j].setIcon (new ImageIcon (getClass ().getResource ("images/filled-circle.gif")));
					}
					
					queuelabels[i][j].revalidate ();
				}
			}
			else
			{
				//for closed queues, put the "closed" icon
				queuelabels[i][MAX_QUEUE_SIZE].setIcon (new ImageIcon (getClass ().getResource ("images/disabled.png")));
				queuelabels[i][MAX_QUEUE_SIZE].revalidate ();
			}
		}
		
		//all the other queues are obviously closed
		for (i = simulator.getNrOfQueues (); i < MAX_QUEUES; i++)
		{
			queuelabels[i][MAX_QUEUE_SIZE].setIcon (new ImageIcon (getClass ().getResource ("images/disabled.png")));
			queuelabels[i][MAX_QUEUE_SIZE].revalidate ();
		}
	}

	//action listener for the start simulation button
	private class StartListener implements ActionListener
	{
		public void actionPerformed (ActionEvent e)
		{
			String s1 = txtnrqueues.getText (),
					s2 = txtnrcust.getText (),
					s3 = txtminarr.getText (),
					s4 = txtmaxarr.getText (),
					s5 = txtminser.getText (),
					s6 = txtmaxser.getText (),
					s7 = txtreorg.getText ();

			//create the simulator
			Simulator.SimulatorBuilder builder = Simulator.createSimulatorBuilder ();

			try
			{
				int tmp_nr_queues = s1.isEmpty () ? Simulator.DEFAULT_NR_QUEUES : Integer.parseInt (s1);
				int tmp_nr_customers = s2.isEmpty () ? Simulator.DEFAULT_NR_CUSTOMERS : Integer.parseInt (s2);
				int tmp_min_arrival = s3.isEmpty () ? Simulator.DEFAULT_MIN_ARRIVAL : Integer.parseInt (s3);
				int tmp_max_arrival = s4.isEmpty () ? Simulator.DEFAULT_MAX_ARRIVAL : Integer.parseInt (s4);
				int tmp_min_service = s5.isEmpty () ? Simulator.DEFAULT_MIN_SERVICE : Integer.parseInt (s5);
				int tmp_max_service = s6.isEmpty () ? Simulator.DEFAULT_MAX_SERVICE : Integer.parseInt (s6);
				int tmp_reorganization = s7.isEmpty () ? Simulator.DEFAULT_REORGANIZATION : Integer.parseInt (s7);

				builder.setNrQueues (tmp_nr_queues);
				builder.setNrCustomers (tmp_nr_customers);
				builder.setArrivalInterval (tmp_min_arrival, tmp_max_arrival);
				builder.setServiceAmount (tmp_min_service, tmp_max_service);
				builder.setReorganization (tmp_reorganization);
			}
			catch (NumberFormatException excep)
			{
				GUIUtilities.showErrorDialog (mainframe, "Parameter(s) format error !", "Input Error");

				return;
			}
			catch (IllegalArgumentException excep2)
			{
				GUIUtilities.showErrorDialog (mainframe, "Invalid Parameter(s): " + excep2.getMessage (), "Input Error");

				return;
			}
			
			simulator = builder.build ();
			simulator.addObserver (_instance);
			
			series = new XYSeries[simulator.getNrOfQueues ()];
			for (int i = 0; i < simulator.getNrOfQueues (); i++)
			{
				series[i] = new XYSeries ("Customers");
				series[i].add (0, 0);
			}
			
			//start the simulation
			simulator.simulate ();
			
			isRunning = true;

			//enable/disable the buttons
			startsim.setEnabled (false);
			stopsim.setEnabled (true);
			closequeue.setEnabled (true);
			openqueue.setEnabled (true);
			showgraphbutton.setEnabled (false);
			
			redrawQueues ();
		}
	}

	//action listener for the stop simulation button
	private class StopListener implements ActionListener
	{
		public void actionPerformed (ActionEvent e)
		{
			//stop the simulation
			simulator.stopSimulation ();
			
			isRunning = false;

			//enable/disable buttons
			startsim.setEnabled (true);
			stopsim.setEnabled (false);
		}
	}
	
	private class OpenListener implements ActionListener
	{
		public void actionPerformed (ActionEvent e)
		{
			if (cbox.getSelectedIndex () < simulator.getNrOfQueues ())
			{
				simulator.openQueue (cbox.getSelectedIndex ());
				redrawQueues ();
			}
		}
	}
	
	private class CloseListener implements ActionListener
	{
		public void actionPerformed (ActionEvent e)
		{
			if (cbox.getSelectedIndex () >= simulator.getNrOfQueues ())
			{
				return;
			}
			
			logSimulation ("Queue " + cbox.getSelectedIndex () + " was scheduled for closing.");
			
			simulator.scheduleClosing (cbox.getSelectedIndex ());
			
			if (simulator.getQueueSize (cbox.getSelectedIndex ()) == 0)
			{
				redrawQueues ();
			}
		}
	}
	
	private class ShowGraphListener implements ActionListener
	{
		public void actionPerformed (ActionEvent e)
		{
			int selectedq = cbox.getSelectedIndex ();
			String charttitle = "Graph for Queue " + selectedq;
			
			//create the chart
			JFreeChart chart = ChartFactory.createXYLineChart (
												charttitle,								//chart title
												"Time",										//label for X axis
												"Customer Count",							//label for Y axis
												new XYSeriesCollection (series[selectedq]),	//dataset (the actual graph)
												PlotOrientation.VERTICAL,					//orientation
												true,										//legend
												true,										//tooltips
												true);										//URLs
			
			chart.setAntiAlias (true);
			
			//enclose the chart inside a panel
			//since ChartPanel is a subclass of JPanel, it can be easily added on Swing elements...
			final ChartPanel panel = new ChartPanel (chart);
			panel.setDomainZoomable (true);
			panel.setRangeZoomable (false);
			
			//the size of the dialog
			final int DWIDTH = 850;
			final int DHEIGHT = 620;
			
			//create the dialog
			//make it non-modal, without any layout manager and not resizable
			final JDialog graphdiag = new JDialog (mainframe, "Polynomial Graph", false);
			graphdiag.setLayout(null);
			graphdiag.setSize (DWIDTH, DHEIGHT);
			graphdiag.setResizable (false);
			graphdiag.setDefaultCloseOperation (JDialog.DISPOSE_ON_CLOSE);
			
			//add the chart panel to the dialog
			panel.setBounds (0, 0, DWIDTH, DHEIGHT - 110);
			graphdiag.add (panel);
			
			//close button. create, position and add.
			JButton diagclose = new JButton ("Close");
			diagclose.setBounds ((DWIDTH - 70) / 2, DHEIGHT - 90, 70, 40);
			graphdiag.add (diagclose);

			//when the close button is pressed, the dialog is disposed
			diagclose.addActionListener (new ActionListener ()
			{
				@Override public void actionPerformed (ActionEvent a)
				{
					graphdiag.dispose ();
				}
			});

			//everything in this panel must be the same font as everything else
			GUIUtilities.applyFont (panel);
			GUIUtilities.applyComponentFont (diagclose);
			
			//resolution of the screen needed
			int[] rez = GUIUtilities.getResolution ();
	
			//calculate coordinates
			int xlocation = (rez[0] - DWIDTH) / 2;
			int ylocation = (rez[1] - DHEIGHT) / 2;

			//position the dialog on the middle of the screen and display it
			graphdiag.setLocation (xlocation, ylocation);
			graphdiag.setVisible (true);
		}
	}
	
	//window listener for the jframe... will display a confirmation dialog
	//if the user closes the window during the simulation.
	private class MyCloseListener implements WindowListener
	{
		@Override public void windowActivated (WindowEvent w) {}
		@Override public void windowClosed (WindowEvent w) {}
		@Override public void windowDeactivated (WindowEvent w) {}
		@Override public void windowDeiconified (WindowEvent w) {}
		@Override public void windowIconified (WindowEvent w) {}
		@Override public void windowOpened (WindowEvent w) {}
		
		@Override public void windowClosing (WindowEvent w)
		{
			//if the simulation is not running, just dispose of the window
			if (! isRunning)
			{
				mainframe.dispose ();
				
				return;
			}
			
			//size of the dialog
			final int DWIDTH = 350;
			final int DHEIGHT = 100;
			
			//create dialog, make it modal and not resizable
			final JDialog diag = new JDialog (mainframe, "Close Program", true);
			diag.setPreferredSize (new Dimension (DWIDTH, DHEIGHT));
			diag.setResizable (false);
			diag.setDefaultCloseOperation (JDialog.DISPOSE_ON_CLOSE);

			//create the panel
			JPanel notifypanel = new JPanel ();
			notifypanel.add (new JLabel ("Are you sure you want to exit ? The simulation is still running."));
			
			//create the buttons
			JButton yesbutton = new JButton ("Yes");
			JButton nobutton = new JButton ("No");
			
			//if yes is pressed, the simulation is first stopped,
			//then the dialog and window are disposed
			yesbutton.addActionListener (new ActionListener ()
			{
				@Override public void actionPerformed (ActionEvent a)
				{
					simulator.stopSimulation ();
					diag.dispose ();
					mainframe.dispose ();
				}
			});
			
			//if no is selected, hide the dialog
			nobutton.addActionListener (new ActionListener ()
			{
				@Override public void actionPerformed (ActionEvent a)
				{
					diag.dispose ();
				}
			});
			
			notifypanel.add (yesbutton);
			notifypanel.add (nobutton);

			//set the panel as the content of the dialog
			diag.setContentPane (notifypanel);
			diag.pack ();
			
			//resolution of the screen needed
			int[] rez = GUIUtilities.getResolution ();
	
			//calculate coordinates
			int xlocation = (rez[0] - DWIDTH) / 2;
			int ylocation = (rez[1] - DHEIGHT) / 2;

			//position the dialog on the middle of the screen and display it
			diag.setLocation (xlocation, ylocation);
			diag.setVisible (true);
		}
	}
}
