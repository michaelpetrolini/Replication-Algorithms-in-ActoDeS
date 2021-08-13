package it.unipr.sowide.actodes.replication.handler;

import it.unipr.sowide.actodes.actor.Behavior;
import it.unipr.sowide.actodes.actor.CaseFactory;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.MessagePattern;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.filtering.constraint.IsInstance;
import it.unipr.sowide.actodes.replication.content.Reset;

/**
 * The ClientManager class is used to handle the life cycle of the clients and the replication nodes.  
**/
public class ClientHandler extends Behavior
{

  private static final long serialVersionUID = 1L;
  private static final MessagePattern RESET = MessagePattern.contentPattern(new IsInstance(Reset.class));
  
  private int nOperations;
  private int current;
  private int nClients;
  private int currentClients;
  
  public ClientHandler(int nOperations, int nClients) {
    this.nOperations = nOperations - nClients;
    this.nClients = nClients;
    this.current = 0;
    this.currentClients = 0;
  }

  /** {@inheritDoc} **/
  @Override
  public void cases(CaseFactory c)
  {
    MessageHandler h = handleResetRequest();

    c.define(RESET, h);
  }

  /**
   * Decides whether to restart a client, terminate it or terminate all replication nodes.
   * 
   * @return a MessageHandler to handle the reset request sent by a client
  **/
  private MessageHandler handleResetRequest()
  {
    return (m) -> {
      current++;
      if (current <= nOperations) {
        send(m.getSender(), new Reset(true));
      } else {
        currentClients++;
        
        if (currentClients < nClients) {
          send(m.getSender(), new Reset(false));
        } else {
          send(APP, new Reset(false));
          
          return Shutdown.SHUTDOWN;
        }
      }
      
      return null;
    };
  }

}
