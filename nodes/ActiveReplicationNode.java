package it.unipr.sowide.actodes.replication.nodes;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.replication.content.NodeResponse;
import it.unipr.sowide.actodes.replication.content.NodeRequest;

/**
* The ActiveReplicationNode class implements the behavior of a replication node in an active replication algorithm.
**/
public class ActiveReplicationNode extends ReplicationNode {
  
  private static final long serialVersionUID = 1L;

  public ActiveReplicationNode(int index, int nClients)
  {
    super(index);
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
        
        NodeResponse response = doOperation(request);
        send(m, response);
      }
      
      return null;
    };
  }

}
