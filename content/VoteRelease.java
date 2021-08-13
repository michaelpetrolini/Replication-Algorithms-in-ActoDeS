package it.unipr.sowide.actodes.replication.content;

/**
 * The VoteRelease class contains the details of the clients who wants to release a replication node from its vote.
**/
public class VoteRelease
{
  private int id;
  
  public VoteRelease(int id) {
    this.id = id;
  }

  public int getId()
  {
    return id;
  }
  
}
