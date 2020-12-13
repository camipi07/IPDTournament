
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import jade.core.AID;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public final class GUI extends JFrame implements ActionListener {
	JLabel leftPanelRoundsLabel;
	JLabel leftPanelExtraInformation;
	JLabel leftPanelGamesPlayed;
	JList<String> list;
	JTable table = new JTable();
	private ArrayList<String> currentPlayers = new ArrayList<String>();
	private MainAgent mainAgent;
	private JPanel rightPanel;
	private JTextArea rightPanelLoggingTextArea;
	private LoggingOutputStream loggingOutputStream;
	JSlider numberOfRoundsSlider;
	private int numberOfRounds;
	private int numberOfPlayers;
	private int numberOfGamesPlayed=0;
	private JTable parameterTable;
	
	public GUI() {
		initUI();
	}
	
	public void updateNumberOfGames(int n) {
		if(n==0) 
			this.numberOfGamesPlayed=0;
		else
			this.numberOfGamesPlayed+=n;
		String gamesPlayed = this.numberOfGamesPlayed+" / "+ nCr(numberOfPlayers,2);
		String data[][] = { {Integer.toString(numberOfPlayers),Integer.toString(numberOfRounds), gamesPlayed } };
		String header[] = { "N", "R","Games Played" };
		DefaultTableModel model = new DefaultTableModel(data, header);
		parameterTable.setModel(model);
	}
	static int nCr(int n, int r)   
	{   
	    return fact(n) / (fact(r) *   
	    fact(n - r));   
	}   
	static int fact(int n)   
	{   
	    int res = 1;   
	           for (int i = 2; i <= n; i++)   
	           res = res * i;   
	          return res;   
	} 
	public GUI(MainAgent agent) {
		mainAgent = agent;
		initUI();
		loggingOutputStream = new LoggingOutputStream(rightPanelLoggingTextArea);
	}

	public void log(String s) {
		Runnable appendLine = () -> {
			rightPanelLoggingTextArea
					.append('[' + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + s);
			rightPanelLoggingTextArea.setCaretPosition(rightPanelLoggingTextArea.getDocument().getLength());
		};
		SwingUtilities.invokeLater(appendLine);
	}

	public OutputStream getLoggingOutputStream() {
		return loggingOutputStream;
	}

	public void logLine(String s) {
		log(s + "\n");
	}

	public void setPlayersUI(String[] players) {
		DefaultListModel<String> listModel = new DefaultListModel<>();
		for (String s : players) {
			listModel.addElement(s);
		}
		list.setModel(listModel);
	}

	public void setPlayersStatisticsUI(HashMap<AID, PlayerStats> pS) {
		String[] header = { "Id","Name", "Wins", "Loses", "Ties","Points" };

		String[][] stats = new String[pS.size()][4];

		ArrayList<PlayerStats> playerStatsList = new ArrayList<PlayerStats>(pS.values());
		playerStatsList.sort( new Comparator<PlayerStats>() {
			public int compare(PlayerStats p1,PlayerStats p2) {
		        double p1points=((PlayerStats)p1).getPoints();
		        double p2points=p2.getPoints();
		        return (int)(p2points-p1points);

		    }});
		int i = 0;
		for (PlayerStats p : playerStatsList) {
			stats[i] = new String[] {Integer.toString(p.getId()),p.getAgentName(), Integer.toString(p.getWins()), Integer.toString(p.getLoses()),Integer.toString(p.getTies()),
					Double.toString(p.getPoints()) };
			i++;
		}

		DefaultTableModel tableModel = new DefaultTableModel(stats, header);

		System.out.println(stats);

		table.setModel(tableModel);
		numberOfPlayers = pS.size();
		return;

	}

	public void initUI() {
		setTitle("GUI");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(600, 400));
		setPreferredSize(new Dimension(1000, 600));
		setJMenuBar(createMainMenuBar());
		setContentPane(createMainContentPane());
		pack();
		setVisible(true);
		// create the status bar panel and shove it down the bottom of the frame
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.ipady = 0; // reset to default
		c.weighty = 1.0; // request any extra vertical space
		c.anchor = GridBagConstraints.PAGE_END; // bottom of space
		c.insets = new Insets(1, 0, 0, 0); // top padding
		c.gridx = 0; // aligned with button 2
		c.gridwidth = 1; // 2 columns wide
		c.gridy = 1; // third row
		JPanel statusPanel = new JPanel();
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		add(statusPanel, c);
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		JLabel statusLabel = new JLabel("Author: Camilo Piñón Blanco PSI Account #11");
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusPanel.add(statusLabel);
	}

	private Container createMainContentPane() {
		JPanel pane = new JPanel(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridy = 0;
		gc.weightx = 0.5;
		gc.weighty = 0.5;

		// LEFT PANEL
		gc.gridx = 0;
		gc.weightx = 1;
		pane.add(createLeftPanel(), gc);

		// CENTRAL PANEL
		gc.gridx = 1;
		gc.weightx = 8;
		pane.add(createCentralPanel(), gc);

		// RIGHT PANEL
		gc.gridx = 2;
		gc.weightx = 8;
		pane.add(createRightPanel(), gc);
		return pane;
	}

	private JPanel createLeftPanel() {

		JLabel leftPanelRunTitle = new JLabel("Running Options");
		JLabel leftPanelEditTitle = new JLabel("Edit Options");
		JLabel numberOfPlayersTitle = new JLabel("Choose the Number Of Rounds");
		leftPanelGamesPlayed = new JLabel("Games Played:  "+numberOfGamesPlayed);
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();

		leftPanelRoundsLabel = new JLabel("Round 0 / null");

		JButton leftPanelNewButton = new JButton("New");
		leftPanelNewButton.addActionListener(actionEvent -> mainAgent.newGame());



		
		JButton leftPanelStopButton = new JButton("Stop Game");
		leftPanelStopButton.addActionListener(actionEvent -> {
				mainAgent.getPaused();
			
		});
		
		JButton leftPanelContinueButton = new JButton("Continue Game");
		leftPanelContinueButton.addActionListener(actionEvent -> {
			
				mainAgent.doWake();
			 
		});
		
		
		JButton leftPanelResetAllPlayersButton = new JButton("Reset All Players");
		leftPanelResetAllPlayersButton.addActionListener(actionEvent -> mainAgent.resetPlayers());

		JTextField playerToRemoveInput = new JTextField("Inserte el jugador que desea eliminar");
		playerToRemoveInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String input = playerToRemoveInput.getText();
				playerToRemoveInput.setText(input);
			}
		});

		JButton deletePlayerButton = new JButton("Delete Player");
		deletePlayerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(mainAgent.removeAgent(playerToRemoveInput.getText())) {
					System.out.println("Eliminado el jugador "+playerToRemoveInput.getText());
				}else
					System.out.println("No existe tal jugador");
				
			}
		});

		// Table with game parameters data
		String data[][] = {
				{ Integer.toString(mainAgent.getParameters().N), Integer.toString(mainAgent.getParameters().R),"0"

				} };
		String header[] = { "N", "R","Games Played"};
		// parameterTable.setBounds(50,50,200,300);

		DefaultTableModel model = new DefaultTableModel(data, header);

		 parameterTable = new JTable(model);
		try {
			parameterTable.setAutoCreateRowSorter(true);
		} catch (Exception continuewithNoSort) {
		}

		JScrollPane tableScroll = new JScrollPane(parameterTable);

		Dimension tablePreferred = tableScroll.getPreferredSize();
		tableScroll.setPreferredSize(new Dimension(tablePreferred.width, tablePreferred.height / 8));
		// Slider to control the number of players of a game
		numberOfRoundsSlider = new JSlider(0, 100, 0);
		numberOfRoundsSlider.setPaintLabels(true);
		numberOfRoundsSlider.setMajorTickSpacing(25);
		numberOfRoundsSlider.setMinorTickSpacing(10);
		numberOfRoundsSlider.setPaintTicks(true);
		Hashtable position = new Hashtable();
		position.put(0, new JLabel("0"));
		position.put(25, new JLabel("25"));
		position.put(50, new JLabel("50"));
		position.put(75, new JLabel("75"));
		position.put(100, new JLabel("100"));

		numberOfRoundsSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				numberOfRounds = numberOfRoundsSlider.getValue();
				String gamesPlayed = numberOfGamesPlayed+" / "+ nCr(numberOfPlayers,2);
				String data[][] = { {Integer.toString(numberOfPlayers),Integer.toString(numberOfRounds), gamesPlayed } };
				String header[] = { "N", "R","Games Played" };
				DefaultTableModel model = new DefaultTableModel(data, header);
				parameterTable.setModel(model);
				mainAgent.setNumberOfRounds(numberOfRounds);
			}

		});

		JCheckBoxMenuItem toggleVerboseWindowMenu = new JCheckBoxMenuItem("Verbose", true);
		toggleVerboseWindowMenu
				.addActionListener(actionEvent -> rightPanel.setVisible(toggleVerboseWindowMenu.getState()));

		leftPanelExtraInformation = new JLabel("Parameters - NR");
		
		JButton leftPanelClearConsoleButton = new JButton("Clear console");
		leftPanelClearConsoleButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rightPanelLoggingTextArea.setText("");
			}

		});

		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;
		gc.weightx = 1;
		gc.weighty = 0.5;
		gc.gridy = 0;

		gc.gridy = gc.gridy + 1;
		leftPanel.add(numberOfPlayersTitle, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(numberOfRoundsSlider, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(leftPanelRunTitle, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(leftPanelNewButton, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(leftPanelStopButton, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(leftPanelContinueButton, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(leftPanelExtraInformation, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(tableScroll, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(leftPanelEditTitle, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(leftPanelResetAllPlayersButton, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(playerToRemoveInput, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(deletePlayerButton, gc);
		gc.gridy = gc.gridy + 1;
		leftPanel.add(leftPanelGamesPlayed, gc);

		gc.gridy = gc.gridy + 1;
		leftPanel.add(toggleVerboseWindowMenu, gc);
		gc.gridy = gc.gridy + 1;
		gc.weighty = 25;
		leftPanel.add(leftPanelClearConsoleButton, gc);
		

		return leftPanel;
	}

	private JPanel createCentralPanel() {
		JPanel centralPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 0.5;

		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;

		gc.gridy = 0;
		gc.weighty = 1;
		centralPanel.add(createCentralTopSubpanel(), gc);
		gc.gridy = 1;
		gc.weighty = 4;
		centralPanel.add(createCentralBottomSubpanel(), gc);

		return centralPanel;
	}

	private JPanel createCentralTopSubpanel() {
		JPanel centralTopSubpanel = new JPanel(new GridBagLayout());

		DefaultListModel<String> listModel = new DefaultListModel<>();
		listModel.addElement("Empty");
		list = new JList<>(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.setVisibleRowCount(5);
		JScrollPane listScrollPane = new JScrollPane(list);

		JLabel info1 = new JLabel("Selected player info");


		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 0.5;
		gc.weighty = 0.5;
		gc.anchor = GridBagConstraints.CENTER;

		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridheight = 666;
		gc.fill = GridBagConstraints.BOTH;
		centralTopSubpanel.add(listScrollPane, gc);
		gc.gridx = 1;
		gc.gridheight = 1;
		gc.fill = GridBagConstraints.NONE;
		centralTopSubpanel.add(info1, gc);
		gc.gridy = 1;
		return centralTopSubpanel;
	}

	private JPanel createCentralBottomSubpanel() {
		JPanel centralBottomSubpanel = new JPanel(new GridBagLayout());

	
		
		Object[] nullPointerWorkAround = { "P1/P2", "C", "D" };

		Object[][] data = { { "P1/P2", "C", "D" }, { "C", "3,3", "0,5" }, { "D", "5,0", "1,1" } };

		JLabel payoffLabel = new JLabel("Payoff matrix");
		JTable payoffTable = new JTable(data, nullPointerWorkAround);
		payoffTable.setTableHeader(null);
		payoffTable.setEnabled(false);

		JScrollPane player1ScrollPane = new JScrollPane(payoffTable);

		String[] header = { "Name", "Wins", "Loses","Ties", "Points" };

		DefaultTableModel tableModel = new DefaultTableModel(header, 0);
		
		table.setModel(tableModel);
		JScrollPane tableScroll = new JScrollPane(table);

		Dimension tablePreferred = tableScroll.getPreferredSize();
		tableScroll.setPreferredSize(new Dimension(tablePreferred.width, tablePreferred.height));

		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 0.5;
		gc.weighty = 0.5;
		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;

		gc.gridx = 0;
		gc.gridy = 0;
		centralBottomSubpanel.add(payoffLabel, gc);
		gc.gridy = 1;
		gc.gridx = 0;
		centralBottomSubpanel.add(player1ScrollPane, gc);
		gc.gridy = 2;
		gc.gridx = 0;
		gc.weighty = 30;
		centralBottomSubpanel.add(tableScroll, gc);

		return centralBottomSubpanel;
	}

	private JPanel createRightPanel() {
		rightPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weighty = 1d;
		c.weightx = 1d;

		rightPanelLoggingTextArea = new JTextArea("");
		rightPanelLoggingTextArea.setEditable(false);
		JScrollPane jScrollPane = new JScrollPane(rightPanelLoggingTextArea);
		rightPanel.add(jScrollPane, c);
		return rightPanel;
	}

	private JMenuBar createMainMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		JMenuItem exitFileMenu = new JMenuItem("Exit");
		exitFileMenu.setToolTipText("Exit application");
		exitFileMenu.addActionListener(this);

		JMenuItem newGameFileMenu = new JMenuItem("New Game");
		newGameFileMenu.setToolTipText("Start a new game");
		newGameFileMenu.addActionListener(this);

		menuFile.add(newGameFileMenu);
		menuFile.add(exitFileMenu);
		menuBar.add(menuFile);

		JMenu menuEdit = new JMenu("Edit");
		JMenuItem resetPlayerEditMenu = new JMenuItem("Reset Players");
		resetPlayerEditMenu.setToolTipText("Reset all player");
		resetPlayerEditMenu.setActionCommand("reset_players");
		resetPlayerEditMenu.addActionListener(this);

		JMenuItem parametersEditMenu = new JMenuItem("Parameters");
		parametersEditMenu.setToolTipText("Modify the parameters of the game");
		parametersEditMenu.addActionListener(actionEvent -> logLine("Parameters: "
				+ JOptionPane.showInputDialog(new Frame("Configure parameters"), "Enter parameters N,S,R,I,P")));

		menuEdit.add(resetPlayerEditMenu);
		menuEdit.add(parametersEditMenu);
		menuBar.add(menuEdit);

		JMenu menuRun = new JMenu("Run");

		JMenuItem newRunMenu = new JMenuItem("New");
		newRunMenu.setToolTipText("Starts a new series of games");
		newRunMenu.addActionListener(this);

		JMenuItem stopRunMenu = new JMenuItem("Stop");
		stopRunMenu.setToolTipText("Stops the execution of the current round");
		stopRunMenu.addActionListener(this);

		JMenuItem continueRunMenu = new JMenuItem("Continue");
		continueRunMenu.setToolTipText("Resume the execution");
		continueRunMenu.addActionListener(this);

		JMenuItem roundNumberRunMenu = new JMenuItem("Number Of rounds");
		roundNumberRunMenu.setToolTipText("Change the number of rounds");
		roundNumberRunMenu.addActionListener(actionEvent -> logLine(
				JOptionPane.showInputDialog(new Frame("Configure rounds"), "How many rounds?") + " rounds"));

		menuRun.add(newRunMenu);
		menuRun.add(stopRunMenu);
		menuRun.add(continueRunMenu);
		menuRun.add(roundNumberRunMenu);
		menuBar.add(menuRun);

		JMenu menuWindow = new JMenu("Window");

		JCheckBoxMenuItem toggleVerboseWindowMenu = new JCheckBoxMenuItem("Verbose", true);
		toggleVerboseWindowMenu
				.addActionListener(actionEvent -> rightPanel.setVisible(toggleVerboseWindowMenu.getState()));

		menuWindow.add(toggleVerboseWindowMenu);
		menuBar.add(menuWindow);
		
		JMenu menuHelp = new JMenu("Help");
		JMenuItem aboutHelpMenu = new JMenuItem("About");
		aboutHelpMenu.setToolTipText("Information about the developer");
		aboutHelpMenu.addActionListener(actionEvent -> 
				JOptionPane.showMessageDialog(new Frame("About the developer"), "Dev: Camilo Piñón Blanco"));
		menuHelp.add(aboutHelpMenu);
		menuBar.add(menuHelp);
		
		
		return menuBar;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton button = (JButton) e.getSource();
			logLine("Button " + button.getText());
		} else if (e.getSource() instanceof JMenuItem) {
			JMenuItem menuItem = (JMenuItem) e.getSource();
			logLine("Menu " + menuItem.getText());
		}
	}

	public class LoggingOutputStream extends OutputStream {
		private JTextArea textArea;

		public LoggingOutputStream(JTextArea jTextArea) {
			textArea = jTextArea;
		}

		@Override
		public void write(int i) throws IOException {
			textArea.append(String.valueOf((char) i));
			textArea.setCaretPosition(textArea.getDocument().getLength());
		}
	}
}