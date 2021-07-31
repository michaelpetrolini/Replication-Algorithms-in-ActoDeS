package it.unipr.sowide.actodes.replication.content;

public class ReplicaResponse
{
  private ReplicationRequest request;
  private int nodeIndex;
  private String response;
  
  public ReplicaResponse(int index, ReplicationRequest request, String response) {
    this.nodeIndex = index;
    this.request = request;
    this.response = response;
  }

  public int getNodeIndex()
  {
    return nodeIndex;
  }

  public ReplicationRequest getRequest()
  {
    return request;
  }

  public String getResponse()
  {
    return response;
  }

}
