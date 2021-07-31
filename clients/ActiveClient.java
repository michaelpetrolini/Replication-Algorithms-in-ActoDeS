package it.unipr.sowide.actodes.replication.clients;

import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.content.ReplicationRequest;

public class ActiveClient extends Client {
  private static final long serialVersionUID = 1L;

  public ActiveClient(int index, Reference[] nodes)
  {
    super(index, nodes);
    this.action = Action.WRITE;
  }

  @Override
  protected MessageHandler sendRequest()
  {
    return (m) -> {
      if (nodes.length > 0) {
        doingThings();
        
        int replica = random.nextInt();
        System.out.printf("Client %d: starting replication request for element %d%n", index, replica);
        
        send(APP, new ReplicationRequest(replica, index, action));
      }       
      return null;
    };
  }

  @Override
  protected MessageHandler receiveResponse()
  {
    return (m) -> {
      received++;
      if (received == nodes.length) {
        System.out.printf("Client %d: replication completed.%n", index);
        return Shutdown.SHUTDOWN;
      }
      
      return null;
    };
  }

}
