package br.inf.ufes.ppd.application;

import br.inf.ufes.ppd.implementation.Configurations;
import br.inf.ufes.ppd.implementation.SlaveImpl;
import com.sun.messaging.ConnectionConfiguration;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Queue;
import javax.jms.*;

/** Slave Application
 *
 * @author Leonardo Santos Paulucio
 */

public class SlaveServer {
    
    public static void main(String[] args) {
                    
        //args[0] Dictionary file path
        //args[1] Slave name
        //args[2] Host address
        
        String DICTIONARY_PATH = (args.length < 1) ? Configurations.DICTIONARY_PATH : args[0];
        String SLAVE_NAME = (args.length < 2) ? "SlaveLeonardo" : args[1];
        String host = (args.length < 3) ? "127.0.0.1" : args[2];

//        System.out.println(SLAVE_NAME);

        //Creating a new Slave
        SlaveImpl slave = new SlaveImpl();
        slave.readDictionary(DICTIONARY_PATH);
        slave.setUid(UUID.randomUUID());
        slave.setSlaveName(SLAVE_NAME);
 
        //JMS
        try{
            Logger.getLogger("").setLevel(Level.SEVERE);

            System.out.println("Obtaining connection factory...");

            com.sun.messaging.ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
            connectionFactory.setProperty(ConnectionConfiguration.imqAddressList, host+":7676");	
            connectionFactory.setProperty(ConnectionConfiguration.imqConsumerFlowLimitPrefetch, "false");
            System.out.println("Obtained connection factory.");

            System.out.println("Obtaining queues...");
            
            Queue subAttackQueue = (Queue) new com.sun.messaging.Queue("SubAttacksQueue");
            Queue guessesQueue = (Queue) new com.sun.messaging.Queue("GuessesQueue");
            
            System.out.println("Obtained queues.");			

            JMSContext context = connectionFactory.createContext();

            slave.setContext(context);
            slave.setProducer(context.createProducer()); 
            slave.setGuessesQueue(guessesQueue);
            slave.setSubAttacksQueue(subAttackQueue);
            slave.setProducer(context.createProducer()); 
            slave.setConsumer(context.createConsumer((Destination) subAttackQueue)); 
            
            System.out.println("Slave ready!\n");
            
            while(true){
                slave.getJob();
            }
        }
        catch(JMSException e){
            System.err.println("JMS Error \n " + e.getMessage());
        }
    }
}
