/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.packagegenerator;
import com.mycompany.packagegenerator.HomeMenu;


/**
 *
 * @author kosugek
 */
public class PackageGenerator {
    public static HomeMenu hm;
    public static void main(String[] args) {
        initializeGUI();
    }
    private static void initializeGUI(){
        hm = new HomeMenu();
        hm.setVisible(true);       
    }
}
