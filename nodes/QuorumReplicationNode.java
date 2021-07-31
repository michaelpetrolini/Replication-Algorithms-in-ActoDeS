package it.unipr.sowide.actodes.replication.nodes;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.replication.clients.QuorumClient.Vote;
import it.unipr.sowide.actodes.replication.content.QuorumResponse;
import it.unipr.sowide.actodes.replication.content.ReleaseNode;
import it.unipr.sowide.actodes.replication.content.ReplicaResponse;
import it.unipr.sowide.actodes.replication.content.ReplicationRequest;
import it.unipr.sowide.actodes.replication.content.VoteRequest;

public class QuorumReplicationNode extends ReplicationNode {

  private static final long serialVersionUID = 1L;
  
  private int actuallyServing;
  private boolean available;

  public QuorumReplicationNode(int index, int nClients)
  {
    super(index, nClients);
    this.actuallyServing = -1;
    this.available = true;
  }

  @Override
  protected MessageHandler handleResponse()
  {
    return (m) -> {
      return null;
    };
  }

  @Override
  protected MessageHandler handleRequest()
  {
    return (m) -> {
      if (isWorking) {
        ReplicationRequest request = (ReplicationRequest) m.getContent();
        
        if (actuallyServing == request.getSender()) {
          System.out.printf("Replication Node %d: received request to replicate element %d from client %d%n", index, request.getReplica(), request.getSender());
    
          ReplicaResponse response = doOperation(request);
          send(m.getSender(), response);
          
          nClients--;
          if (nClients == 0) {
            System.out.printf("Replication Node %d: finished to serve all clients%n", index);
            return Shutdown.SHUTDOWN;
          }
        }  
      }

      return null;
    };
  }

  @Override
  protected MessageHandler handleVoteRequest()
  {
    return (m) -> {
      if (isWorking) {
        VoteRequest request = (VoteRequest) m.getContent();
        
        if (available) {
          System.out.printf("Replication Node %d: ricevuta richiesta di voto dal client %d,"
              + " essendo libero dò il mio voto%n", index, request.getRequester());
          available = false;
          actuallyServing = request.getRequester();
          send(m, new QuorumResponse(Vote.AVAILABLE, index));
        } else {
          System.out.printf("Replication Node %d: ricevuta richiesta di voto dal client %d,"
              + " essendo occupato non dò il mio voto%n", index, request.getRequester());
          send(m, new QuorumResponse(Vote.OCCUPIED, index));
        }
      }
      
      return null;
    };
  }

  @Override
  protected MessageHandler handleNodeRelease()
  {
    return (m) -> {
      if (isWorking) {
        ReleaseNode release = (ReleaseNode) m.getContent();
        
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
