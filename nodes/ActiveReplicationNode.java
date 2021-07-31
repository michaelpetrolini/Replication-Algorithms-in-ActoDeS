package it.unipr.sowide.actodes.replication.nodes;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.replication.content.ReplicaResponse;
import it.unipr.sowide.actodes.replication.content.ReplicationRequest;

public class ActiveReplicationNode extends ReplicationNode {
  
  private static final long serialVersionUID = 1L;

  public ActiveReplicationNode(int index, int nClients)
  {
    super(index, nClients);
  }

  @Override
  protected MessageHandler handleRequest()
  {
    return (m) -> {
      if (isWorking) {
        ReplicationRequest request = (ReplicationRequest) m.getContent();
        
        System.out.printf("Replication Node %d: received request to replicate element %d from client %d%n", index, request.getReplica(), request.getSender());

        ReplicaResponse response = doOperation(request);
        send(m.getSender(), response);
        
        nClients--;
        if (nClients == 0) {
          System.out.printf("Replication Node %d: finished to serve all clients%n", index);
          return Shutdown.SHUTDOWN;
        }
      }
      
      return null;
    };
  }

  @Override
  protected MessageHandler handleResponse()
  {
    return (m) -> {
      return null;
    };
  }

  @Override
  protected MessageHandler handleVoteRequest()
  {
    return (m) -> {
      return null;
    };
  }

  @Override
  protected MessageHandler handleNodeRelease()
  {
    return (m) -> {
      return null;
    };
  }

  @Override
  public void handleRecovery()
  {
    
  }

}
