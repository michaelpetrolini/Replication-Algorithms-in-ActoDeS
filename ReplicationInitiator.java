package it.unipr.sowide.actodes.replication;


import java.util.Scanner;

import it.unipr.sowide.actodes.actor.Behavior;
import it.unipr.sowide.actodes.actor.CaseFactory;
import it.unipr.sowide.actodes.actor.MessageHandler;
import it.unipr.sowide.actodes.actor.Shutdown;
import it.unipr.sowide.actodes.configuration.Configuration;
import it.unipr.sowide.actodes.controller.SpaceInfo;
import it.unipr.sowide.actodes.executor.active.ThreadCoordinator;
import it.unipr.sowide.actodes.registry.Reference;
import it.unipr.sowide.actodes.replication.clients.Client;
import it.unipr.sowide.actodes.replication.handler.FrontEnd;
import it.unipr.sowide.actodes.replication.handler.OperationHandler;
import it.unipr.sowide.actodes.replication.nodes.ActiveReplicationNode;
import it.unipr.sowide.actodes.replication.nodes.PassiveReplicationNode;
import it.unipr.sowide.actodes.replication.nodes.QuorumReplicationNode;
import it.unipr.sowide.actodes.service.logging.ConsoleWriter;
import it.unipr.sowide.actodes.service.logging.Logger;
import it.unipr.sowide.actodes.service.logging.util.NoCycleProcessing;

/**
 * The ReplicationInitiator class handler the creation of clients and replication nodes.  
**/
public class ReplicationInitiator extends Behavior {

  private static final long serialVersionUID = 1L;
  
  private Reference[] nodes;
  private int nClients;
  private int nNodes;
  private int nOperations;
  private AlgorithmType mode;
  
  public ReplicationInitiator(int nClients, int nNodes, AlgorithmType mode, int nOperations) {
    this.nClients = nClients;
    this.nNodes = nNodes;
    this.mode = mode;
    this.nOperations = nOperations;
    
    OperationHandler.resetMemory(nNodes);
  }
  
  /**{@inheritDoc}**/
  @Override
  public void cases(CaseFactory c) {
    MessageHandler h = (m) -> {
      if (nNodes > 0 && nClients > 0) {  
        
        nodes = new Reference[nNodes];
        
        Reference fe = actor(new FrontEnd(mode, nodes, nClients));

        for (int i = 0; i < nNodes; i++) {
          switch (mode) {
            case ACTIVE:
              nodes[i] = actor(new ActiveReplicationNode(i));
              break;
            case PASSIVE:
              nodes[i] = actor(new PassiveReplicationNode(i, nClients, fe));
              break;
            case QUORUM:
              nodes[i] = actor(new QuorumReplicationNode(i));
              break;
          }
        }
                
        for (int i = 0; i < nClients; i++) {
          actor(new Client(i, fe, nOperations));
        }
      }
      
      return Shutdown.SHUTDOWN;
    };
    
    c.define(START, h);
  }

  /**
   * Starts the clients and the replication nodes depending on the type of replication algorithm chosen.
   *
   * @param v  the arguments.
   *
   * It does not need arguments.
   *
  **/
  public static void main(final String[] v) {
    int nNodes = 10;
    int nClients = 3;
    int nOperations = 5;
    boolean actodesVerbose = false;
    
    Configuration c =  SpaceInfo.INFO.getConfiguration();

    c.setFilter(Logger.ACTIONS);
    
    if (actodesVerbose) {
      c.setLogFilter(new NoCycleProcessing());

      c.addWriter(new ConsoleWriter()); 
    }
    
    Scanner scanner = new Scanner(System.in);

    System.out.println("Enter:");
    System.out.println(" a to start the active replication algorithm");
    System.out.println(" p to start the passive replication algorithm");
    System.out.println(" q to start the quorum based replication algorithm");

    String s = scanner.next();

    scanner.close();
    
    AlgorithmType type = null;
    
    switch (s) {
      case "a":
        type = AlgorithmType.ACTIVE;
        break;
      case "p":
        type = AlgorithmType.PASSIVE;
        break;
      case "q":
        type = AlgorithmType.QUORUM;
        break;
    }
    
    c.setExecutor(new ThreadCoordinator(new ReplicationInitiator(nClients, nNodes, type, nOperations)));
    
    c.start();
  }
  
  public enum AlgorithmType {
    ACTIVE,
    PASSIVE,
    QUORUM
  }
}
