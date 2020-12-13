import jade.core.AID;

public class PlayerStats implements Comparable<Object> {

    @Override
	public String toString() {
		return "PlayerStats [ wins=" + wins + ", loses=" + loses + ", points=" + points + ", agentName="
				+ agentName + "]";
	}



	AID aid;
    private int id;
	private int wins;
	private int loses;
	private double points;
	private String agentName;
	private int ties;
	
	public PlayerStats(AID a) {
		this.wins = 0;
		this.loses = 0;
		this.points = 0;
		this.ties = 0;
		this.agentName = a.getLocalName();
        aid = a;
    }
	
	public int getTies() {
		return ties;
	}

	public void setTies(int ties) {
		this.ties = ties;
	}
	public void addTies(int n) {
		this.ties+=n;
	}

	public void setPoints(double points) {
		this.points = points;
	}

	public AID getAid() {
		return aid;
	}

	public void setAid(AID aid) {
		this.aid = aid;
	}


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getWins() {
		return wins;
	}
	public void setWins(int wins) {
		this.wins = wins;
	}
	public void addWins(int wins) {
		this.wins = wins;
	}
	public int getLoses() {
		return loses;
	}
	public void setLoses(int loses) {
		this.loses = loses;
	}
	public void addLoses(int loses) {
		this.loses += loses;
	}
	
	public double getPoints() {
		return points;
	}
	public void setPoints(int points) {
		this.points = points;
	}
	public void addPoints(double points) {
		this.points += points;
	}
	public String getAgentName() {
		return agentName;
	}
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}
	
	public void reset() {
		this.loses=0;
		this.points=0;
		this.ties=0;
		this.wins=0;
	}

    public double compareTo(PlayerStats p) {
       double comparepoints=((PlayerStats)p).getPoints();
        /* For Ascending order*/
        return this.points-comparepoints;

        /* For Descending order do like this */
        //return compareage-this.studentage;
    }
	
    @Override
    public boolean equals(Object o) {
        return aid.equals(o);
    }

	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
}
