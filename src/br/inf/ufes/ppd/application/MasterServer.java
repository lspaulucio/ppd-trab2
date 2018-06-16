package br.inf.ufes.ppd.application;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.implementation.Configurations;
import br.inf.ufes.ppd.implementation.MasterImpl;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.messaging.ConnectionConfiguration;
import javax.jms.JMSContext;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
/** Master application
 *
 * @author Leonardo Santos Paulucio
 */

public class MasterServer {
    
    public static void main(String[] args) {
        
        //args[0] Registry address
        
        String REGISTRY_ADDRESS = (args.length < 1) ? Configurations.REGISTRY_ADDRESS : args[0]; 
        
        String host = (args.length < 2) ? "127.0.0.1" : args[1];
        
        try {
        
            // RMI 
            MasterImpl masterObj = new MasterImpl();
            Master masterRef = (Master) UnicastRemoteObject.exportObject(masterObj, 0);
            // Bind the remote object in the registry
            Registry registry = LocateRegistry.getRegistry(REGISTRY_ADDRESS);
            registry.rebind(Configurations.REGISTRY_MASTER_NAME, masterRef);

            // JMS 
            Logger.getLogger("").setLevel(Level.SEVERE);
            
            System.out.println("Obtaining connection factory...");
            
            com.sun.messaging.ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
            connectionFactory.setProperty(ConnectionConfiguration.imqAddressList, host + ":7676");	
            
            System.out.println("Obtained connection factory.");
            
            System.out.println("Obtaining queues...");
            
            Queue subAttackQueue = (Queue) new com.sun.messaging.Queue("SubAttacksQueue");
            Queue guessesQueue = (Queue) new com.sun.messaging.Queue("GuessesQueue");
            
            System.out.println("Obtained queues.");			

            JMSContext context = connectionFactory.createContext();
         
            masterObj.setContext(context);
            masterObj.setSubAttacksQueue(subAttackQueue);
            masterObj.setProducer(context.createProducer()); 
            masterObj.setConsumer(context.createConsumer((Destination) guessesQueue)); 
            masterObj.startGuessTask();
            
            System.out.println("Master ready!");
        } 
        catch (JMSException e) {
            System.err.println("Master JMS exception:\n" + e.getMessage());
        }  
        catch (RemoteException e){
            System.err.println("Master Remote exception:\n" + e.getMessage());
        }
    }
}
