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
import it.unipr.sowide.actodes.replication.clients.PassiveClient;
import it.unipr.sowide.actodes.replication.clients.QuorumClient;
import it.unipr.sowide.actodes.replication.content.UpdateNodes;
import it.unipr.sowide.actodes.replication.handler.OperationHandler;
import it.unipr.sowide.actodes.replication.nodes.ActiveReplicationNode;
import it.unipr.sowide.actodes.replication.nodes.PassiveReplicationNode;
import it.unipr.sowide.actodes.replication.nodes.QuorumReplicationNode;
import it.unipr.sowide.actodes.service.logging.Logger;

public class Initiator extends Behavior {

  private static final long serialVersionUID = 1L;
  
  private Reference[] nodes;
  private int nClients;
  private int nNodes;
  private String mode;
  
  public Initiator(int nClients, int nNodes, String mode) {
    this.nClients = nClients;
    this.nNodes = nNodes;
    this.mode = mode;
    
    OperationHandler.resetMemory(nNodes);
  }
  
  @Override
  public void cases(CaseFactory c) {
    MessageHandler h = (m) -> {
      if (nNodes > 0) {          
        this.nodes = new Reference[this.nNodes];

        //Creazione dei nodi di replicazione
        for (int i = 0; i < this.nNodes; i++) {
          switch (mode) {
            case "a":
              this.nodes[i] = actor(new ActiveReplicationNode(i, nClients));
              break;
            case "p":
              this.nodes[i] = actor(new PassiveReplicationNode(i, nClients));
              break;
            case "q":
              this.nodes[i] = actor(new QuorumReplicationNode(i, nClients));
              break;
          }
        }
        
        //Broadcast ai nodi di replicazione con la lista dei fratelli
        UpdateNodes update = new UpdateNodes(nodes);
        send(APP, update);
        
        //Creazione dei client
        for (int i = 0; i < this.nClients; i++) {
          switch (mode) {
            case "a":
              actor(new ActiveClient(i, this.nodes));
              break;
            case "p":
              actor(new PassiveClient(i, this.nodes));
              break;
            case "q":
              actor(new QuorumClient(i, this.nodes));
              break;
          }
        }
      }
      
      return Shutdown.SHUTDOWN;
    };
    
    c.define(START, h);
  }

  public static void main(final String[] v) {
    int nNodes = 10;
    int nClients = 10;
    
    Configuration c =  SpaceInfo.INFO.getConfiguration();

    c.setFilter(Logger.ACTIONS);
    
    //c.setLogFilter(new NoCycleProcessing());

    //c.addWriter(new ConsoleWriter());
    
    Scanner scanner = new Scanner(System.in);

    System.out.println("Enter:");
    System.out.println(" a for starting the active replication algorithm");
    System.out.println(" p for starting the passive replication algorithm");
    System.out.println(" q for starting the quorum based replication algorithm");

    String s = scanner.next();

    scanner.close();
    
    c.setExecutor(new ThreadCoordinator(new Initiator(nClients, nNodes, s)));
    
    c.start();
  }
}
