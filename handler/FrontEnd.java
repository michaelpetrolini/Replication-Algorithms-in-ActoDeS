package it.unipr.sowide.actodes.replication.handler;

import it.unipr.sowide.actodes.actor.Behavior;
import it.unipr.sowide.actodes.actor.CaseFactory;
import it.unipr.sowide.actodes.actor.Message;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.MessagePattern;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.filtering.constraint.IsInstance;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.ReplicationInitiator.AlgorithmType;
import it.unipr.sowide.actodes.replication.clients.Client.Action;
import it.unipr.sowide.actodes.replication.clients.Client.Vote;
import it.unipr.sowide.actodes.replication.request.FEResponse;
import it.unipr.sowide.actodes.replication.request.NodeRequest;
import it.unipr.sowide.actodes.replication.request.NodeResponse;
import it.unipr.sowide.actodes.replication.request.RequestStatus;
import it.unipr.sowide.actodes.replication.votes.VoteRelease;
import it.unipr.sowide.actodes.replication.votes.VoteRequest;
import it.unipr.sowide.actodes.replication.votes.VoteResponse;
import it.unipr.sowide.actodes.replication.votes.VoteStatus;
import it.unipr.sowide.actodes.replication.content.Forward;
import it.unipr.sowide.actodes.replication.content.ForwardHandler;
import it.unipr.sowide.actodes.replication.content.Reset;
import it.unipr.sowide.actodes.replication.content.Terminate;

/**
 * The FrontEnd class is used to handle communication between clients and replication nodes.  
**/
public class FrontEnd extends Behavior
{

  private static final long serialVersionUID = 1L;
  private static final long VOTE_TIMEOUT = 2000;
  private static final long REQUEST_TIMEOUT = 2000;
  private static final long PASSIVE_TIMEOUT = 5000;

  private static final MessagePattern REQUEST = MessagePattern.contentPattern(new IsInstance(NodeRequest.class));
  private static final MessagePattern RESET = MessagePattern.contentPattern(new IsInstance(Reset.class));
  private static final MessagePattern TERMINATE = MessagePattern.contentPattern(new IsInstance(Terminate.class));
  private static final MessagePattern FORWARD = MessagePattern.contentPattern(new IsInstance(Forward.class));

  private Reference[] nodes;
  private AlgorithmType mode;
  private int left;
  
  public FrontEnd(AlgorithmType mode, Reference[] nodes, int nClients) {
    this.mode = mode;
    this.nodes = nodes;
    this.left = nClients;
  }

  /** {@inheritDoc} **/
  @Override
  public void cases(CaseFactory c)
  {

    c.define(REQUEST, handleRequest());
    
    c.define(RESET, handleReset());
    
    c.define(TERMINATE, handleTerminate());
    
    c.define(FORWARD, handleForward());
    
  }

  /**
   * Handles a client's request based on the type of replication algorithm.
   * 
   * @return a MessageHandler to handle the request sent by a client
  **/
  private MessageHandler handleRequest()
  {
    return (m) -> {
      NodeRequest request = (NodeRequest) m.getContent();
      
      switch (mode) {
        case ACTIVE:
          RequestStatus status = new RequestStatus(request.getAction());
          for (Reference node: nodes) {
            future(node, request, REQUEST_TIMEOUT, handleResponse(m, status, request.getSender(), null));
          }
          break;
        case PASSIVE:
          future(nodes[0], request, PASSIVE_TIMEOUT, handleResponse(m, new RequestStatus(request.getAction()), request.getSender(), null));
          break;
        case QUORUM:
          MessageHandler voteHandler = handleVote(request, m, new VoteStatus(request.getAction(), nodes.length));
          for (Reference node: nodes) {
            future(node, new VoteRequest(request.getSender()), VOTE_TIMEOUT, voteHandler);
          }
          break;
      }
      
      return null;
    };
  }
  
  
  /**
  * Manages the voting session and decides what to do based on the number of positive votes.
  * @param request the client's request to which the voting session must decide upon 
  * @param m the message to which send the voting result
  * @param voteStatus the status of the current voting session 
  * 
  * @return a MessageHandler to handle the votes arriving from the replication nodes.
  **/
  private MessageHandler handleVote(NodeRequest request, Message m, VoteStatus voteStatus)
  {
    return (a) -> {
      int sender = request.getSender();
      Action action = voteStatus.getAction();
      voteStatus.incrementTotalVotes();
      
      if (a.getContent() instanceof VoteResponse) {
        VoteResponse response = (VoteResponse) a.getContent();
        if (voteStatus.isCompleted()) {
          send(a.getSender(), new VoteRelease(sender));
        } 
        else {
          voteStatus.setVote(response.getVoter(), response.getVote());
        }
      }
      
      
      
      if (voteStatus.hasEnoughVotes()) {
        
        voteStatus.setCompleted(true);

        System.out.printf("FE: Client %d has enough votes for the %s request%n",
            sender, action.getAction());

        RequestStatus status = new RequestStatus(voteStatus.getAction());
        for (int i = 0; i < nodes.length; i++) {
          if (voteStatus.getVotes()[i].equals(Vote.AVAILABLE)) {
            future(nodes[i], request, REQUEST_TIMEOUT, handleResponse(m, status, sender, voteStatus));
          }
        }
      }
      
      if (voteStatus.getTotalVotes() == nodes.length && !voteStatus.isCompleted())
      {
        System.out.printf("FE: Client %d doesn't have enough votes for the %s request (%d/%d)%n", sender, action.getAction(),
            voteStatus.getAvailables(), (action.equals(Action.WRITE))? nodes.length - 1: 2);
        
        releaseNodes(sender, voteStatus.getVotes());
        send(m, new FEResponse(0, 0, false));
      }
      
      return null;
    };
  }
  

