package it.unipr.sowide.actodes.replication.clients;

import java.util.Random;

import it.unipr.sowide.actodes.actor.Behavior;
import it.unipr.sowide.actodes.actor.CaseFactory;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.content.NodeResponse;
import it.unipr.sowide.actodes.replication.content.VoteRelease;

public abstract class Client extends Behavior {
	
	private static final long serialVersionUID = 1L;
	private static final float P_REQUEST = 0.05f;
	private static final int SLEEP = 1000;
  protected static final long REQUEST_TIMEOUT = 60000;
	
	protected Reference[] nodes;
	protected Random random;
	protected int index;
	protected int received;
	protected int total;
  protected Action action;

	public Client(int index, Reference[] nodes) {
	  this.index = index;
		this.nodes = nodes;
		this.received = 0;
		this.total = 0;
		this.random = new Random();
		this.action = Action.WRITE;
	}

	@Override
	public void cases(CaseFactory c) {
	  
	  //Gestione dell'invio della richiesta di replicazione
		MessageHandler a = handleRequest();
		
		c.define(START, a);
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

  private MessageHandler handleRequest() {
    return (m) -> {
      sendRequest();
      
      return null;
    };
  }
  
  protected abstract void sendRequest();
  
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
  
  protected MessageHandler handleResponse() {
    return (m) -> {
      total++;
      if (m.getContent() instanceof NodeResponse) {
        NodeResponse response = (NodeResponse) m.getContent();
        
        if(action.equals(Action.READ)) 
        {
          System.out.printf("Client %d: ho letto il file del nodo %d: %s%n", index, response.getNodeIndex(), response.getResponse());
          
          releaseNodes();
          
          sendRequest();
        } 
        else
        {
          received++;
          
          send(m.getSender(), new VoteRelease(index));
        } 
      }
      

      if (total == getnNodes()) {
        System.out.printf("Client %d: replication completed (%d/%d).%n", index, received, getnNodes());      
        sendRequest();
      }
      
      return null;
    };
  }
  
  protected abstract int getnNodes();

  protected void releaseNodes()
  {
    VoteRelease release = new VoteRelease(index);
    
    for (int i = 0; i < nodes.length; i++) {
        send(nodes[i], release);
    }
  }

}

