package edu.upenn.cis.cis455;

import org.apache.logging.log4j.Level;

import java.util.*;
import static edu.upenn.cis.cis455.WebServiceController.*;

public class WebServer {
    public static void main(String[] args) {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
        // TODO: make sure you parse *BOTH* command line arguments properly
        System.out.println(Arrays.toString(args));
        int port = Integer.parseInt(args[0]);
        String rootPath = args[1];
//        String rootPath = "./www";
//        int port = 8888;
        // TODO: launch your server daemon  
        staticFileLocation(rootPath);
        port(port);
//        get("/", (req, res) -> {
//        	return "Hello world";
//        });
        System.out.println("Waiting to handle requests!");
            
    }

}
