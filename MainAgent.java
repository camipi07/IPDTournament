
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.leap.HashSet;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;

public class MainAgent extends Agent {

	private GUI gui;
	private AID[] playerAgents;
	private GameParametersStruct parameters = new GameParametersStruct();
	private HashMap<AID, PlayerStats> playerStats = new HashMap<AID, PlayerStats>();
	private HashSet removedPlayers = new HashSet();
	private MainAgent ma = this;
	private boolean paused=false;


	public void getPaused(){
		this.paused = true;
	}
	


	

	@Override
	protected void setup() {
		gui = new GUI(ma);
		System.setOut(new PrintStream(gui.getLoggingOutputStream()));
		updatePlayers();

		gui.logLine("Agent " + getAID().getName() + " is ready.");
		

	}

	public void resetPlayers() { // reset all player stats

		for (AID a : playerStats.keySet()) {
			playerStats.get(a).reset();
		}
		gui.setPlayersStatisticsUI(playerStats);

	}

	public int updatePlayers() {
		gui.logLine("Updating player list");
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);

			if (result.length > 0) {
				gui.logLine("Found " + (result.length - removedPlayers.size()) + " players");
			}
			playerAgents = new AID[result.length - removedPlayers.size()];
			for (int i = 0; i < result.length; ++i) { // aquí cambié ++i por i++
				playerAgents[i] = result[i].getName();
			}
		} catch (FIPAException fe) {
			gui.logLine(fe.getMessage());
		}

		String[] playerNames = new String[playerAgents.length];
		for (int i = 0; i < playerAgents.length; i++) {
			playerNames[i] = playerAgents[i].getName();
			playerStats.put(playerAgents[i], new PlayerStats(playerAgents[i]));
		}
		gui.setPlayersUI(playerNames);
		gui.setPlayersStatisticsUI(playerStats);
		gui.updateNumberOfGames(0);

		return 0;
	}

	public boolean removeAgent(String agentName) {
		// removes the agent from the stats map and the player array
		ArrayList<AID> playersList = new ArrayList<AID>(Arrays.asList(playerAgents));
		System.out.println("Lista de jugadores: " + playersList);
		AID aidPlayerToDelete = new AID();
		for (int i = 0; i < playerAgents.length; i++) {
			if (playerAgents[i].getLocalName().contentEquals(agentName)) {
				aidPlayerToDelete = playerAgents[i];
				playersList.remove(aidPlayerToDelete);
				System.out.println("Num players después de eliminar: " + playersList.size());
				playerAgents = new AID[playersList.size()];
				for (int j = 0; j < playersList.size(); j++) {
					playerAgents[j] = playersList.get(j);
				}
				playerStats.remove(aidPlayerToDelete);
				String[] playerNames = new String[playerAgents.length];
				for (int j = 0; j < playerAgents.length; j++) {
					playerNames[j] = playerAgents[j].getName();
				}
				gui.setPlayersUI(playerNames);
				gui.setPlayersStatisticsUI(playerStats);
				return true;

			}
		}
		return false;
	}

	public int newGame() {
		resetPlayers();
		addBehaviour(new GameManager());
		return 0;
	}

	public AID[] getPlayerAgents() {
		return playerAgents;
	}

	public void setPlayerAgents(AID[] playerAgents) {
		this.playerAgents = playerAgents;
	}

	public GameParametersStruct getParameters() {
		return parameters;
	}

	public void setParameters(GameParametersStruct parameters) {
		this.parameters = parameters;
	}

	public void setNumberOfRounds(int rounds) {
		this.parameters.R = rounds;
	}

	/**
	 * In this behavior this agent manages the course of a match during all the
	 * rounds.
	 */
	private class GameManager extends SimpleBehaviour {
		@Override
		public void action() {
			// Assign the IDs
			gui.updateNumberOfGames(0);
			ArrayList<PlayerInformation> players = new ArrayList<>();
			int lastId = 0;
			for (AID a : playerAgents) {
				playerStats.get(a).setId(lastId);
				players.add(new PlayerInformation(a, lastId++));
			}

			gui.setPlayersStatisticsUI(playerStats);

			for (PlayerInformation player : players) {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("Id#" + player.id + "#" + parameters.N + "," + parameters.R);
				msg.addReceiver(player.aid);
				send(msg);
			}

			// Organize the matches
			for (int i = 0; i < players.size(); i++) {
				for (int j = i + 1; j < players.size(); j++) {
					playGame(players.get(i), players.get(j));
				}
			}

		}

		private String generateResultsString(PlayerInformation player1, PlayerInformation player2, String actionPlayer1,
				String actionPlayer2, int[] playerPoints) {

			String results = "Results#";

			results += player1.id + "," + player2.id + "#" + actionPlayer1 + "," + actionPlayer2 + "#";

			if (actionPlayer1.equals("C") && actionPlayer2.equals("C")) { // both cooperate
				results += "3,3";
				playerPoints[0] += 3;
				playerPoints[1] += 3;

			} else if ((actionPlayer1.equals("C") && actionPlayer2.equals("D") && player1.id < player2.id)
					|| (actionPlayer1.equals("D") && actionPlayer2.equals("C") && player1.id > player2.id)) {
				results += "0,5";
				playerPoints[1] += 5;

			} else if ((actionPlayer1.equals("D") && actionPlayer2.equals("C") && player1.id < player2.id)
					|| (actionPlayer1.equals("C") && actionPlayer2.equals("D") && player1.id > player2.id)) {
				results += "5,0";
				playerPoints[0] += 5;

			} else { // both defeat
				results += "1,1";
				playerPoints[0] += 1;
				playerPoints[0] += 1;
			}
			return results;
		}

		private String generateGameOverString(PlayerInformation player1, PlayerInformation player2, int[] playerPoints,
				int rounds) {
			String gameOver = "GameOver#";

			gameOver += player1.id + "," + player2.id + "#";
			gameOver += (double) playerPoints[0] / rounds + "," + (double) playerPoints[1] / rounds;
			playerStats.get(player1.aid).addPoints((double) playerPoints[0] / rounds);
			playerStats.get(player2.aid).addPoints((double) playerPoints[1] / rounds);

			if (playerPoints[0] > playerPoints[1]) {
				playerStats.get(player1.aid).addWins(1);
				playerStats.get(player2.aid).addLoses(1);
			} else if (playerPoints[0] < playerPoints[1]) {
				playerStats.get(player1.aid).addLoses(1);
				playerStats.get(player2.aid).addWins(1);
			} else {
				playerStats.get(player1.aid).addTies(1);
				playerStats.get(player2.aid).addTies(1);
			}

			return gameOver;
		}

		private void playGame(PlayerInformation player1, PlayerInformation player2) {
			int[] playerPoints = { 0, 0 };
			double rounds = Math.random() * (parameters.R - parameters.R * 0.9 + 1) + parameters.R * 0.9;
			int r0 = (int) rounds;

			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(player1.aid);
			msg.addReceiver(player2.aid);
			msg.setContent("NewGame#" + player1.id + "#" + player2.id);
			send(msg);

			for (int i = 0; i < r0; i++) {

				String actionPlayer1, actionPlayer2;

				msg = new ACLMessage(ACLMessage.REQUEST);
				msg.setContent("Action");
				msg.addReceiver(player1.aid);
				send(msg);
				

				gui.logLine("Main Waiting for movement");
				ACLMessage move1 = blockingReceive();
				gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
				actionPlayer1 = move1.getContent().split("#")[1];

				msg = new ACLMessage(ACLMessage.REQUEST);
				msg.setContent("Action");
				msg.addReceiver(player2.aid);
				send(msg);

				gui.logLine("Main Waiting for movement2");
				ACLMessage move2 = blockingReceive();
				gui.logLine("Main Received " + move2.getContent() + " from " + move2.getSender().getName());
				actionPlayer2 = move2.getContent().split("#")[1];

				msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(player1.aid);
				msg.addReceiver(player2.aid);
				msg.setContent(generateResultsString(player1, player2, actionPlayer1, actionPlayer2, playerPoints));
				send(msg);
				if (paused){
					ma.doWait();
					paused = false;
				}
			}

			msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(player1.aid);
			msg.addReceiver(player2.aid);
			msg.setContent(generateGameOverString(player1, player2, playerPoints, r0));
			send(msg);
			
			gui.updateNumberOfGames(1);
			gui.setPlayersStatisticsUI(playerStats);
			return;

		}

		public GameManager() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean done() {
			return true;
		}

	}

	public class PlayerInformation {

		AID aid;
		int id;

		public PlayerInformation(AID a, int i) {

			aid = a;
			id = i;
		}

		public AID getAid() {
			return aid;
		}

		public void setAid(AID aid) {
			this.aid = aid;
		}

		@Override
		public boolean equals(Object o) {
			return aid.equals(o);
		}
	}

	public class GameParametersStruct {

		int N;
		int R;

		public GameParametersStruct() {
			N = 2;
			R = 50;
		}
	}

}