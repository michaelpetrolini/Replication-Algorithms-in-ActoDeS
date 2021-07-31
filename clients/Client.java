package it.unipr.sowide.actodes.replication.clients;

import java.util.Random;

import it.unipr.sowide.actodes.actor.Behavior;
import it.unipr.sowide.actodes.actor.CaseFactory;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.MessagePattern;
import it.unipr.sowide.actodes.filtering.constraint.IsInstance;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.content.ReplicaResponse;

public abstract class Client extends Behavior {
	
	private static final long serialVersionUID = 1L;
	private static final double P_REQUEST = 0.05;
	private static final int SLEEP = 100;
	private static final MessagePattern RESPONSE = MessagePattern.contentPattern(new IsInstance(ReplicaResponse.class));
	
	protected Reference[] nodes;
	protected Random random;
	protected int index;
	protected int received;
  protected Action action;

	public Client(int index, Reference[] nodes) {
	  this.index = index;
		this.nodes = nodes;
		this.received = 0;
		this.random = new Random();
	}

	@Override
	public void cases(CaseFactory c) {
	  
	  //Gestione dell'invio della richiesta di replicazione
		MessageHandler a = sendRequest();
		
		//Gestione dell'arrivo delle risposte dei nodi di replicazione
		MessageHandler b = receiveResponse();
		
		c.define(START, a);
		c.define(RESPONSE, b);
	}
	
  protected void doingThings()
  {
    while (random.nextFloat() > P_REQUEST) {
      try {
        Thread.sleep(SLEEP);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  protected abstract MessageHandler receiveResponse();

  protected abstract MessageHandler sendRequest();
  
  public enum Action {
    READ("lettura"),
    WRITE("scrittura");
    
    private final String action;

    Action(String action)
    {
      this.action = action;
    }

    public String getAction()
    {
      return action;
    }
  }

}

