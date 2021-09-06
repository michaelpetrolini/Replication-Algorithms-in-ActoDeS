package it.unipr.sowide.actodes.replication.content;

import it.unipr.sowide.actodes.replication.request.NodeRequest;

public class Forward
{
  private NodeRequest request;
  
  public Forward(NodeRequest req) {
    request = req;
  }

  public NodeRequest getRequest()
  {
    return request;
  }
}
