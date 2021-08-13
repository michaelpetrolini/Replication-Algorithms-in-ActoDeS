package it.unipr.sowide.actodes.replication.clients;

import java.util.Arrays;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.content.VoteResponse;
import it.unipr.sowide.actodes.replication.content.VoteRelease;
import it.unipr.sowide.actodes.replication.content.NodeRequest;
import it.unipr.sowide.actodes.replication.content.Reset;
import it.unipr.sowide.actodes.replication.content.VoteRequest;

/**
* The QuorumClient class implements the behavior of a client in a quorum based replication algorithm.
**/
public class QuorumClient extends Client {

  private static final long serialVersionUID = 1L;
  private static final int VOTE_TIMEOUT = 5000;
  private static final float WRITE_PROBABILITY = 0.2f;

  private Vote[] votes;
  private boolean completed;
  private int totalVotes;

  public QuorumClient(int index, Reference[] nodes, Reference manager)
  {
    super(index, nodes, manager);
  }

  /**
  * Sends a vote request to all replication nodes and wait for their response.
  **/
  @Override
  protected void sendRequest()
  {
    doingThings();
    
    votes = new Vote[nodes.length];
    Arrays.fill(votes, Vote.NOT_ARRIVED);
    
    action = (random.nextFloat() > WRITE_PROBABILITY)? Action.READ: Action.WRITE;
    completed = false;
    totalVotes = 0;
    
    MessageHandler voteHandler = handleVote();
    
    System.out.printf("Client %d: starting to send vote requests for %s to all replication nodes%n", index, action.getAction());
    
    for (int i = 0; i < nodes.length; i++) {
      if (votes[i].equals(Vote.NOT_ARRIVED)) {
        future(nodes[i], new VoteRequest(index), VOTE_TIMEOUT, voteHandler);
      }
    }
  }

  /**
  * Counts the number of votes arrived and, if they are enough, send a replication request for reading or writing.
  * 
  * @return a MessageHandler to handle the votes arriving from the replication nodes.
  **/
  private MessageHandler handleVote()
  {
    return (a) -> {
      totalVotes++;
      
      if (a.getContent() instanceof VoteResponse) {                
        if (completed) {
          send(a.getSender(), new VoteRelease(index));
        } 
        else {
          VoteResponse response = (VoteResponse) a.getContent();
          votes[response.getVoter()] = response.getVote();
        }
      }
      
      long nAvailable = Arrays.asList(votes).stream().filter(vote -> vote.equals(Vote.AVAILABLE)).count();
      
      if (!completed && ((action.equals(Action.READ) && nAvailable == 2) ||
          (action.equals(Action.WRITE) && nAvailable == nodes.length - 1))) {
        
        completed = true;

        System.out.printf("Client %d: has enough votes for the %s request%n", index, action.getAction());

        int replica = random.nextInt();
        NodeRequest request = new NodeRequest(replica, index, action);
                  
        for (int i = 0; i < nodes.length; i++) {
          if (votes[i].equals(Vote.AVAILABLE)) {
            future(nodes[i], request, REQUEST_TIMEOUT, handleResponse());
          }
        }
      }
      
      if (totalVotes == nodes.length && !completed)
      {
        System.out.printf("Client %d: doesn't have enough votes for the %s request (%d/%d)%n", index, action.getAction(),
            nAvailable, (action.equals(Action.WRITE))?nodes.length - 1: 2);
        releaseNodes();
        
        send(manager, new Reset(false));
      }
      
      return null;
    };
  }
  
  /**
  * Used to manage the different vote outcomes.
  **/
  public enum Vote {
    AVAILABLE,
    OCCUPIED,
    NOT_ARRIVED
  }

  /**{@inheritDoc}**/
  @Override
  protected long getnNodes()
  {
    return Arrays.asList(votes).stream().filter(vote -> vote.equals(Vote.AVAILABLE)).count();
  }

}
