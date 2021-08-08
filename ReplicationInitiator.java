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
import it.unipr.sowide.actodes.replication.clients.ActiveClient;
import it.unipr.sowide.actodes.replication.clients.ClientManager;
import it.unipr.sowide.actodes.replication.clients.PassiveClient;
import it.unipr.sowide.actodes.replication.clients.QuorumClient;
import it.unipr.sowide.actodes.replication.content.UpdateNodes;
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
  private String mode;
  
  public ReplicationInitiator(int nClients, int nNodes, String mode, int nOperations) {
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
      if (nNodes > 0 && nClients > 0 && nOperations >= nClients) {          
        nodes = new Reference[nNodes];

        //Creazione dei nodi di replicazione
        for (int i = 0; i < nNodes; i++) {
          switch (mode) {
            case "a":
              nodes[i] = actor(new ActiveReplicationNode(i, nClients));
              break;
            case "p":
              nodes[i] = actor(new PassiveReplicationNode(i, nClients));
              break;
            case "q":
              nodes[i] = actor(new QuorumReplicationNode(i, nClients));
              break;
          }
        }
        
        //Broadcast ai nodi di replicazione con la lista dei fratelli
        send(APP, new UpdateNodes(nodes));
        
        Reference manager = actor(new ClientManager(nOperations, nClients));
        
        //Creazione dei client
        for (int i = 0; i < nClients; i++) {
          switch (mode) {
            case "a":
              actor(new ActiveClient(i, nodes, manager));
              break;
            case "p":
              actor(new PassiveClient(i, nodes, manager));
              break;
            case "q":
              actor(new QuorumClient(i, nodes, manager));
              break;
          }
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
    int nClients = 10;
    int nOperations = 50;
    boolean actodesVerbose = false;
    
    Configuration c =  SpaceInfo.INFO.getConfiguration();

    c.setFilter(Logger.ACTIONS);
    
    if (actodesVerbose) {
      c.setLogFilter(new NoCycleProcessing());

      c.addWriter(new ConsoleWriter()); 
    }
    
    Scanner scanner = new Scanner(System.in);

    System.out.println("Enter:");
    System.out.println(" a for starting the active replication algorithm");
    System.out.println(" p for starting the passive replication algorithm");
    System.out.println(" q for starting the quorum based replication algorithm");

    String s = scanner.next();

    scanner.close();
    
    c.setExecutor(new ThreadCoordinator(new ReplicationInitiator(nClients, nNodes, s, nOperations)));
    
    c.start();
  }
}
