package br.inf.ufes.ppd.implementation;

/** Classe com as configurações padrões.
 *
 * @author Leonardo Santos Paulucio
 */

public class Configurations {
    
    //Registry configurations
    public static final String REGISTRY_ADDRESS = "192.168.1.116";
    public static final String REGISTRY_MASTER_NAME = "mestre";
    
    //Directories 
    public static final String RESULTS_FOLDER = "Results/";
    public static final String MEASURE_FOLDER = "Measures/";
    
    //Message and dictionart configurations
    public static final int KNOWN_TEXT_SIZE = 10;
    public static final int DICTIONARY_SIZE = 80367;
    public static final String DICTIONARY_PATH = "dictionary.txt";
    
    //Master and Slave configurations
    public static final int REBIND_TIME = 30000; //30 seconds
    public static final int CHECKPOINT_TIME = 10000; //10 seconds
    public static final int TIMEOUT = 20000; //20 seconds
    
    //Tester configurations
    public static final int NUMBER_SAMPLES = 5;
    public static final int BREAKS_LENGTH = 5000;
    
}
