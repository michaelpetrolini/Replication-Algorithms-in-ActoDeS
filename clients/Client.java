package it.unipr.sowide.actodes.replication.clients;

import java.util.Random;

import it.unipr.sowide.actodes.actor.Behavior;
import it.unipr.sowide.actodes.actor.CaseFactory;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.MessagePattern;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.filtering.constraint.IsInstance;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.content.NodeResponse;
import it.unipr.sowide.actodes.replication.content.Reset;
import it.unipr.sowide.actodes.replication.content.VoteRelease;

/**
* The Client abstract class provides a partial implementation of a replication client.
**/
public abstract class Client extends Behavior {
	
	private static final long serialVersionUID = 1L;
	private static final float P_REQUEST = 0.05f;
	private static final int SLEEP = 1000;
  protected static final long REQUEST_TIMEOUT = 5000;
  private static final MessagePattern RESET = MessagePattern.contentPattern(new IsInstance(Reset.class));

	
	protected Reference[] nodes;
	protected Reference manager;
	protected Random random;
	protected int index;
	protected int received;
	protected int total;
  protected Action action;

	public Client(int index, Reference[] nodes, Reference manager) {
	  this.index = index;
		this.nodes = nodes;
		this.manager = manager;
		this.received = 0;
		this.total = 0;
		this.random = new Random();
		this.action = Action.WRITE;
	}

	/** {@inheritDoc} **/
	@Override
	public void cases(CaseFactory c) {
	  
		MessageHandler a = handleRequest();
		
		c.define(START, a);
		
		c.define(RESET, restart());
	}
	
	/**
	 * Simulation of client's execution for a random time before the replication request.  
	**/
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

  /**
   * Manages the sending of the requests to the replication nodes based on the type of replication algorithm
   * 
   * @return a MessageHandler to handle the request
  **/
  private MessageHandler handleRequest() {
    return (m) -> {
      sendRequest();
      
      return null;
    };
  }
  
  /**
   * Handles the restart of the client to send a new request  
   * 
   * @return a MessageHandler to handle the restart or termination of the client
  **/
  private MessageHandler restart() {
    return (m) -> {
      Reset r = (Reset) m.getContent();
      
      if (r.isRestart()) {
        System.out.printf("Client %d: restarting...%n", index);
        
        this.received = 0;
        this.total = 0;
        
        sendRequest();
      } else {
        System.out.printf("Client %d: terminated.%n", index);
        return Shutdown.SHUTDOWN;
      }
      
      return null;
    };
  }
  
  
  protected abstract void sendRequest();
  
  /**
   * Description of the type of replication request.  
  **/
  public enum Action {
    READ("reading"),
    WRITE("writing");
    
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
  
  /**
   * Handles the reception of a node response.
   * 
   * @return a MessageHandler to handle the replication nodes' replies.
  **/
  protected MessageHandler handleResponse() {
    return (m) -> {
      total++;
      if (m.getContent() instanceof NodeResponse) {
        NodeResponse response = (NodeResponse) m.getContent();
        
        if(action.equals(Action.READ)) 
        {
          System.out.printf("Client %d: received a value from the replication node %d: %s%n", index, response.getNodeIndex(), response.getResponse());
          
          releaseNodes();
          
          send(manager, new Reset(false));
        } 
        else
        {
          received++;
          send(m.getSender(), new VoteRelease(index));
        } 
      }
      

      if (total == getnNodes()) {
        System.out.printf("Client %d: replication completed (%d/%d).%n", index, received, getnNodes());      
        send(manager, new Reset(false));
      }
      
      return null;
    };
  }
  
  /**
  * Returns the number of node responses (or timeouts) to consider before terminating the request.
  * 
  * @return the number of responses to consider before terminating the request.
  **/
  protected abstract long getnNodes();

  /**
   * Handles the release of the nodes voting for the client.  
  **/
  protected void releaseNodes()
  {
    VoteRelease release = new VoteRelease(index);
    
    for (int i = 0; i < nodes.length; i++) {
        send(nodes[i], release);
    }
  }

}

