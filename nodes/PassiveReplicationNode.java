package it.unipr.sowide.actodes.replication.nodes;

import it.unipr.sowide.actodes.actor.Message;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.request.NodeRequest;
import it.unipr.sowide.actodes.replication.request.NodeResponse;
import it.unipr.sowide.actodes.replication.content.Forward;
import it.unipr.sowide.actodes.replication.content.ForwardHandler;

/**
* The PassiveReplicationNode class implements the behavior of a replication node in a passive replication algorithm.
**/
public class PassiveReplicationNode extends ReplicationNode {
  
  private static final long serialVersionUID = 1L;
  private static final int REPLICATION_TIMEOUT = 5000;

  private int[] completed;
  private int [] total;
  private Reference fe;

  public PassiveReplicationNode(int index, int nClients, Reference fe)
  {
    super(index);
    
    if (index == 0) {
      completed = new int[nClients];
      total = new int[nClients];
      this.fe = fe;
    }
  }

  /**
   * If the replication node is primary it saves the client's value, forwards it to all secondary
   * nodes and then waits for their response. If the replication node is secondary it simply
   * saves the client's value and replies to the primary node.
   * 
   * @return a MessageHandler to handle the request coming from clients or the primary node.
   **/
  @Override
  protected MessageHandler handleRequest()
  {
    return (m) -> {
      if (isWorking()) {
        NodeRequest request = (NodeRequest) m.getContent();

        if (index == 0) {
          System.out.printf("Primary Node %d: received request by client %d%n", index, request.getSender());
          doOperation(request);

          completed[request.getSender()] = 1;
          total[request.getSender()] = 1;
          MessageHandler handler = handleResponse(m, request.getSender());
          
          future(fe, new Forward(request), REPLICATION_TIMEOUT, handler);
        } else {        

          NodeResponse response = doOperation(request);
          send(m, response);
        }
      }
            
      return null;
    };
  }

  /**
   * Implements the primary node's behavior when it receives a secondary node's response.
   * 
   * @param message the original client message.
   * @param clientIndex index of the client asking for replication.
   * 
   * @return a MessageHandler to handle the secondary nodes' responses.
   **/
  private MessageHandler handleResponse(Message message, int clientIndex)
  {
    return (k) -> {
      if (isWorking()) {
        total[clientIndex] = total[clientIndex] + 1;
        
        if (k.getContent() instanceof ForwardHandler) { 
          ForwardHandler fh = (ForwardHandler) k.getContent();
          System.out.printf("Primary Node %d: received all responses for request sent by client %d (%d/%d)%n",
              index, clientIndex, fh.getCurrent(), fh.getTotal());
          send(message, new NodeResponse(index, null, null));
        } 
      }

      return null;
    };
  }
}
