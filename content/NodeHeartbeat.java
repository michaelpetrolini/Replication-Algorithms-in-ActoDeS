package it.unipr.sowide.actodes.replication.content;

public class NodeHeartbeat
{
  
  private Boolean[] completedClients;
  
  public NodeHeartbeat(Boolean[] completedClients) {
    this.completedClients = completedClients;
  }

  public Boolean[] getCompletedClients()
  {
    return completedClients;
  }
  
}
