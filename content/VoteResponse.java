package it.unipr.sowide.actodes.replication.content;

import it.unipr.sowide.actodes.replication.clients.QuorumClient.Vote;

public class VoteResponse
{
  private Vote vote;
  private int voter;
  
  public VoteResponse(Vote vote, int voter) {
    this.vote = vote;
    this.voter = voter;
  }

  public Vote getVote()
  {
    return vote;
  }

  public int getVoter()
  {
    return voter;
  }

}
