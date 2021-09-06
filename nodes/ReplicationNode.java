package it.unipr.sowide.actodes.replication.nodes;


import java.io.IOException;
import java.util.Random;

import it.unipr.sowide.actodes.actor.Behavior;
import it.unipr.sowide.actodes.actor.CaseFactory;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.MessagePattern;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.filtering.constraint.IsInstance;
import it.unipr.sowide.actodes.replication.clients.Client.Action;
import it.unipr.sowide.actodes.replication.content.Terminate;
import it.unipr.sowide.actodes.replication.handler.OperationHandler;
import it.unipr.sowide.actodes.replication.request.NodeRequest;
import it.unipr.sowide.actodes.replication.request.NodeResponse;
import it.unipr.sowide.actodes.replication.votes.VoteRelease;
import it.unipr.sowide.actodes.replication.votes.VoteRequest;

/**
 * The ReplicationNode abstract class provides a partial implementation of a replication node.  
**/
public abstract class ReplicationNode extends Behavior {

	private static final long serialVersionUID = 1L;
  private static final int MAX_REPLICATION_TIME = 200;
  private static final float ERROR_PROBABILITY = 0.05f;
  private static final float RECOVERY_PROBABILITY = 0.2f;
  
  private static final MessagePattern REQUEST = MessagePattern.contentPattern(new IsInstance(NodeRequest.class));
  private static final MessagePattern VOTE = MessagePattern.contentPattern(new IsInstance(VoteRequest.class));  
  private static final MessagePattern RELEASE = MessagePattern.contentPattern(new IsInstance(VoteRelease.class));  
  private static final MessagePattern TERMINATE = MessagePattern.contentPattern(new IsInstance(Terminate.class));  
	
	protected int index;
	private Random random;
	private boolean isWorking;
	
	public ReplicationNode(int index) {
	  this.index = index;
	  this.random = new Random();
	  this.isWorking = true;
	}

	/** {@inheritDoc} **/
	@Override
	public void cases(CaseFactory c) {
	  
		c.define(REQUEST, handleRequest());
		
		c.define(VOTE, handleVoteRequest());
		
		c.define(RELEASE, handleNodeRelease());
		
		c.define(TERMINATE, terminate());
	}
	
  /**
   * Performs the action required by the client.
   * 
   * @return a NodeResponse containing the result of the operation required by the client.
  **/
  protected NodeResponse doOperation(NodeRequest request) {
    String response = null;
    try
    {
      Thread.sleep(random.nextInt(MAX_REPLICATION_TIME));
      
      if (request.getAction().equals(Action.WRITE))
      {
        System.out.printf("Replication Node %d: received write request for element %d from client %d%n", index, request.getReplica(), request.getSender());
        OperationHandler.writeOperation(index, request.getSender(), request. getReplica());
      } 
      else
      {
        System.out.printf("Replication Node %d: received read request from client %d%n", index, request.getSender());
        response = OperationHandler.readOperation(index, request.getSender());
      }
      return new NodeResponse(index, request, response);
    }
    catch (InterruptedException | IOException e)
    {
      e.printStackTrace();
    }
    return null;
  }
  
  /**
   * Checks if the replication node is currently working or if it is down.
   * 
   * @return true if the replication node is currently working, false otherwise.
  **/
  protected boolean isWorking() {
    if (isWorking) {
      if (random.nextFloat() <= ERROR_PROBABILITY) {
        isWorking = false;
        System.out.printf("Replication Node %d: STOPPED WORKING%n", index);
      }
    } else {
      if (random.nextFloat() <= RECOVERY_PROBABILITY) {
        isWorking = true;
        reset();
        System.out.printf("Replication Node %d: RECOVERED%n", index);
      }
    }
    
    return isWorking;
  }
	
  /**
   * Handles the release of the replication node from a vote given to a certain client, 
   * enabling the node to accept other clients' requests.
   * 
   * @return a MessageHandler to handle the release of the replication node.
  **/
	protected MessageHandler handleNodeRelease()
  {
    return (m) -> {
      return null;
    };
  }

  /**
   * Handles a client's vote request. If the node is not serving anyone it gives its vote
   * to the client, otherwise it refuses.
   * 
   * @return a MessageHandler to handle the vote request from a client.
  **/
  protected MessageHandler handleVoteRequest()
  {
    return (m) -> {
      return null;
    };
  }

  /**
   * Resets the node to its default values.
   **/
  public void reset() {
    
  }
  
  /**
   * Terminates the replication node.
   * 
   * @return a MessageHandler to terminate the replication node.
  **/
  private MessageHandler terminate() {
    return (m) -> {
        System.out.printf("Replication Node %d: terminated.%n", index);
        return Shutdown.SHUTDOWN;
    };
  }
  
  protected abstract MessageHandler handleRequest();

}

