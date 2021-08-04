package it.unipr.sowide.actodes.replication.clients;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.content.NodeRequest;

public class ActiveClient extends Client {
  private static final long serialVersionUID = 1L;

  public ActiveClient(int index, Reference[] nodes)
  {
    super(index, nodes);
    this.action = Action.WRITE;
  }

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

  @Override
  protected int getnNodes()
  {
    return nodes.length;
  }

}
