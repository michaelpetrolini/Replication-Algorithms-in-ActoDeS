package it.unipr.sowide.actodes.replication.clients;

import java.util.Random;

import it.unipr.sowide.actodes.actor.Behavior;
import it.unipr.sowide.actodes.actor.CaseFactory;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.MessagePattern;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.filtering.constraint.IsInstance;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.content.Reset;
import it.unipr.sowide.actodes.replication.content.Terminate;
import it.unipr.sowide.actodes.replication.request.FEResponse;
import it.unipr.sowide.actodes.replication.request.NodeRequest;

/**
* The Client abstract class provides an implementation of a replication client.
**/
public class Client extends Behavior {
	
	private static final long serialVersionUID = 1L;
  private static final float WRITE_PROBABILITY = 0.2f;
	private static final float P_REQUEST = 0.05f;
	private static final int SLEEP = 1000;
  private static final long REQUEST_TIMEOUT = 10000;
  
  private static final MessagePattern RESET = MessagePattern.contentPattern(new IsInstance(Reset.class));
	
	private Reference fe;
	private Random random;
	private int nOperations;
	private int current;
	private int index;
	private Action action;
  

	public Client(int index, Reference fe, int nOperations)
  {
	  this.index = index;
	  this.fe = fe;
	  this.nOperations = nOperations;
	  this.current = 0;
    this.random = new Random();
  }

  /** {@inheritDoc} **/
	@Override
	public void cases(CaseFactory c) {
	  
		MessageHandler a = handleRequest();
		
		c.define(START, a);
		
		c.define(RESET, a);
		
	}
	
  /**
   * Manages the sending of a request to the Front End
   * 
   * @return a MessageHandler to handle the request
  **/
  private MessageHandler handleRequest() {
    return (m) -> {
      current++;
      
      if (current <= nOperations) {
        
        doingThings();
        
        this.action = (random.nextFloat() > WRITE_PROBABILITY)? Action.READ: Action.WRITE;
        
        int replica = random.nextInt();
        
        if (action.equals(Action.WRITE)) {
          System.out.printf("Client %d: starting %s request for element %d%n", index, action.getAction(), replica);
        } else {
          System.out.printf("Client %d: starting %s request%n", index, action.getAction());
        }
        
        MessageHandler handler = handleResponse();
        
        future(fe, new NodeRequest(replica, index, action), REQUEST_TIMEOUT, handler);
        
        return null;
      } else {
        System.out.printf("Client %d: terminating%n", index);
        send(fe, new Terminate());
        return Shutdown.SHUTDOWN;
      }
    };
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
   * Handles the reception of the Front End's response.
   * 
   * @return a MessageHandler to handle the replication nodes' replies.
  **/
  protected MessageHandler handleResponse() {
    return (m) -> {
      FEResponse response;
      if (m.getContent() instanceof FEResponse && (response = (FEResponse) m.getContent()).isSuccess()) {
        
        if(action.equals(Action.READ)) 
        {
          System.out.printf("Client %d: received a value (%d/%d)%n", index, response.getCompleted(), response.getTotal());                 
        } 
        else
        {
          System.out.printf("Client %d: replication completed (%d/%d)%n", index, response.getCompleted(), response.getTotal());
        } 
      } else {
        System.out.printf("Client %d: replication failed%n", index);
      }
      
      send(fe, new Reset(true));
      return null;
    };
  }

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
  * Used to manage the different vote outcomes.
  **/
  public enum Vote {
    AVAILABLE,
    OCCUPIED,
    NOT_ARRIVED
  }
}

