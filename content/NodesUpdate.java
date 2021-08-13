package it.unipr.sowide.actodes.replication.content;

import it.unipr.sowide.actodes.registry.Reference;

/**
 * The NodesUpdate class contains the list of replication nodes within the application.
**/
public class NodesUpdate
{
  
  private Reference[] nodes;
  
  public NodesUpdate(Reference[] nodes) {
    this.nodes = nodes;
  }

  public Reference[] getNodes()
  {
    return nodes;
  }

}
