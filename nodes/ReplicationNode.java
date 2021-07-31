package it.unipr.sowide.actodes.replication.nodes;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import it.unipr.sowide.actodes.actor.Behavior;
import it.unipr.sowide.actodes.actor.CaseFactory;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.MessagePattern;
import it.unipr.sowide.actodes.filtering.constraint.IsInstance;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.ErrorHandler;
import it.unipr.sowide.actodes.replication.clients.Client.Action;
import it.unipr.sowide.actodes.replication.content.ReleaseNode;
import it.unipr.sowide.actodes.replication.content.ReplicaResponse;
import it.unipr.sowide.actodes.replication.content.ReplicationRequest;
import it.unipr.sowide.actodes.replication.content.UpdateNodes;
import it.unipr.sowide.actodes.replication.content.VoteRequest;

public abstract class ReplicationNode extends Behavior {

	private static final long serialVersionUID = 1L;
	private static final int MAX_REPLICATION_TIME = 1000;
  private static final MessagePattern REQUEST = MessagePattern.contentPattern(new IsInstance(ReplicationRequest.class));
  private static final MessagePattern RESPONSE = MessagePattern.contentPattern(new IsInstance(ReplicaResponse.class));
  private static final MessagePattern UPDATE = MessagePattern.contentPattern(new IsInstance(UpdateNodes.class));  
  private static final MessagePattern VOTE = MessagePattern.contentPattern(new IsInstance(VoteRequest.class));  
  private static final MessagePattern RELEASE = MessagePattern.contentPattern(new IsInstance(ReleaseNode.class));  
	
	protected int index;
	protected int nClients;
	protected Reference[] nodes;
	private Random random;
	protected boolean isWorking;
	
	public ReplicationNode(int index, int nClients) {
	  this.index = index;
	  this.nClients = nClients;
	  this.random = new Random();
	  this.isWorking = true;
	  
	  ErrorHandler handler = new ErrorHandler(this, index);
	  handler.start();
	}

	@Override
	public void cases(CaseFactory c) {
	  
	  //Gestione dell'arrivo delle richieste di replicazione da parte dei client
		MessageHandler h = handleRequest();
		
		MessageHandler r = handleResponse();
		
		//Gestione dell'aggiornamento della lista dei nodi di replicazione
		MessageHandler a = (m) -> {
		  UpdateNodes un = (UpdateNodes) m.getContent();
		  
		  nodes = un.getNodes();
		  
		  System.out.printf("Replication Node %d: nodes received%n", index);
		  return null;
		};
		
		MessageHandler f = handleVoteRequest();
		
		MessageHandler s = handleNodeRelease();

		c.define(REQUEST, h);
		c.define(RESPONSE, r);
		c.define(UPDATE, a);
		c.define(VOTE, f);
		c.define(RELEASE, s);
	}
	
  protected ReplicaResponse doOperation(ReplicationRequest request) {
    String response = null;
    try
    {
      Thread.sleep(random.nextInt(MAX_REPLICATION_TIME));
      
      if (request.getAction().equals(Action.WRITE)) {
        writeOperation(request.getSender(), request. getReplica());
      } else {
        response = readOperation();
      }
      
      return new ReplicaResponse(index, request, response);
    }
    catch (InterruptedException | IOException e)
    {
      e.printStackTrace();
    }
    return null;
  }
  
  private String readOperation() throws IOException {
    File f = new File("partitions/node_" + index + ".txt");
    
    if (f.exists()) {
      Path fileName = Path.of("partitions/node_" + index + ".txt");
      
      return Files.readString(fileName);
    } else {
      return "";
    }
  }
  
  private void writeOperation(int client, int value) throws IOException {
    Path fileName = Path.of("partitions/node_" + index + ".txt");
    
    Files.write(fileName, ("Client " + client + ": " + value + System.lineSeparator()).getBytes(),
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }
  
  public boolean isWorking() {
    return isWorking;
  }
  
  public void setIsWorking(boolean isWorking) {
    this.isWorking = isWorking;
  }
	
	protected abstract MessageHandler handleNodeRelease();

  protected abstract MessageHandler handleVoteRequest();

  protected abstract MessageHandler handleResponse();

  protected abstract MessageHandler handleRequest();

  public abstract void handleRecovery();
}

