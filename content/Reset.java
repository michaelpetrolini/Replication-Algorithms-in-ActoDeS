package it.unipr.sowide.actodes.replication.content;

/**
 * The Reset class handles the request and the response to manage the restarting of the replication process.
**/
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
