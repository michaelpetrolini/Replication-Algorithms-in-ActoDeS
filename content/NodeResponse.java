package it.unipr.sowide.actodes.replication.content;

public class NodeResponse
{
  private NodeRequest request;
  private int nodeIndex;
  private String response;
  
  public NodeResponse(int index, NodeRequest request, String response) {
    this.nodeIndex = index;
    this.request = request;
    this.response = response;
  }

  public int getNodeIndex()
  {
    return nodeIndex;
  }

  public NodeRequest getRequest()
  {
    return request;
  }

  public String getResponse()
  {
    return response;
  }

}
