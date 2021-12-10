package org.bigbase.textprovider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Main service launcher 
 */
public class Server {
  
  private static final Logger log = LogManager.getLogger(Server.class);
  /*
   * Worker thread pool
   */
  private static ExecutorService pool;

  /*
   * Server's port number
   */
  private static int port = 10322;
  
  /*
   * Text file path
   */
  private static String filePath;

  
  public static void main(String[] args) {
    if (args.length != 1) {
      usage();
    }
    try {
      startServer(args[0]);
    } catch (IOException e) {
      exitWithError("Server aborted", e);
    }
  }

  private static void startServer(String filePath) throws IOException {
    
    info(String.format("Starting Text Provider Server for file: %s", filePath));    
    // Initialize worker pool
    int threadPoolSize = Runtime.getRuntime().availableProcessors() / 2;

    // One more check - for text file
    if (!Files.exists(Path.of(filePath))) {
      exitWithError(String.format("Text file '%s' does not exist", filePath), null);
    }
    
    Server.filePath = filePath;
    
    // Create thread pool with a bounded queue
    pool = new ThreadPoolExecutor(threadPoolSize, threadPoolSize,
      0L, TimeUnit.MILLISECONDS,
      new ArrayBlockingQueue<Runnable>(threadPoolSize * 16));
        
    // Load index
    FileIndex.openOrCreate(filePath);
    // Start network server
    runServer();
    
  }
  /**
   * Starts networks server, opens server socket,
   * binds it to a local host:port, initialize selector
   * and starts listening incoming requests
   * 
   * @throws IOException
   */
  private static void runServer() throws IOException {
    final Selector selector = Selector.open(); // selector is open here
    log.debug("Selector started");

    ServerSocketChannel serverSocket = ServerSocketChannel.open();
    log.debug("Server socket opened");

    InetSocketAddress serverAddr = new InetSocketAddress("localhost", port);

    // Binds the channel's socket to a local address and configures the socket to listen for
    // connections
    serverSocket.bind(serverAddr);
    // Adjusts this channel's blocking mode.
    serverSocket.configureBlocking(false);
    int ops = serverSocket.validOps();
    // Register selector
    serverSocket.register(selector, ops, null);
    log.debug("[{}] Server started on port: {}", Thread.currentThread().getName(), port);

    // Infinite loop..
    // Keep server running
    while (true) {
      // Selects a set of keys whose corresponding channels are ready for I/O operations
      int readyChannels = selector.select();
      if (readyChannels == 0) {
        continue;
      }
      Set<SelectionKey> keys = selector.selectedKeys();
      Iterator<SelectionKey> it = keys.iterator();

      while (it.hasNext()) {
        SelectionKey key = it.next();
        it.remove();
        try {
          if (key.isValid() && key.isAcceptable()) {
            SocketChannel client = serverSocket.accept();
            // Adjusts this channel's blocking mode to false
            client.configureBlocking(false);
            client.setOption(StandardSocketOptions.TCP_NODELAY, true);
            client.setOption(StandardSocketOptions.SO_SNDBUF, 64 * 1024);
            client.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);
            // Operation-set bit for read operations
            client.register(selector, SelectionKey.OP_READ);
            log.debug("[{}] Connection Accepted: {}", Thread.currentThread().getName(),
              client.getLocalAddress());
          } else if (key.isValid() && key.isReadable() && key.attachment() == null) {
            // process request - submit task
            key.attach(new Object()); // mark this key as in-use
            submitRequest(key);
          }
        } catch (IOException e) {
          log.error("StackTrace: ", e);
        }
      }
    }
  }
  
  private static void submitRequest(SelectionKey key) throws IOException {
    RequestHandler sender = new RequestHandler(filePath, key);
    pool.submit(sender);
  }


  private static void usage() {
    exitWithError("Usage: java org.bigbase.textprovider.Server text_file_path", null);
  }

  static void info(String str) {
    log.info("[{}] {}", Thread.currentThread().getName(), str);
  }
  
  static void error(String str) {
    log.error("[{}] {}", Thread.currentThread().getName(), str);
  }
  
  static void exitWithError(String str, Exception e) {
    log.error("[{}] {}", Thread.currentThread().getName(), str, e);
    System.exit(-1);
  }
  
  public static synchronized void shutdown() {
    
    new Thread( () -> {
      pool.shutdown(); // Disable new tasks from being submitted
      try {
        // Wait a while for existing tasks to terminate
        if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
          pool.shutdownNow(); // Cancel currently executing tasks
          // Wait a while for tasks to respond to being cancelled
          if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
            error("Pool did not terminate");
          }
        }
      } catch (InterruptedException ie) {
        // (Re-)Cancel if current thread also interrupted
        pool.shutdownNow();
      }
      // Exit server
      System.exit(0);
    }).start();
  }
}

