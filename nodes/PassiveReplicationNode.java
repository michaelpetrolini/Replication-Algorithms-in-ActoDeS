package it.unipr.sowide.actodes.replication.nodes;

import it.unipr.sowide.actodes.actor.Message;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.replication.content.NodeResponse;
import it.unipr.sowide.actodes.replication.content.NodeRequest;

/**
* The PassiveReplicationNode class implements the behavior of a replication node in a passive replication algorithm.
**/
public class PassiveReplicationNode extends ReplicationNode {
  
  private static final long serialVersionUID = 1L;
  private static final int REPLICATION_TIMEOUT = 2000;

  private int[] completed;
  private int [] total;

  public PassiveReplicationNode(int index, int nClients)
  {
    super(index);
    
    if (index == 0) {
      completed = new int[nClients];
      total = new int[nClients];
    }
  }

  /**
   * If the replication node is primary it saves the client's value, forwards it to all secondary
   * nodes and then waits for their response. If the replication node is secondary it simply
   * saves the client's value and replies to the primary node.
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
                    
          for (int i = 1; i < nodes.length; i++) {
            future(nodes[i], request, REPLICATION_TIMEOUT, handler);
          }
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
   **/
  private MessageHandler handleResponse(Message message, int clientIndex)
  {
    return (k) -> {
      if (isWorking()) {
        total[clientIndex] = total[clientIndex] + 1;
        
        if (k.getContent() instanceof NodeResponse) { 
          completed[clientIndex] = completed[clientIndex] + 1;
        }
        
        if (total[clientIndex] == nodes.length) {
          System.out.printf("Primary Node %d: received all responses for request sent by client %d (%d/%d)%n",
              index, clientIndex, completed[clientIndex], total[clientIndex]);
          
          send(message, new NodeResponse(index, null, null));
        }  
      }

      return null;
    };
  }
}
