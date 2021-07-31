package it.unipr.sowide.actodes.replication.clients;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.content.QuorumResponse;
import it.unipr.sowide.actodes.replication.content.ReleaseNode;
import it.unipr.sowide.actodes.replication.content.ReplicaResponse;
import it.unipr.sowide.actodes.replication.content.ReplicationRequest;
import it.unipr.sowide.actodes.replication.content.VoteRequest;

public class QuorumClient extends Client {

  private static final long serialVersionUID = 1L;
  private static final int VOTE_TIMEOUT = 1000;
  private static final double WRITE_PROBABILITY = 0.2;

  private MessageHandler pAlive;
  private Vote[] votes;
  private boolean completed;

  public QuorumClient(int index, Reference[] nodes)
  {
    super(index, nodes);
    
    votes = new Vote[nodes.length];
    Arrays.fill(votes, Vote.NOT_ARRIVED);
    
    action = (random.nextFloat() > WRITE_PROBABILITY)? Action.READ: Action.WRITE;
    this.completed = false;
  }

  @Override
  protected MessageHandler receiveResponse()
  {
    return (m) -> {
      ReplicaResponse response = (ReplicaResponse) m.getContent();
      if(action.equals(Action.READ)) {
        System.out.printf("Client %d: ho letto il file del nodo %d: %s", index, response.getNodeIndex(), response.getResponse());
        
        return Shutdown.SHUTDOWN;
      } else {
        received++;
        if (received == nodes.length) {
          System.out.printf("Client %d: replication completed.%n", index);
          
          
          
          ReleaseNode release = new ReleaseNode(index);
          for (int i = 0; i < nodes.length; i++) {
            if (votes[i].equals(Vote.AVAILABLE)) {
              send(nodes[i], release);
            }
          }
        
          return Shutdown.SHUTDOWN;
        }
      }
      
      return null;
    };
  }

  @Override
  protected MessageHandler sendRequest()
  {
    return (m) -> {
      
      doingThings();
      
      this.pAlive = (a) -> {
        if (a.getContent() instanceof QuorumResponse) {
          QuorumResponse response = (QuorumResponse) a.getContent();
          System.out.printf("Client %d: arrivata risposta di voto dal nodo %d%n", index, response.getVoter());
          
          votes[response.getVoter()] = response.getVote();
          
          int availableNumber = Arrays.asList(votes).stream().filter(vote -> vote.equals(Vote.AVAILABLE)).collect(toList()).size();
          int occupiedNumber = Arrays.asList(votes).stream().filter(vote -> vote.equals(Vote.OCCUPIED)).collect(toList()).size();
          if (!completed && ((action.equals(Action.READ) && availableNumber == Math.min(2, nodes.length)) ||
              (action.equals(Action.WRITE) && availableNumber == nodes.length - 1))) {
            
            completed = true;

            System.out.printf("Client %d: arrivati abbastanza voti per la rischiesta di %s%n", index, action.getAction());

            int replica = random.nextInt();
            ReplicationRequest request = new ReplicationRequest(replica, index, action);
            
            for (int i = 0; i < nodes.length; i++) {
              if (votes[i].equals(Vote.AVAILABLE)) {
                send(nodes[i], request);
              }
            }
          } else if (!completed && occupiedNumber + availableNumber == nodes.length){
            System.out.printf("Client %d: non sono arrivati abbastanza voti per la rischiesta di %s (%d/%d)%n", index,
                action.getAction(), availableNumber, nodes.length - 1);
            ReleaseNode release = new ReleaseNode(index);
            for (int i = 0; i < nodes.length; i++) {
                send(nodes[i], release);
            }
          } else if (completed) {
            send(a.getSender(), new ReleaseNode(index));
          }
        }
        
        return null;
      };
      
      System.out.printf("Client %d: inizio invio richieste di voto di %s a tutti i client%n", index, action.getAction());
      for (int i = 0; i < nodes.length; i++) {
        if (votes[i].equals(Vote.NOT_ARRIVED)) {
          future(nodes[i], new VoteRequest(index), VOTE_TIMEOUT, this.pAlive);
        }
      }
      
      return null;
    };
  }
  
  public enum Vote {
    AVAILABLE,
    OCCUPIED,
    NOT_ARRIVED
  }

}
