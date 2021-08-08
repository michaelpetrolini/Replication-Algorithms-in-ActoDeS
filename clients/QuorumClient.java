package it.unipr.sowide.actodes.replication.clients;

import static java.util.stream.Collectors.toList;

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
  private static final int VOTE_TIMEOUT = 15000;
  private static final float WRITE_PROBABILITY = 0.2f;

  private Vote[] votes;
  private boolean completed;

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
    
    MessageHandler voteHandler = handleVote();
    
    System.out.printf("Client %d: inizio invio richieste di voto di %s a tutti i client%n", index, action.getAction());
    
    for (int i = 0; i < nodes.length; i++) {
      if (votes[i].equals(Vote.NOT_ARRIVED)) {
        future(nodes[i], new VoteRequest(index), VOTE_TIMEOUT, voteHandler);
      }
    }
  }

  /**
  * Counts the number of votes arrived and, if they are enough, send a replication request for reading or writing.
  **/
  private MessageHandler handleVote()
  {
    return (a) -> {
      total++;
      
      int nAvailable = Arrays.asList(votes).stream().filter(vote -> vote.equals(Vote.AVAILABLE)).collect(toList()).size();

      if (a.getContent() instanceof VoteResponse) {
        VoteResponse response = (VoteResponse) a.getContent();
        
        votes[response.getVoter()] = response.getVote();
                
        if (completed) {
          send(a.getSender(), new VoteRelease(index));
        }
      }
      
      if (!completed) {
        if ((action.equals(Action.READ) && nAvailable == 2) ||
            (action.equals(Action.WRITE) && nAvailable == nodes.length - 1)) {
          
          completed = true;

          System.out.printf("Client %d: arrivati abbastanza voti per la richiesta di %s%n", index, action.getAction());

          int replica = random.nextInt();
          NodeRequest request = new NodeRequest(replica, index, action);
                    
          for (int i = 0; i < nodes.length; i++) {
            if (votes[i].equals(Vote.AVAILABLE)) {
              future(nodes[i], request, REQUEST_TIMEOUT, handleResponse());
            }
          }
        }
      } 
      
      if (total == nodes.length && !completed)
      {
        System.out.printf("Client %d: non sono arrivati abbastanza voti per la richiesta di %s (%d/%d)%n", index, action.getAction(),
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
  protected int getnNodes()
  {
    return Arrays.asList(votes).stream().filter(vote -> vote.equals(Vote.AVAILABLE)).collect(toList()).size();
  }

}
