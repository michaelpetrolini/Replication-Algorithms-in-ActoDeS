package it.unipr.sowide.actodes.replication.clients;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.content.NodeRequest;

/**
* The ActiveClient class implements the behavior of a client in an active replication algorithm.
**/
public class ActiveClient extends Client {
  private static final long serialVersionUID = 1L;

  public ActiveClient(int index, Reference[] nodes, Reference manager)
  {
    super(index, nodes, manager);
    this.action = Action.WRITE;
  }

  /**
  * Sends a request to all replication nodes and waits for their response.
  **/
  @Override
  protected void sendRequest()
  {
    if (nodes.length > 0) {
      doingThings();
      
      int replica = random.nextInt();
      System.out.printf("Client %d: starting replication request for element %d%n", index, replica);
      
      MessageHandler handler = handleResponse();
      
      for (Reference node: nodes) {
        future(node, new NodeRequest(replica, index, action), REQUEST_TIMEOUT, handler);
      }
    }
  }

  /**{@inheritDoc}**/
  @Override
  protected int getnNodes()
  {
    return nodes.length;
  }

}
