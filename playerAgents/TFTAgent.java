package playerAgents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class TFTAgent extends Agent {

	private State state;
	private AID mainAgent;
	private int myId, opponentId;
	private ACLMessage msg;
	private boolean isFirstRound = true;
	private String opponentLastAction = "";

	protected void setup() {
		state = State.s0NoConfig;

		// Register in the yellow pages as a player
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		sd.setName("Game");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new Play());
		System.out.println("TFTAgent " + getAID().getName() + " is ready.");

	}


	private enum State {
		s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
	}

	private class Play extends CyclicBehaviour {
		@Override
		public void action() {
			System.out.println(getAID().getName() + ":" + state.name());
			msg = blockingReceive();
			if (msg != null) {
				System.out.println(
						getAID().getName() + " received " + msg.getContent() + " from " + msg.getSender().getName()); // DELETEME
				// -------- Agent logic
				switch (state) {
				case s0NoConfig:
					// If INFORM Id#_#_,_,_,_ PROCESS SETUP --> go to state 1
					// Else ERROR
					if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
						boolean parametersUpdated = false;
						try {
							parametersUpdated = validateSetupMessage(msg);
						} catch (NumberFormatException e) {
							System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
						}
						if (parametersUpdated)
							state = State.s1AwaitingGame;

					} else {
						System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
					}
					break;
				case s1AwaitingGame:
					// If INFORM NEWGAME#_,_ PROCESS NEWGAME --> go to state 2
					// If INFORM Id#_#_,_,_,_ PROCESS SETUP --> stay at s1
					// Else ERROR

					if (msg.getPerformative() == ACLMessage.INFORM) {
						if (msg.getContent().startsWith("Id#")) { // Game settings updated
							try {
								validateSetupMessage(msg);
							} catch (NumberFormatException e) {
								System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
							}
						} else if (msg.getContent().startsWith("NewGame#")) {
							boolean gameStarted = false;
							try {
								gameStarted = validateNewGame(msg.getContent());
							} catch (NumberFormatException e) {
								System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
							}
							if (gameStarted) {
								/**
								 * Resets the parameters of the algorithm for the next game
								 */
								isFirstRound = true;
								state = State.s2Round;
								
								}
						} else if (msg.getContent().startsWith("TakeDown")) {
							System.out.println(getAID().getName() + ":" + "Se acabo lo que sedaba");
							takeDown();
						}
					} else {
						System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
					}
					break;
				case s2Round:
					// If REQUEST POSITION --> INFORM POSITION --> go to state 3
					// If INFORM ENDGAME go to state 1
					// Else error
					if (msg.getPerformative() == ACLMessage.REQUEST) {
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.addReceiver(mainAgent);
						
						//TFT Action logic
						if (isFirstRound) {
							msg.setContent("Action#C"); //cooperates
							isFirstRound = false;
						} else {

							msg.setContent("Action#" + opponentLastAction); //mimmicks the opponent
						}

						System.out.println(getAID().getName() + " sent " + msg.getContent());
						send(msg);
						state = State.s3AwaitingResult;
					} else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("GameOver#")) {
						System.out.println("DEBUG GAME OVER");
						state = State.s1AwaitingGame;
					} else {
						System.out.println(
								getAID().getName() + ":" + state.name() + " - Unexpected message:" + msg.getContent());
					}
					break;
				case s3AwaitingResult:
					// If INFORM RESULTS --> go to state 2
					// Else error
					if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
						saveOpponentLastAction(msg);
						state = State.s2Round;
					} else {
						System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
					}
					break;
				}
			}
		}
		
		/**
		 * Stores the opponent last action
		 * 
		 * @param the message containing the actions for this round
		 * 
		 */

		private void saveOpponentLastAction(ACLMessage msg) {
			// format Results#4,7#D,C#5,0.
			String msgContent = msg.getContent();
			String[] contentSplit = msgContent.split("#");
			String[][] playerIdsAndMoves = { { contentSplit[1].split(",")[0], contentSplit[1].split(",")[1] },
					{ contentSplit[2].split(",")[0], contentSplit[2].split(",")[1] } };

			if (Integer.parseInt(playerIdsAndMoves[0][0]) == myId)
				opponentLastAction = playerIdsAndMoves[1][1];
			else
				opponentLastAction = playerIdsAndMoves[1][0];

			return;
		}

		/**
		 * Validates and extracts the parameters from the setup message
		 *
		 * @param msg ACLMessage to process
		 * @return true on success, false on failure
		 */
		private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
			int tN, tR, tMyId;
			String msgContent = msg.getContent();

			String[] contentSplit = msgContent.split("#");
			if (contentSplit.length != 3)
				return false;
			if (!contentSplit[0].equals("Id"))
				return false;
			tMyId = Integer.parseInt(contentSplit[1]);

			String[] parametersSplit = contentSplit[2].split(",");
			if (parametersSplit.length != 2)
				return false;
			tN = Integer.parseInt(parametersSplit[0]);
			tR = Integer.parseInt(parametersSplit[1]);

			mainAgent = msg.getSender();
			myId = tMyId;
			return true;
		}

		/**
		 * Processes the contents of the New Game message
		 * 
		 * @param msgContent Content of the message
		 * @return true if the message is valid
		 */
		public boolean validateNewGame(String msgContent) {
			int msgId0, msgId1;
			String[] contentSplit = msgContent.split("#");
			if (contentSplit.length != 3)
				return false;
			if (!contentSplit[0].equals("NewGame"))
				return false;
			msgId0 = Integer.parseInt(contentSplit[1]);
			msgId1 = Integer.parseInt(contentSplit[2]);

			if (myId == msgId0) {
				opponentId = msgId1;
				return true;
			} else if (myId == msgId1) {
				opponentId = msgId0;
				return true;
			}
			return false;
		}
	}
}