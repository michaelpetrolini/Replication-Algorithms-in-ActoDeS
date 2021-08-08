package it.unipr.sowide.actodes.replication.nodes;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.replication.clients.QuorumClient.Vote;
import it.unipr.sowide.actodes.replication.content.VoteResponse;
import it.unipr.sowide.actodes.replication.content.VoteRelease;
import it.unipr.sowide.actodes.replication.content.NodeResponse;
import it.unipr.sowide.actodes.replication.content.NodeRequest;
import it.unipr.sowide.actodes.replication.content.VoteRequest;

/**
 * The QuorumReplicationNode class implements the behavior of a replication node in a quorum based replication algorithm.
 **/
public class QuorumReplicationNode extends ReplicationNode {

  private static final long serialVersionUID = 1L;
  
  private int currentlyServing;
  private boolean available;

  public QuorumReplicationNode(int index, int nClients)
  {
    super(index);
    reset();
  }

  /**
   * Handles a client's replication request by saving its value.
   **/
  @Override
  protected MessageHandler handleRequest()
  {
    return (m) -> {
      if (isWorking()) {
        NodeRequest request = (NodeRequest) m.getContent();
        
        if (currentlyServing == request.getSender()) {
    
          NodeResponse response = doOperation(request);
          send(m, response);
        }  
      }
      
      return null;
    };
  }

  /**
   * Handles a client's vote request. If the node is not serving anyone it gives its vote
   * to the client, otherwise it refuses.
   **/
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
          currentlyServing = request.getRequester();
          
          send(m, new VoteResponse(Vote.AVAILABLE, index));
        } 
        else
        {
          System.out.printf("Replication Node %d: ricevuta richiesta di voto dal client %d,"
              + " essendo occupato dal client %d non dò il mio voto%n", index, request.getRequester(), currentlyServing);
          
          send(m, new VoteResponse(Vote.OCCUPIED, index));
        }
      }
      
      return null;
    };
  }

  /**
   * Handles a client's release, enabling the node to accept other clients' requests.
   **/
  @Override
  protected MessageHandler handleNodeRelease()
  {
    return (m) -> {
      if (isWorking()) {
        VoteRelease release = (VoteRelease) m.getContent();
        
        if (currentlyServing == release.getId())
        { 
          System.out.printf("Replication Node %d: ricevuto permesso di rilascio dal client %d%n", index, release.getId());
          reset();
        }
      }
      
      return null;
    };
  }

  /**{@inheritDoc}**/
  @Override
  public void reset()
  {
    this.currentlyServing = -1;
    this.available = true;
  }

}
