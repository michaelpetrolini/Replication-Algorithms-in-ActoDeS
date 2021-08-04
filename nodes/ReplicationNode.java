package it.unipr.sowide.actodes.replication.nodes;


import java.io.IOException;
import java.util.Random;

import it.unipr.sowide.actodes.actor.Behavior;
import it.unipr.sowide.actodes.actor.CaseFactory;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.MessagePattern;
import it.unipr.sowide.actodes.filtering.constraint.IsInstance;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.clients.Client.Action;
import it.unipr.sowide.actodes.replication.content.VoteRelease;
import it.unipr.sowide.actodes.replication.content.NodeResponse;
import it.unipr.sowide.actodes.replication.content.NodeRequest;
import it.unipr.sowide.actodes.replication.content.UpdateNodes;
import it.unipr.sowide.actodes.replication.content.VoteRequest;
import it.unipr.sowide.actodes.replication.handler.OperationHandler;

public abstract class ReplicationNode extends Behavior {

	private static final long serialVersionUID = 1L;
  private static final int MAX_REPLICATION_TIME = 200;
  protected static final int HEARTBEAT_TIMEOUT = 2000;
  protected static final int REPLICATION_TIMEOUT = 2000;
  private static final float ERROR_PROBABILITY = 0.05f;
  private static final float RECOVERY_PROBABILITY = 0.2f;
  
  private static final MessagePattern REQUEST = MessagePattern.contentPattern(new IsInstance(NodeRequest.class));
  private static final MessagePattern UPDATE = MessagePattern.contentPattern(new IsInstance(UpdateNodes.class));  
  private static final MessagePattern VOTE = MessagePattern.contentPattern(new IsInstance(VoteRequest.class));  
  private static final MessagePattern RELEASE = MessagePattern.contentPattern(new IsInstance(VoteRelease.class));  
	
	protected int index;
	protected Reference[] nodes;
	private Random random;
	protected boolean isWorking;
	
	public ReplicationNode(int index) {
	  this.index = index;
	  this.random = new Random();
	  this.isWorking = true;
	}

	@Override
	public void cases(CaseFactory c) {
	  
    //Gestione dell'arrivo delle richieste di replicazione da parte dei client
		c.define(REQUEST, handleRequest());

		//Gestione dell'aggiornamento della lista dei nodi di replicazione
		c.define(UPDATE, handleNodesUpdate());
		
		//Gestione della richiesta di voto per l'algoritmo a Quorum
		c.define(VOTE, handleVoteRequest());
		
		//Gestione del rilascio del nodo dal voto per l'algoritmo a Quorum
		c.define(RELEASE, handleNodeRelease());
	}

  private MessageHandler handleNodesUpdate()
  {
    return (m) -> {
		  UpdateNodes un = (UpdateNodes) m.getContent();
		  
		  nodes = un.getNodes();
		  
		  System.out.printf("Replication Node %d: nodes received%n", index);
		        
		  return null;
		};
  }
	
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
  
  protected boolean isWorking() {
    if (isWorking) {
      if (random.nextFloat() <= ERROR_PROBABILITY) {
        isWorking = false;
        System.out.printf("Replication Node %d: STOPPED WORKING%n", index);
      }
    } else {
      if (random.nextFloat() <= RECOVERY_PROBABILITY) {
        isWorking = true;
        handleRecovery();
        System.out.printf("Replication Node %d: RECOVERED%n", index);
      }
    }
    
    return isWorking;
  }
	
	protected MessageHandler handleNodeRelease()
  {
    return (m) -> {
      return null;
    };
  }

  protected MessageHandler handleVoteRequest()
  {
    return (m) -> {
      return null;
    };
  }

  public void handleRecovery() {
    
  }
  
  protected abstract MessageHandler handleRequest();

}

