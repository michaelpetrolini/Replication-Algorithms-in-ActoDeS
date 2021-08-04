package it.unipr.sowide.actodes.replication.nodes;

import it.unipr.sowide.actodes.actor.Message;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.replication.content.NodeResponse;
import it.unipr.sowide.actodes.replication.content.NodeRequest;

public class PassiveReplicationNode extends ReplicationNode {
  
  private static final long serialVersionUID = 1L;
  
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

  @Override
  protected MessageHandler handleRequest()
  {
    return (m) -> {
      if (isWorking()) {
        NodeRequest request = (NodeRequest) m.getContent();

        if (index == 0) {
          System.out.printf("Primary Node %d: received request by client %d%n", index, request.getSender());
          doOperation(request);

          completed[request.getSender()] = 0;
          total[request.getSender()] = 0;
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

  private MessageHandler handleResponse(Message message, int clientIndex)
  {
    return (k) -> {
      if (isWorking()) {
        total[clientIndex] = total[clientIndex] + 1;
        
        if (k.getContent() instanceof NodeResponse) {
          NodeResponse response = (NodeResponse) k.getContent();
          System.out.printf("Primary Node %d: received response from node %d for request sent by client %d%n",
              index, response.getNodeIndex(), clientIndex);
          
          completed[clientIndex] = completed[clientIndex] + 1;
        }
        
        if (total[clientIndex] == nodes.length - 1) {
          System.out.printf("Primary Node %d: received all responses for request sent by client %d (%d/%d)%n",
              index, clientIndex, completed[clientIndex], total[clientIndex]);
          
          send(message, new NodeResponse(index, null, null));
        }  
      }

      return null;
    };
  }
}
