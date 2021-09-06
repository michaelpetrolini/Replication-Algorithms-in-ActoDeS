package it.unipr.sowide.actodes.replication.votes;

import java.util.Arrays;

import it.unipr.sowide.actodes.replication.clients.Client.Action;
import it.unipr.sowide.actodes.replication.clients.Client.Vote;

public class VoteStatus
{
  private Vote[] votes;
  private int totalVotes;
  private int nNodes;
  private boolean completed;
  private Action action;
  
  public VoteStatus(Action action, int nNodes) {
    this.action = action;
    this.nNodes = nNodes;
    votes = new Vote[nNodes];
    Arrays.fill(votes, Vote.NOT_ARRIVED);
    setCompleted(false);
    totalVotes = 0;
  }

  public int getTotalVotes()
  {
    return totalVotes;
  }
  
  public void incrementTotalVotes() {
    totalVotes++;
  }

  public boolean isCompleted()
  {
    return completed;
  }

  public void setCompleted(boolean completed)
  {
    this.completed = completed;
  }

  public Action getAction()
  {
    return action;
  }
  
  public void setVote(int voter, Vote vote) {
    votes[voter] = vote;
  }
  
  public Vote[] getVotes() {
    return votes;
  }
  
  public boolean hasEnoughVotes() {
    long nAvailable = getAvailables();
    
    return !completed && ((action.equals(Action.READ) && nAvailable == 2) ||
        (action.equals(Action.WRITE) && nAvailable == nNodes - 1));
  }

  public long getAvailables() {
    return Arrays.asList(votes).stream().filter(vote -> vote.equals(Vote.AVAILABLE)).count();
  }
}
