package it.unipr.sowide.actodes.replication.nodes;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.replication.clients.QuorumClient.Vote;
import it.unipr.sowide.actodes.replication.content.VoteResponse;
import it.unipr.sowide.actodes.replication.content.VoteRelease;
import it.unipr.sowide.actodes.replication.content.NodeResponse;
import it.unipr.sowide.actodes.replication.content.NodeRequest;
import it.unipr.sowide.actodes.replication.content.VoteRequest;

public class QuorumReplicationNode extends ReplicationNode {

  private static final long serialVersionUID = 1L;
  
  private int actuallyServing;
  private boolean available;

  public QuorumReplicationNode(int index, int nClients)
  {
    super(index);
    this.actuallyServing = -1;
    this.available = true;
  }

  @Override
  protected MessageHandler handleRequest()
  {
    return (m) -> {
      if (isWorking()) {
        NodeRequest request = (NodeRequest) m.getContent();
        
        if (actuallyServing == request.getSender()) {
    
          NodeResponse response = doOperation(request);
          send(m, response);
        }  
      }
      
      return null;
    };
  }

  @Override
  protected MessageHandler handleVoteRequest()
  {
    return (m) -> {
      if (isWorking()) {
        VoteRequest request = (VoteRequest) m.getContent();
        
        if (available) 
        {
          System.out.printf("Replication Node %d: ricevuta richiesta di voto dal client %d,"
              + " essendo libero dò il mio voto%n", index, request.getRequester());
          
          available = false;
          actuallyServing = request.getRequester();
          
          send(m, new VoteResponse(Vote.AVAILABLE, index));
        } 
        else
        {
          System.out.printf("Replication Node %d: ricevuta richiesta di voto dal client %d,"
              + " essendo occupato dal client %d non dò il mio voto%n", index, request.getRequester(), actuallyServing);
          
          send(m, new VoteResponse(Vote.OCCUPIED, index));
        }
      }
      
      return null;
    };
  }

  @Override
  protected MessageHandler handleNodeRelease()
  {
    return (m) -> {
      if (isWorking()) {
        VoteRelease release = (VoteRelease) m.getContent();
        
        if (actuallyServing == release.getId())
        { 
          System.out.printf("Replication Node %d: ricevuto permesso di rilascio dal client %d%n", index, release.getId());
          available = true;
          actuallyServing = -1;
        }
      }
      
      return null;
    };
  }

  @Override
  public void handleRecovery()
  {
    this.actuallyServing = -1;
    this.available = true;
  }

}
