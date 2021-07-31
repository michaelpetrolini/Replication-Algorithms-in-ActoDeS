package it.unipr.sowide.actodes.replication;

import java.util.Random;

import it.unipr.sowide.actodes.replication.nodes.ReplicationNode;

public class ErrorHandler extends Thread {

  private static final float ERROR_PROBABILITY = 0.05f;
  private static final float RECOVERY_PROBABILITY = 0.2f;
  private static final long DURATION = 200;
  
  private ReplicationNode node;
  private Random random;
  private int index;
  
  public ErrorHandler(ReplicationNode node, int index) {
    this.node = node;
    this.random = new Random();
    this.index = index;
  }
  
  @Override
  public void run()
  {
    while (true) {
      if (node.isWorking()) {
        if (random.nextFloat() <= ERROR_PROBABILITY) {
          node.setIsWorking(false);
          System.out.printf("Replication Node %d: STOPPED WORKING%n", index);
        }
      } else {
        if (random.nextFloat() <= RECOVERY_PROBABILITY) {
          node.setIsWorking(true);
          node.handleRecovery();
          System.out.printf("Replication Node %d: RECOVERED%n", index);
        }
      }
      
      try
      {
        Thread.sleep(DURATION);
      }
      catch (InterruptedException e)
      {
        e.printStackTrace();
      }
    }
    
  }

}
