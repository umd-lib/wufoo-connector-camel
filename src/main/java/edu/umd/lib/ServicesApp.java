package edu.umd.lib;

import org.apache.camel.main.Main;

import edu.umd.lib.routes.WofooListener;

public class ServicesApp {

  /**
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    ServicesApp hub = new ServicesApp();
    hub.boot();
  }

  public void boot() throws Exception {
    Main app = new Main();
    // enable the shutdown hook
    app.enableHangupSupport();
    app.addRouteBuilder(new WofooListener());
    // do .run() instead of .start()
    app.run();
  }
}
