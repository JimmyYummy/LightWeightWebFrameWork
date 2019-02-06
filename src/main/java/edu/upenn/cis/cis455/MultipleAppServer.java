package edu.upenn.cis.cis455;

import org.apache.logging.log4j.Level;

import edu.upenn.cis.cis455.m1.server.implementations.Application;
import edu.upenn.cis.cis455.m1.server.implementations.MultipleAppWebService;

import java.util.*;

public class MultipleAppServer {
    public static void main(String[] args) {
    	
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
        MultipleAppWebService webService = new MultipleAppWebService();
        webService.port(8888);
        webService.get("/", (req, res) -> {
        	return "hello app1";
        });
        
        Application app = new Application();
        app.get("/",(req, res) -> {
        	return "hello app2";
        });
        app.port(8889);
        webService.start(app);

        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        app.stop();
        System.out.println("Waiting to handle requests!");
            
    }

}
