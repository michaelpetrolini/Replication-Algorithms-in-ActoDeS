package it.unipr.sowide.actodes.replication.content;

import it.unipr.sowide.actodes.replication.clients.Client.Action;

public class ReplicationRequest {
  
  private int replica;
  private int sender;
  private Action action;
  
  public ReplicationRequest(int replica, int sender, Action action) {
    this.replica = replica;
    this.sender = sender;
    this.action = action;
  }

  public int getReplica() {
    return replica;
  }

  public int getSender() {
    return sender;
  }

  public Action getAction()
  {
    return action;
  }

}
