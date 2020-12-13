import jade.core.Agent;

  @SuppressWarnings("serial")
public class FirstAgent extends Agent 
  { 
      protected void setup() 
      { 
          System.out.println("Hello World. ");
          System.out.println("My name is "+ getLocalName()); 
      }
  }
