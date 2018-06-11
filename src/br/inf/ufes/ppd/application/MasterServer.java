package br.inf.ufes.ppd.application;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.implementation.Configurations;
import br.inf.ufes.ppd.implementation.MasterImpl;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/** Master application
 *
 * @author Leonardo Santos Paulucio
 */

public class MasterServer {
    
    public static void main(String[] args) {
        
        //args[0] Registry address
        
        String REGISTRY_ADDRESS = (args.length < 1) ? Configurations.REGISTRY_ADDRESS : args[0]; 
        
        try {
        
            MasterImpl masterObj = new MasterImpl();
            Master masterRef = (Master) UnicastRemoteObject.exportObject(masterObj, 0);
            // Bind the remote object in the registry
            Registry registry = LocateRegistry.getRegistry(REGISTRY_ADDRESS);
            registry.rebind(Configurations.REGISTRY_MASTER_NAME, masterRef);

            System.out.println("Master ready!");
            
        } catch (RemoteException e) {
            System.err.println("Master exception:\n" + e.getMessage());
        }
    }
}
