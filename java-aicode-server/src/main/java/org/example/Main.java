package org.example;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class Main {
    public static void main(String[] args) throws UnsupportedEncodingException {


//        System.out.println("Hello world!");
//        // Java 代码生成 Base64
//        String userName ="c758960e39ae4e048ca3aef959caf000";
//        String passWord = "fdce0eb6-651d-4dd1-8427-7ebda62e1265";
////        String credentials = "c758960e39ae4e048ca3aef959caf000:fdce0eb6-651d-4dd1-8427-7ebda62e1265";
//        String credentials = userName+":"+passWord;
//        String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes("UTF-8"));
//        System.out.println(base64Credentials);
        TDSQLTableExporter.exportTableStructure("data");
    }

}