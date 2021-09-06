package it.unipr.sowide.actodes.replication.request;

public class FEResponse
{
  private int completed;
  private int total;
  private boolean success;
  
  public FEResponse(int completed, int total, boolean success) {
    this.completed = completed;
    this.total = total;
    this.success = success;
  }

  public int getCompleted()
  {
    return completed;
  }

  public int getTotal()
  {
    return total;
  }

  public boolean isSuccess()
  {
    return success;
  }

}
