package br.inf.ufes.ppd.application;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.implementation.Configurations;
import br.inf.ufes.ppd.implementation.SlaveImpl;
import br.inf.ufes.ppd.implementation.RebindService;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.UUID;

/** Slave Application
 *
 * @author Leonardo Santos Paulucio
 */

public class SlaveServer {
    
    public static void main(String[] args) {
                    
        //args[0] Dictionary file path
        //args[1] Slave name
        //args[2] Registry address
        
        String DICTIONARY_PATH = (args.length < 1) ? Configurations.DICTIONARY_PATH : args[0];
        String SLAVE_NAME = (args.length < 2) ? "SlaveLeonardo" : args[1];
        String REGISTRY_ADDRESS = (args.length < 3) ? Configurations.REGISTRY_ADDRESS : args[2];

//        System.out.println(SLAVE_NAME);

        //Creating a new Slave
        SlaveImpl slave = new SlaveImpl();
        slave.readDictionary(DICTIONARY_PATH);
        slave.setUid(UUID.randomUUID());
 
        try {
            Registry registry = LocateRegistry.getRegistry(REGISTRY_ADDRESS);
            Master m = (Master) registry.lookup(Configurations.REGISTRY_MASTER_NAME);
            
            Slave slaveRef = (Slave) UnicastRemoteObject.exportObject(slave,0);
            
            //Creating rebind service
            Timer timer = new Timer();   
            RebindService rs = new RebindService(m, slaveRef, SLAVE_NAME, slave.getUid(), REGISTRY_ADDRESS);            
            timer.scheduleAtFixedRate(rs, 0, Configurations.REBIND_TIME);  // 0 = delay, REBIND_TIME = frequence
            
            System.out.println("Slave: " + SLAVE_NAME + " ready");
            
        }
        catch(RemoteException e) {
            System.err.println("Slave exception:\n" + e.getMessage());
        }
        catch(Exception p){
            System.err.println("Slave exception:\n" + p.getMessage());
        }
    }
}
