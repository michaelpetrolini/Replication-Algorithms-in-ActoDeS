package it.unipr.sowide.actodes.replication.content;

import it.unipr.sowide.actodes.registry.Reference;

public class UpdateNodes
{
  
  private Reference[] nodes;
  
  public UpdateNodes(Reference[] nodes) {
    this.nodes = nodes;
  }

  public Reference[] getNodes()
  {
    return nodes;
  }

}
