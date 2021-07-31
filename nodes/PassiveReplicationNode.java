package it.unipr.sowide.actodes.replication.nodes;

import java.util.Arrays;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.replication.content.ReplicaResponse;
import it.unipr.sowide.actodes.replication.content.ReplicationRequest;

public class PassiveReplicationNode extends ReplicationNode {
  
  private static final long serialVersionUID = 1L;
  
  private Boolean[][] completed;

  public PassiveReplicationNode(int index, int nClients)
  {
    super(index, nClients);
    
    if (index == 0) {
      completed = new Boolean[nClients][];
    }
  }

  @Override
  protected MessageHandler handleRequest()
  {
    return (m) -> {
      if (isWorking) {
        ReplicationRequest request = (ReplicationRequest) m.getContent();

        if (index == 0) {
          System.out.printf("Primary Node %d: received request to replicate element %d from client %d%n", index, request.getReplica(), request.getSender());
          Boolean[] c = new Boolean[nodes.length - 1];
          Arrays.fill(c, false);
          completed[request.getSender()] = c;
          
          for (int i = 1; i < nodes.length; i++) {
            send(nodes[i], request);
          }
        } else {        
          System.out.printf("Secondary Node %d: received request to replicate element %d from client %d%n", index, request.getReplica(), request.getSender());

          ReplicaResponse response = doOperation(request);
          send(m.getSender(), response);
          
          nClients--;
          if (nClients == 0) {
            System.out.printf("Secondary Node %d: finished to serve all clients%n", index);
            return Shutdown.SHUTDOWN;
          }
        }
      }
      
      return null;
    };
  }

  @Override
  protected MessageHandler handleResponse()
  {
    return (m) -> {
      if (isWorking) {
        if (index == 0) {
          ReplicaResponse response = (ReplicaResponse) m.getContent();
          System.out.printf("Primary Node %d: received response from node %d for request sent by client %d%n",
              index, response.getNodeIndex(), response.getRequest().getSender());
          
          completed[response.getRequest().getSender()][response.getNodeIndex() - 1] = true;
          
          if (!Arrays.asList(completed[response.getRequest().getSender()]).contains(false)) {
            System.out.printf("Primary Node %d: received all responses for request sent by client %d%n",
                index, response.getRequest().getSender());
            
            send(m.getSender(), new ReplicaResponse(index, response.getRequest(), null));
            
            nClients--;
            if (nClients == 0) {
              System.out.printf("Primary Node %d: finished to serve all clients%n", index);
              return Shutdown.SHUTDOWN;
            }
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
