package it.unipr.sowide.actodes.replication.content;

public class Reset
{
  
  private boolean restart;
  
  public Reset(boolean r) {
    setRestart(r);
  }

  public boolean isRestart()
  {
    return restart;
  }

  public void setRestart(boolean restart)
  {
    this.restart = restart;
  }

}
