package it.unipr.sowide.actodes.replication.clients;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.content.ReplicationRequest;

public class PassiveClient extends Client {

  private static final long serialVersionUID = 1L;

  public PassiveClient(int index, Reference[] nodes)
  {
    super(index, nodes);
    this.action = Action.WRITE;
  }

  @Override
  protected MessageHandler receiveResponse()
  {
    return (m) -> {
      System.out.printf("Client %d: replication completed.%n", index);
      return Shutdown.SHUTDOWN;
    };
  }

  @Override
  protected MessageHandler sendRequest()
  {
    return (m) -> {
      if (nodes.length > 0) {
        doingThings();
        
        int replica = random.nextInt();
        System.out.printf("Client %d: starting replication request for element %d%n", index, replica);
        
        send(nodes[0], new ReplicationRequest(replica, index, action));    
      }       
      return null;
    };
  }

}
