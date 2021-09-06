package it.unipr.sowide.actodes.replication.request;

import it.unipr.sowide.actodes.replication.clients.Client.Action;

public class RequestStatus
{

  private Action action;
  private int total;
  private int completed;
  
  public RequestStatus(Action action) {
    this.action = action;
    total = 0;
    completed = 0;
  }

  public int getTotal()
  {
    return total;
  }

  public void incrementTotal()
  {
    this.total++;
  }

  public int getCompleted()
  {
    return completed;
  }

  public void incrementCompleted()
  {
    this.completed++;
  }

  public Action getAction()
  {
    return action;
  }
}
