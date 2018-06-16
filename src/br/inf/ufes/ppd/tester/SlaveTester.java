package br.inf.ufes.ppd.tester;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.implementation.Configurations;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

/** Slave Tester to measure overhead
 *
 * @author Leonardo Santos Paulucio
 */

public class SlaveTester {
    
    public static void main(String[] args) {
        
        //args[0] Slave name
        //args[1] Registry address
        
        String SLAVE_NAME = (args.length < 1) ? "SlaveLeonardo" : args[0];
        String REGISTRY_ADDRESS = (args.length < 2) ? Configurations.REGISTRY_ADDRESS : args[1];
        
        //Creating a new Slave
        SlaveImplTester slave = new SlaveImplTester();
        slave.setUid(UUID.randomUUID());
        
        try {
            Registry registry = LocateRegistry.getRegistry(REGISTRY_ADDRESS);
            Master m = (Master) registry.lookup(Configurations.REGISTRY_MASTER_NAME);
            
            Slave slaveRef = (Slave) UnicastRemoteObject.exportObject(slave,0);
            
        }
        catch(RemoteException e) {
            System.err.println("Slave tester remote exception:\n" + e.getMessage());
        }
        catch(Exception p){
            System.err.println("Slave tester exception:\n" + p.getMessage());
        }
    }
}
