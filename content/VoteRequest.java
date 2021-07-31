package it.unipr.sowide.actodes.replication.content;

public class VoteRequest
{
  private int requester;
  
  public VoteRequest(int requester) {
    this.requester = requester;
  }

  public int getRequester()
  {
    return requester;
  }

}
