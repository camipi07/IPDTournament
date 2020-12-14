package playerAgents;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;


public class Spiteful_CC extends Agent {

    private State state;
    private AID mainAgent;
    private int myId, opponentId;
    private int N,R;
    private ACLMessage msg;
	private boolean mode_defeat = false;
	private int current_round=0;
    
    protected void setup() {
        state = State.s0NoConfig;

        //Register in the yellow pages as a player
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
        System.out.println("SpitefulCCAgent " + getAID().getName() + " is ready.");

    }

    protected void takeDown() {
        System.out.println("RandomPlayer " + getAID().getName() + " terminating.");
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
                System.out.println(getAID().getName() + " received " + msg.getContent() + " from " + msg.getSender().getName()); //DELETEME
                //-------- Agent logic
                switch (state) {
                    case s0NoConfig:
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> go to state 1
                        //Else ERROR
                        if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
                            boolean parametersUpdated = false;
                            try {
                                parametersUpdated = validateSetupMessage(msg);
                            } catch (NumberFormatException e) {
                                System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                            }
                            if (parametersUpdated) state = State.s1AwaitingGame;

                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                    case s1AwaitingGame:
                        //If INFORM NEWGAME#_,_ PROCESS NEWGAME --> go to state 2
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> stay at s1
                        //Else ERROR
                    	
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            if (msg.getContent().startsWith("Id#")) { //Game settings updated
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
                                	mode_defeat = false;
                                	current_round=0;
                                	state = State.s2Round;}
                            }
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                    case s2Round:
                        //If REQUEST POSITION --> INFORM POSITION --> go to state 3
                        //If INFORM ENDGAME go to state 1
                        //Else error
                        if (msg.getPerformative() == ACLMessage.REQUEST) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(mainAgent);
                            
                            //Agent logic
                            if(current_round<2) {  //cooperate the first two rounds
    							msg.setContent("Action#C");
    						}else { //standard Spiteful behavior 
    							// Cooperate till opponent defeats
    							if (mode_defeat == false)
    								msg.setContent("Action#C");
    							else
    								msg.setContent("Action#D");
    						}
                            current_round++;
                            
                            System.out.println(getAID().getName() + " sent " + msg.getContent());
                            send(msg);
                            state = State.s3AwaitingResult;
                        } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("GameOver#")) {
                            state = State.s1AwaitingGame;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message:" + msg.getContent());
                        }
                        break;
                    case s3AwaitingResult:
                        //If INFORM RESULTS --> go to state 2
                        //Else error
                    	if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {

                    		//if opponent defeated enter mode defeat till the end
                    		if (checkOpponentDefeat(msg) && (mode_defeat==false) ) {
									mode_defeat = true; 
							}
                    		
                            state = State.s2Round;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                }
            }
        }
    
    /**
     * Checks if opponent defeated in the last round
     *
     * @param msg ACLMessage to process
     * @return true on if the opponent defeated, false in other case
     */
    private boolean checkOpponentDefeat(ACLMessage msg) {
		String msgContent = msg.getContent();
		System.out.println("Checking message: "+msgContent);
		if (msgContent.contains("D")) { //implies that the opponent defeated, because i didn't
			return true;
		}else {
			return false;
		}
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
            if (contentSplit.length != 3) return false;
            if (!contentSplit[0].equals("Id")) return false;
            tMyId = Integer.parseInt(contentSplit[1]);

            String[] parametersSplit = contentSplit[2].split(",");
            if (parametersSplit.length != 2) return false;
            tN = Integer.parseInt(parametersSplit[0]);
            tR = Integer.parseInt(parametersSplit[1]);
        

            //At this point everything should be fine, updating class variables
            mainAgent = msg.getSender();
            N = tN;
            R = tR;
            myId = tMyId;
            return true;
        }

        /**
         * Processes the contents of the New Game message
         * @param msgContent Content of the message
         * @return true if the message is valid
         */
        public boolean validateNewGame(String msgContent) {
            int msgId0, msgId1;
            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 3) return false;
            if (!contentSplit[0].equals("NewGame")) return false;
          
          
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