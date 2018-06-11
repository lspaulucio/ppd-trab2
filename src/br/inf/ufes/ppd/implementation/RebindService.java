package br.inf.ufes.ppd.implementation;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.TimerTask;
import java.util.UUID;

/** Classe que realiza o serviço de rebind.
 *
 * @author Leonardo Santos Paulucio
 */

public class RebindService extends TimerTask {
    
    private Master masterRef;
    private final Slave slaveRef;
    private final String slaveName;
    private final UUID slaveUID;
    private final String regist;
    /**
     * Construtor do serviço de rebind.
     * @param m referência para o mestre.
     * @param s referência para o escravo.
     * @param name nome do escravo.
     * @param uid identificador unico do escravo.
     */
    public RebindService(Master m, Slave s, String name, UUID uid, String reg){
        this.masterRef = m;
        this.slaveRef = s;
        this.slaveName = name;
        this.slaveUID = uid;
        this.regist = reg;
    }
    
    @Override
    public void run(){
                
        try{
            //Trying to rebind on master
            masterRef.addSlave(slaveRef, slaveName, slaveUID);
            System.out.println("Slave registered");
        }
        catch (RemoteException e){
            System.err.println("Master down. Error:\n" + e.getMessage());

            //Master down, so try to find another master on registry
            try {
                Registry registry = LocateRegistry.getRegistry(regist);
                Master m = (Master) registry.lookup(Configurations.REGISTRY_MASTER_NAME);
                
                m.addSlave(slaveRef, slaveName, slaveUID);
                System.out.println("Slave registered");
                masterRef = m; //Save new master reference
            }
            catch (RemoteException p){
                System.err.println("Master not found or not registered on registry. Error:\n" + p.getMessage());
            }
            catch (Exception a){
                System.err.println("Rebind service error:\n" + a.getMessage());
            }
        }
    }
}