  /**
   * Handles the nodes' responses and replies to the client.
   * @param m the requesting client
   * @param status the status of the current client's request
   * @param sender the client's index
   * @param voteStatus the status of the current voting session
   * 
   * @return a MessageHandler to handle the FE response
   */
  private MessageHandler handleResponse(Message m, RequestStatus status, int sender, VoteStatus voteStatus)
  {
    return (r) -> {
      status.incrementTotal();
      
      if (r.getContent() instanceof NodeResponse) {
        status.incrementCompleted();
      }
      
      switch (mode) {
        case ACTIVE:          
          if (status.getTotal() == nodes.length) {
            send(m, new FEResponse(status.getCompleted(), status.getTotal(), status.getCompleted() > 0));
          }
          break;
        case PASSIVE:
          send(m, new FEResponse(status.getCompleted(), 1, status.getCompleted() > 0));
          break;
        case QUORUM:
          if ((status.getAction().equals(Action.READ) && status.getTotal() == 2) ||
              (status.getAction().equals(Action.WRITE) && status.getTotal() == nodes.length - 1)) {
            releaseNodes(sender, voteStatus.getVotes());
            send(m, new FEResponse(status.getCompleted(), status.getTotal(), status.getCompleted() > 0));
          }
          break;
      }
      return null;
    };
  }

  /**
   * Releases the nodes who sent a positive vote
   * 
   * @param sender the client's index
   * @param votes the votes of the current voting session
   */
  private void releaseNodes(int sender, Vote[] votes)
  {
    for (int i = 0; i < nodes.length; i++) {
      if (votes[i].equals(Vote.AVAILABLE)) {
        send(nodes[i], new VoteRelease(sender));
      }
    }
    
  }
  
  /**
   * Sends a Reset message to the client
   * 
   * @return a MessageHandler to send a Reset message to the client
   */
  private MessageHandler handleReset()
  {
    return (m) -> {
      send(m.getSender(), new Reset(true));
      return null;
    };
  }
  
  /**
   * Sends a Terminate message to the replication nodes
   * 
   * @return a MessageHandler to send a Terminate message to the replication node
   */
  private MessageHandler handleTerminate() {
    return (m) -> {
      left--;
      
      if (left == 0) {
        send(APP, new Terminate());
        return Shutdown.SHUTDOWN;
      }
      
      return null;
    };
  }
  
  /**
   * Forwards a primary node's message to all secondary nodes
   * 
   * @return a MessageHandler to forward a primary node's message
   */
  private MessageHandler handleForward() {
    return (m) -> {
      Forward forward = (Forward) m.getContent();
      MessageHandler handler = handleForwardResponse(m, new ForwardHandler());
      for (int i = 1; i < nodes.length; i++) {
        future(nodes[i], forward.getRequest(), REQUEST_TIMEOUT, handler);
      }
      return null;
    };
  }
  
  /**
   * Handles the secondary nodes' responses.
   * 
   * @param m the primary node's original message
   * @param handler the status of the forwarding process
   * 
   * @return a MessageHandler to get the secondary nodes' responses
   */
  private MessageHandler handleForwardResponse(Message m, ForwardHandler handler) {
    return (r) -> {
      handler.incrementTotal();
      
      if (r.getContent() instanceof NodeResponse) {
        handler.incrementCurrent();
      }
      
      if (handler.getTotal() == nodes.length - 1) {
        send(m, handler);
      }
      
      return null;
    };
  }
}
