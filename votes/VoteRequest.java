package it.unipr.sowide.actodes.replication.votes;

/**
 * The VoteRequest class contains the details of the clients who asks a replication for a vote.
**/
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
