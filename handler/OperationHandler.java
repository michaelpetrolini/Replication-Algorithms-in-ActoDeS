package it.unipr.sowide.actodes.replication.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the nodes' operations of reading and writing.
**/
public class OperationHandler
{
  
  /**
   * Reads the value previously saved by the client, if it exists.
   *
   * @param node index of the replication node.
   * @param client index of the client.
   * 
   * @return the value previously stored by the client or an empty string.
  **/
  public static String readOperation(int node, int client) throws IOException {
    File f = new File("partitions/node_" + node + ".txt");
    
    if (f.exists()) {
      Path fileName = Path.of("partitions/node_" + node + ".txt");
      
      List<String> lines = Files.readAllLines(fileName).stream().filter(s -> s.contains("Client " + client)).collect(Collectors.toList());
      
      if (!lines.isEmpty()) {
        return lines.get(0).replace("Client " + client + ": ", "");
      }
    }
    
    return "";
  }
  
  /**
   * Writes a value sent by the client on the node.
   *
   * @param node index of the replication node.
   * @param client index of the client.
   * @param value value to be saved on the node
  **/
  public static void writeOperation(int node, int client, int value) throws IOException {
    File f = new File("partitions/node_" + node + ".txt");
    Path fileName = Path.of("partitions/node_" + node + ".txt");

    if (f.exists()) {
      List<String> lines = Files.readAllLines(fileName);
      
      for (int i = 0; i < lines.size(); i++) {
        if (lines.get(i).contains("Client " + client)) {
          lines.set(i, "Client " + client + ": " + value);
          Files.write(fileName, lines);
          return;
        }
      }
    }
    
    Files.write(fileName, ("Client " + client + ": " + value + System.lineSeparator()).getBytes(),
        StandardOpenOption.CREATE, StandardOpenOption.APPEND); 
  }
  
  /**
   * Resets the memory of all replication nodes.
   *
   * @param nNodes number of replication nodes.
  **/
  public static void resetMemory(int nNodes) {
    for (int i = 0; i < nNodes; i++) {
      File f = new File("partitions/node_" + i + ".txt");

      f.delete();
    }
  }

}
