/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.inf.ufes.ppd.utils;

import br.inf.ufes.ppd.implementation.Configurations;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** File tools.
 *
 * @author Leonardo Santos Paulucio
 */

public class FileTools {
    /**
     * Realiza a leitura de um arquivo.
     * @param filename Nome do arquivo que se deseja ler.
     * @return Vetor de bytes do arquivo lido.
    */
    public static byte[] readFile(String filename)
    {
        byte[] data = null;
        try{
            File file = new File(filename);
            InputStream is = new FileInputStream(file);
            long length = file.length();
            // creates array (assumes file length<Integer.MAX_VALUE)
            data = new byte[(int)length];
            int offset = 0; int count = 0;
            while ((offset < data.length) &&
                            (count=is.read(data, offset, data.length-offset)) >= 0) {
                offset += count;
            }
            is.close();
        }
        catch(IOException e){
            System.err.println("File not found.");
        }
        return data;
    }
    
    /**
     * Realiza a leitura do dicionario.
     * @param filename Nome do arquivo de dicionario.
     * @return Uma lista de strings com as palavras do dicionario
     */     
    public static List<String> readDictionary(String filename) {
        List<String> words = new ArrayList<>();
        
        try {
            Scanner dic = new Scanner(new File(filename));

            while (dic.hasNext()) {
                words.add(dic.next());
            }

            dic.close();

        } catch (IOException e) {
            System.err.println("ReadDictionary error: \n" + e.getMessage());
        }
        
        return words;
    }
    
    /**
     * Salva um vetor de bytes em um arquivo.
     * @param file Nome do arquivo que será gerado.
     * @param data Vetor de bytes a ser gravado.
     * @throws java.io.IOException
    */
    public static void saveResult(String file, byte[] data) throws IOException
    {
        File dir = new File(Configurations.RESULTS_FOLDER);
        
        //Checking if directory exists
        if(!dir.exists()){
            dir.mkdir();
        }
        
        String filename = Configurations.RESULTS_FOLDER + file;
        FileOutputStream out = new FileOutputStream(filename);
        out.write(data);
        out.close();
    }
    
    
}
