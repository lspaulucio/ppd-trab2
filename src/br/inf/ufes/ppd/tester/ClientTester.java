package br.inf.ufes.ppd.tester;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.implementation.Configurations;
import br.inf.ufes.ppd.utils.Crypto;
import br.inf.ufes.ppd.utils.FileTools;
import br.inf.ufes.ppd.utils.Tupla;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Client Application Tester.
 *  For automation of tests. 
 * @author Leonardo Santos Paulucio
 */

public class ClientTester {
    
    static List<String> keys;

    public static void main(String[] args)
    {              
        //args[0] random vector lengths or linearly
        //args[1] number of attacks
        //args[2] initial range of random bytes vector
        //args[3] final range of random bytes vector
        //args[4] number of samples
        
        
        try {
            
            if(args.length < 3){
                System.err.println("Missing parameters");
                throw new Exception("Usage: Client Tester <s|r> <InitialRange> <FinalRange> [<NumberOfSamples>]");
            }
            
            keys = FileTools.readDictionary(Configurations.DICTIONARY_PATH);

            System.out.println("Client start");
            Registry registry = LocateRegistry.getRegistry(Configurations.REGISTRY_ADDRESS);
            Master m = (Master) registry.lookup(Configurations.REGISTRY_MASTER_NAME); 
            
            Random rand = new Random();
            
            String type = args[0];
            
            int initialRange = new Integer(args[1]);
            int finalRange = new Integer(args[2]);
            int samples = (args.length < 4) ? Configurations.NUMBER_SAMPLES : new Integer(args[3]);
            int numBreaks = (finalRange - initialRange)/Configurations.BREAKS_LENGTH;
            
            byte[] knownText;
            byte[] encryptedText;
            double startTime, endTime;
            
            List<Tupla> dados = new ArrayList<>();
            
            for(int i = 0; i <= numBreaks; i++){
                
                Guess[] guessVector = null;
                
                int key = rand.nextInt(Configurations.DICTIONARY_SIZE);
                int length;
                
                if(type.equals("r")){
                    length = rand.nextInt(finalRange - initialRange + 1) + initialRange;
                }
                else{
                    length = initialRange;
                    initialRange += Configurations.BREAKS_LENGTH;
                }

                System.out.println("Size: " + length);
                
                encryptedText = new byte[length];
                rand.nextBytes(encryptedText);
                knownText = Arrays.copyOfRange(encryptedText, 0, Configurations.KNOWN_TEXT_SIZE);
                encryptedText = Crypto.encrypter(keys.get(key).getBytes(), encryptedText);
                
                System.out.println("Key " + i + ": " + keys.get(key));
//                System.out.println(knownText);            
//                System.out.println(encryptedText);
                
                startTime = System.nanoTime();
                
                for(int s = 0; s < samples; s++)
                    guessVector = m.attack(encryptedText, knownText);
                
                endTime = System.nanoTime() - startTime;
                
                endTime /= samples*1000000000;
                
                dados.add(new Tupla(endTime, length));
                
                if(guessVector != null){

                    for (Guess guess : guessVector) {
                        System.out.println("Key found: " + guess.getKey());
                    }
                }
                else{
                    System.out.println("No keys found");
                }
            }
            
            Tupla.generateCSV(dados);
            
        }
        catch(RemoteException e)
        {
            System.err.println("Client - Master remote error:\n" + e.getMessage());
        }
        catch(Exception p){
            System.err.println("Client error:\n" + p.getMessage());
            p.printStackTrace();
        }
    }  
}