package it.unipr.sowide.actodes.replication.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OperationHandler
{
  
  public static String readOperation(int node, int client) throws IOException {
    File f = new File("partitions/node_" + node + ".txt");
    
    if (f.exists()) {
      Path fileName = Path.of("partitions/node_" + node + ".txt");
      
      List<String> lines = Files.readAllLines(fileName).parallelStream().filter(s -> s.matches("Client " + client)).collect(Collectors.toList());
      
      if (!lines.isEmpty()) {
        Pattern pattern = Pattern.compile("-?\\d+");
        Matcher matcher = pattern.matcher(lines.get(0));
        
        return matcher.group();
      }
    }
    
    return "";
  }
  
  public static void writeOperation(int node, int client, int value) throws IOException {
    File f = new File("partitions/node_" + node + ".txt");
    Path fileName = Path.of("partitions/node_" + node + ".txt");

    if (f.exists()) {
      List<String> lines = Files.readAllLines(fileName);
      
      for (int i = 0; i < lines.size(); i++) {
        if (lines.get(i).matches("Client " + client)) {
          lines.set(i, "Client " + client + ": " + value + System.lineSeparator());
          Files.write(fileName, lines);
          return;
        }
      }
    }
    
    Files.write(fileName, ("Client " + client + ": " + value + System.lineSeparator()).getBytes(),
        StandardOpenOption.CREATE, StandardOpenOption.APPEND); 
  }
  
  public static void resetMemory(int nNodes) {
    for (int i = 0; i < nNodes; i++) {
      File f = new File("partitions/node_" + i + ".txt");

      f.delete();
    }
  }

}
