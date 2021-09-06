package it.unipr.sowide.actodes.replication.content;

public class ForwardHandler
{
  private int current;
  private int total;
  
  public ForwardHandler() {
    current = 0;
    total = 0;
  }

  public int getCurrent()
  {
    return current;
  }

  public void incrementCurrent()
  {
    this.current++;
  }

  public int getTotal()
  {
    return total;
  }

  public void incrementTotal()
  {
    this.total++;
  }

}
